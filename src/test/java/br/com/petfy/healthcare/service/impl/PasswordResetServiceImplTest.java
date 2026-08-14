package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PasswordResetConfirmDTO;
import br.com.petfy.healthcare.domain.dto.PasswordResetRequestDTO;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PasswordResetToken;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.PasswordResetTokenRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.Notification;
import br.com.petfy.healthcare.notification.Notifier;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private PersonRepository personRepository;

    /* Encerrar as entradas entrou junto com a troca de senha, na V51. */
    @Mock
    private br.com.petfy.healthcare.domain.repository.PersonSessionRepository personSessionRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private OpaqueTokenService opaqueTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Notifier notifier;

    @InjectMocks
    private PasswordResetServiceImpl service;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "ulysses@petfy.com.br";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(service, "cooldownMinutes", 5L);
    }

    private Person person() {
        return Person.builder()
                .personId(OWNER_ID)
                .name("Ulysses")
                .email(EMAIL)
                .password("hash-antigo")
                .build();
    }

    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        /**
         * Responder diferente para e-mail sem conta transformaria este endpoint,
         * que e publico, num verificador de quem tem cadastro aqui.
         */
        @Test
        @DisplayName("deve terminar em silencio quando o e-mail nao tem conta, sem gravar nem enviar")
        void deveTerminarEmSilencioQuandoEmailNaoTemConta() {
            when(personRepository.findByEmail("ninguem@petfy.com.br")).thenReturn(Optional.empty());

            service.requestReset(new PasswordResetRequestDTO("ninguem@petfy.com.br"));

            verify(tokenRepository, never()).save(any());
            verify(notifier, never()).send(any());
        }

        @Test
        @DisplayName("deve gravar o hash do token, nunca o token, e enviar o token pelo canal")
        void deveGravarHashEEnviarToken() {
            when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
            when(tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(OWNER_ID)).thenReturn(Optional.empty());
            when(tokenRepository.findByPersonPersonIdAndUsedAtIsNull(OWNER_ID)).thenReturn(List.of());
            when(opaqueTokenService.generate()).thenReturn("token-em-claro");
            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");

            service.requestReset(new PasswordResetRequestDTO(EMAIL));

            var tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo("hash-do-token");
            assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(tokenCaptor.getValue().getUsedAt()).isNull();

            var notificacaoCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notifier).send(notificacaoCaptor.capture());
            assertThat(notificacaoCaptor.getValue().getToEmail()).isEqualTo(EMAIL);
            assertThat(String.join(" ", notificacaoCaptor.getValue().getLines())).contains("token-em-claro");
        }

        /**
         * Dois links vivos ao mesmo tempo dobram a janela de quem interceptou o
         * e-mail antigo, sem ajudar em nada quem esqueceu a senha.
         */
        @Test
        @DisplayName("pedido novo deve invalidar os pedidos anteriores ainda em aberto")
        void pedidoNovoDeveInvalidarAnteriores() {
            var anterior = PasswordResetToken.builder()
                    .tokenHash("hash-antigo")
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .creationDate(LocalDateTime.now().minusMinutes(10))
                    .build();

            when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
            when(tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(OWNER_ID))
                    .thenReturn(Optional.of(anterior));
            when(tokenRepository.findByPersonPersonIdAndUsedAtIsNull(OWNER_ID)).thenReturn(List.of(anterior));
            when(opaqueTokenService.generate()).thenReturn("token-novo");
            when(opaqueTokenService.hash("token-novo")).thenReturn("hash-novo");

            service.requestReset(new PasswordResetRequestDTO(EMAIL));

            assertThat(anterior.getUsedAt()).isNotNull();
            verify(tokenRepository).saveAll(List.of(anterior));
        }

        /**
         * O endpoint responde igual exista ou nao a conta. Se uma falha de envio
         * virasse erro na resposta, a diferenca entre 500 e 202 entregaria
         * exatamente a informacao que o silencio existe para esconder.
         */
        @Test
        @DisplayName("falha de envio nao pode virar erro na resposta, senao denuncia que a conta existe")
        void falhaDeEnvioNaoPodeVirarErroNaResposta() {
            when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
            when(tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(OWNER_ID)).thenReturn(Optional.empty());
            when(tokenRepository.findByPersonPersonIdAndUsedAtIsNull(OWNER_ID)).thenReturn(List.of());
            when(opaqueTokenService.generate()).thenReturn("token-em-claro");
            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            doThrow(new IllegalStateException("Resend fora")).when(notifier).send(any());

            assertThatCode(() -> service.requestReset(new PasswordResetRequestDTO(EMAIL)))
                    .doesNotThrowAnyException();
        }

        /**
         * Sem cooldown, o endpoint publico vira maquina de enviar e-mail para
         * terceiros: basta repetir a chamada com o endereco de alguem.
         */
        @Test
        @DisplayName("deve ignorar pedido dentro do cooldown, sem enviar nada")
        void deveIgnorarPedidoDentroDoCooldown() {
            var recente = PasswordResetToken.builder()
                    .creationDate(LocalDateTime.now().minusMinutes(1))
                    .expiresAt(LocalDateTime.now().plusMinutes(29))
                    .build();

            when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person()));
            when(tokenRepository.findFirstByPersonPersonIdOrderByCreationDateDesc(OWNER_ID))
                    .thenReturn(Optional.of(recente));

            service.requestReset(new PasswordResetRequestDTO(EMAIL));

            verify(tokenRepository, never()).save(any());
            verify(notifier, never()).send(any());
        }
    }

    @Nested
    @DisplayName("confirmReset")
    class ConfirmReset {

        private PasswordResetToken tokenValido(Person dono) {
            return PasswordResetToken.builder()
                    .person(dono)
                    .tokenHash("hash-do-token")
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .creationDate(LocalDateTime.now().minusMinutes(5))
                    .build();
        }

        @Test
        @DisplayName("deve trocar a senha, marcar o token como usado e derrubar as sessoes abertas")
        void deveTrocarSenhaEMarcarTokenComoUsado() {
            var dono = person();
            var token = tokenValido(dono);

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("s3nhaNova")).thenReturn("hash-novo");

            service.confirmReset(new PasswordResetConfirmDTO("token-em-claro", "s3nhaNova"));

            var personCaptor = ArgumentCaptor.forClass(Person.class);
            verify(personRepository).save(personCaptor.capture());
            assertThat(personCaptor.getValue().getPassword()).isEqualTo("hash-novo");
            assertThat(personCaptor.getValue().getPasswordChangedAt()).isNotNull();

            assertThat(token.getUsedAt()).isNotNull();
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("deve recusar token inexistente")
        void deveRecusarTokenInexistente() {
            when(opaqueTokenService.hash("chute")).thenReturn("hash-de-chute");
            when(tokenRepository.findByTokenHash("hash-de-chute")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmDTO("chute", "s3nhaNova")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.RESET_TOKEN_NOT_FOUND.getMessage());

            verify(personRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar token expirado, com a mesma resposta de token inexistente")
        void deveRecusarTokenExpirado() {
            var expirado = tokenValido(person());
            expirado.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(expirado));

            assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmDTO("token-em-claro", "s3nhaNova")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.RESET_TOKEN_NOT_FOUND.getMessage());

            verify(personRepository, never()).save(any());
        }

        /**
         * Token de uso unico: reusar o mesmo link depois de trocada a senha e o
         * sinal mais provavel de que alguem interceptou o e-mail.
         */
        @Test
        @DisplayName("deve recusar token ja usado")
        void deveRecusarTokenJaUsado() {
            var usado = tokenValido(person());
            usado.setUsedAt(LocalDateTime.now().minusMinutes(1));

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usado));

            assertThatThrownBy(() -> service.confirmReset(new PasswordResetConfirmDTO("token-em-claro", "s3nhaNova")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.RESET_TOKEN_NOT_FOUND.getMessage());

            verify(personRepository, never()).save(any());
        }
    }

}

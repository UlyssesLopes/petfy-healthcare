package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.EmailVerificationConfirmDTO;
import br.com.petfy.healthcare.domain.dto.EmailVerificationResendDTO;
import br.com.petfy.healthcare.domain.entity.EmailVerificationToken;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.EmailVerificationTokenRepository;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private OpaqueTokenService opaqueTokenService;

    @Mock
    private Notifier notifier;

    @InjectMocks
    private EmailVerificationServiceImpl service;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "ulysses@petfy.com.br";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationHours", 24L);
        ReflectionTestUtils.setField(service, "cooldownMinutes", 5L);
    }

    private Owner owner() {
        return Owner.builder().ownerId(OWNER_ID).name("Ulysses").email(EMAIL).build();
    }

    @Nested
    @DisplayName("sendVerification")
    class SendVerification {

        @Test
        @DisplayName("deve gravar o hash e enviar o token no cadastro")
        void deveGravarHashEEnviarToken() {
            when(tokenRepository.findByOwnerOwnerIdAndUsedAtIsNull(OWNER_ID)).thenReturn(List.of());
            when(opaqueTokenService.generate()).thenReturn("token-em-claro");
            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");

            service.sendVerification(owner());

            var captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isEqualTo("hash-do-token");

            var notificacao = ArgumentCaptor.forClass(Notification.class);
            verify(notifier).send(notificacao.capture());
            assertThat(String.join(" ", notificacao.getValue().getLines())).contains("token-em-claro");
        }

        /**
         * O efeito principal do cadastro e a conta criada. Perde-la porque o
         * canal de notificacao caiu seria trocar um problema pequeno - reenviar a
         * confirmacao - por um grande.
         */
        @Test
        @DisplayName("falha de envio nao pode derrubar o cadastro")
        void falhaDeEnvioNaoPodeDerrubarCadastro() {
            when(tokenRepository.findByOwnerOwnerIdAndUsedAtIsNull(OWNER_ID)).thenReturn(List.of());
            when(opaqueTokenService.generate()).thenReturn("token-em-claro");
            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            doThrow(new RuntimeException("SMTP fora")).when(notifier).send(any());

            service.sendVerification(owner());
        }
    }

    @Nested
    @DisplayName("resend")
    class Resend {

        @Test
        @DisplayName("deve terminar em silencio quando o e-mail nao tem conta")
        void deveTerminarEmSilencioQuandoEmailNaoTemConta() {
            when(ownerRepository.findByEmail("ninguem@petfy.com.br")).thenReturn(Optional.empty());

            service.resend(new EmailVerificationResendDTO("ninguem@petfy.com.br"));

            verify(tokenRepository, never()).save(any());
            verify(notifier, never()).send(any());
        }

        @Test
        @DisplayName("nao deve reenviar para quem ja confirmou")
        void naoDeveReenviarParaQuemJaConfirmou() {
            var jaConfirmado = owner();
            jaConfirmado.setEmailVerifiedAt(LocalDateTime.now().minusDays(1));
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(jaConfirmado));

            service.resend(new EmailVerificationResendDTO(EMAIL));

            verify(tokenRepository, never()).save(any());
            verify(notifier, never()).send(any());
        }

        @Test
        @DisplayName("deve ignorar pedido dentro do cooldown")
        void deveIgnorarPedidoDentroDoCooldown() {
            var recente = EmailVerificationToken.builder()
                    .creationDate(LocalDateTime.now().minusMinutes(1))
                    .build();

            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner()));
            when(tokenRepository.findFirstByOwnerOwnerIdOrderByCreationDateDesc(OWNER_ID))
                    .thenReturn(Optional.of(recente));

            service.resend(new EmailVerificationResendDTO(EMAIL));

            verify(tokenRepository, never()).save(any());
            verify(notifier, never()).send(any());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        private EmailVerificationToken tokenValido(Owner dono) {
            return EmailVerificationToken.builder()
                    .owner(dono)
                    .tokenHash("hash-do-token")
                    .expiresAt(LocalDateTime.now().plusHours(20))
                    .creationDate(LocalDateTime.now().minusHours(4))
                    .build();
        }

        @Test
        @DisplayName("deve marcar o e-mail como confirmado e queimar o token")
        void deveMarcarEmailComoConfirmado() {
            var dono = owner();
            var token = tokenValido(dono);

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(token));

            service.confirm(new EmailVerificationConfirmDTO("token-em-claro"));

            var captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            assertThat(captor.getValue().getEmailVerifiedAt()).isNotNull();
            assertThat(captor.getValue().podeReceberNotificacao()).isTrue();

            assertThat(token.getUsedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve recusar token expirado")
        void deveRecusarTokenExpirado() {
            var expirado = tokenValido(owner());
            expirado.setExpiresAt(LocalDateTime.now().minusHours(1));

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(expirado));

            assertThatThrownBy(() -> service.confirm(new EmailVerificationConfirmDTO("token-em-claro")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.VERIFICATION_TOKEN_NOT_FOUND.getMessage());

            verify(ownerRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar token ja usado")
        void deveRecusarTokenJaUsado() {
            var usado = tokenValido(owner());
            usado.setUsedAt(LocalDateTime.now().minusMinutes(1));

            when(opaqueTokenService.hash("token-em-claro")).thenReturn("hash-do-token");
            when(tokenRepository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usado));

            assertThatThrownBy(() -> service.confirm(new EmailVerificationConfirmDTO("token-em-claro")))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage(ErrorMessageEnum.VERIFICATION_TOKEN_NOT_FOUND.getMessage());

            verify(ownerRepository, never()).save(any());
        }
    }

}

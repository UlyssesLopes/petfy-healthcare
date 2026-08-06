package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicInviteRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicInviteRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicInviteServiceImplTest {

    @Mock
    private ClinicInviteRepository clinicInviteRepository;

    @Mock
    private VetRepository vetRepository;

    @Mock
    private CurrentVetProvider currentVetProvider;

    private ClinicInviteServiceImpl service;

    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");
    private static final UUID INVITE_ID = UUID.fromString("bbbbbbbb-5555-5555-5555-555555555555");
    private static final UUID VET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        // token service real: o valor do teste esta em conferir que o token nao e
        // guardado em claro, e nao em reanimalir o hash num mock
        service = new ClinicInviteServiceImpl(clinicInviteRepository, vetRepository,
                currentVetProvider, new OpaqueTokenService());
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 7);
    }

    private Clinic clinic(UUID id) {
        return Clinic.builder().clinicId(id).name("Clinica Bicho Feliz").build();
    }

    private Vet vetDa(UUID clinicId) {
        return Vet.builder().vetId(VET_ID).name("Dra. Marina").clinic(clinic(clinicId)).build();
    }

    private void autenticadoComoVetDa(UUID clinicId) {
        when(currentVetProvider.require()).thenReturn(vetDa(clinicId));
    }

    private ClinicInvite convite(String email, LocalDateTime expiresAt,
                                 LocalDateTime acceptedAt, LocalDateTime revokedAt) {
        return ClinicInvite.builder()
                .clinicInviteId(INVITE_ID)
                .clinic(clinic(CLINIC_ID))
                .createdBy(vetDa(CLINIC_ID))
                .tokenHash("hash")
                .email(email)
                .expiresAt(expiresAt)
                .acceptedAt(acceptedAt)
                .revokedAt(revokedAt)
                .creationDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    private ClinicInvite conviteAberto() {
        return convite(null, LocalDateTime.now().plusDays(7), null, null);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve emitir o convite para a clinica de quem esta autenticado")
        void deveEmitirParaAClinicaDoEmissor() {
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.save(any(ClinicInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(ClinicInviteRequestDTO.builder().build());

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getCreatedByVetName()).isEqualTo("Dra. Marina");
            assertThat(result.isUsable()).isTrue();
        }

        @Test
        @DisplayName("deve devolver o token apenas na criacao, e guardar so o hash")
        void deveDevolverTokenApenasNaCriacao() {
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.save(any(ClinicInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(null);

            assertThat(result.getToken()).isNotBlank();

            var captor = ArgumentCaptor.forClass(ClinicInvite.class);
            verify(clinicInviteRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).isNotEqualTo(result.getToken());
        }

        @Test
        @DisplayName("a entidade de convite nao deve ter campo para o token em claro")
        void entidadeNaoDeveGuardarTokenEmClaro() {
            assertThat(ClinicInvite.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("tokenHash")
                    .doesNotContain("token");
        }

        @Test
        @DisplayName("deve respeitar a validade informada")
        void deveRespeitarValidadeInformada() {
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.save(any(ClinicInvite.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.create(ClinicInviteRequestDTO.builder().expiresInDays(2).build());

            assertThat(result.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(3));
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        private void baseTem(ClinicInvite invite) {
            when(clinicInviteRepository.findByTokenHash(any())).thenReturn(Optional.ofNullable(invite));
        }

        @Test
        @DisplayName("deve aceitar convite aberto e valido")
        void deveAceitarConviteAberto() {
            baseTem(conviteAberto());

            assertThat(service.validate("qualquer-token", "novo@vet.com.br").getClinic().getClinicId())
                    .isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve recusar convite inexistente")
        void deveRecusarConviteInexistente() {
            baseTem(null);

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(111, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("deve recusar convite expirado")
        void deveRecusarConviteExpirado() {
            baseTem(convite(null, LocalDateTime.now().minusDays(1), null, null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("deve recusar convite revogado")
        void deveRecusarConviteRevogado() {
            baseTem(convite(null, LocalDateTime.now().plusDays(7), null, LocalDateTime.now()));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("deve recusar convite ja usado - e de uso unico")
        void deveRecusarConviteJaUsado() {
            baseTem(convite(null, LocalDateTime.now().plusDays(7), LocalDateTime.now().minusHours(1), null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "novo@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("convite endereçado deve recusar email diferente, mesmo com o token certo")
        void conviteEnderecadoDeveRecusarOutroEmail() {
            baseTem(convite("convidada@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThatThrownBy(() -> service.validate("qualquer-token", "estranho@vet.com.br"))
                    .isInstanceOf(PetfyHealthcareException.class);
        }

        @Test
        @DisplayName("convite endereçado deve aceitar o email destinatario, ignorando maiusculas")
        void conviteEnderecadoDeveAceitarODestinatario() {
            baseTem(convite("convidada@vet.com.br", LocalDateTime.now().plusDays(7), null, null));

            assertThat(service.validate("qualquer-token", "Convidada@Vet.Com.Br")).isNotNull();
        }

        @Test
        @DisplayName("todos os motivos de recusa devem responder a mesma coisa")
        void todosOsMotivosDevemResponderIgual() {
            var mensagens = List.of(
                    capturaMensagem(null),
                    capturaMensagem(convite(null, LocalDateTime.now().minusDays(1), null, null)),
                    capturaMensagem(convite(null, LocalDateTime.now().plusDays(7), null, LocalDateTime.now())),
                    capturaMensagem(convite(null, LocalDateTime.now().plusDays(7), LocalDateTime.now(), null)),
                    capturaMensagem(convite("outra@vet.com.br", LocalDateTime.now().plusDays(7), null, null)));

            assertThat(mensagens).containsOnly("Invite not found or no longer valid");
        }

        private String capturaMensagem(ClinicInvite invite) {
            when(clinicInviteRepository.findByTokenHash(any())).thenReturn(Optional.ofNullable(invite));
            try {
                service.validate("qualquer-token", "novo@vet.com.br");
                throw new AssertionError("deveria ter lancado PetfyHealthcareException");
            } catch (PetfyHealthcareException e) {
                return e.getMessage();
            }
        }
    }

    @Nested
    @DisplayName("markAccepted")
    class MarkAccepted {

        @Test
        @DisplayName("deve consumir o convite registrando quem aceitou")
        void deveConsumirRegistrandoQuemAceitou() {
            var invite = conviteAberto();
            var novoVet = vetDa(CLINIC_ID);
            when(vetRepository.findById(VET_ID)).thenReturn(Optional.of(novoVet));

            service.markAccepted(invite, VET_ID);

            assertThat(invite.getAcceptedAt()).isNotNull();
            assertThat(invite.getAcceptedBy()).isEqualTo(novoVet);
            assertThat(invite.isUsable(LocalDateTime.now())).isFalse();
            verify(clinicInviteRepository).save(invite);
        }
    }

    @Nested
    @DisplayName("listFromMyClinic e revoke")
    class ListarERevogar {

        @Test
        @DisplayName("deve listar apenas os convites da clinica do vet autenticado")
        void deveListarApenasDaPropriaClinica() {
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.findByClinicClinicIdOrderByCreationDateDesc(CLINIC_ID))
                    .thenReturn(List.of(conviteAberto()));

            var result = service.listFromMyClinic();

            assertThat(result).singleElement()
                    .satisfies(i -> assertThat(i.getToken()).isNull());
        }

        @Test
        @DisplayName("deve revogar convite da propria clinica")
        void deveRevogarDaPropriaClinica() {
            var invite = conviteAberto();
            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite));

            service.revoke(INVITE_ID);

            assertThat(invite.getRevokedAt()).isNotNull();
            verify(clinicInviteRepository).save(invite);
        }

        @Test
        @DisplayName("nao deve revogar convite de outra clinica")
        void naoDeveRevogarDeOutraClinica() {
            autenticadoComoVetDa(OUTRA_CLINIC_ID);
            when(clinicInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(conviteAberto()));

            assertThatThrownBy(() -> service.revoke(INVITE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(111);

            verify(clinicInviteRepository, never()).save(any());
        }

        @Test
        @DisplayName("revogar de novo nao deve mexer na data original")
        void revogarDeNovoNaoDeveMexerNaData() {
            var original = LocalDateTime.now().minusDays(2);
            var jaRevogado = convite(null, LocalDateTime.now().plusDays(7), null, original);

            autenticadoComoVetDa(CLINIC_ID);
            when(clinicInviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(jaRevogado));

            service.revoke(INVITE_ID);

            assertThat(jaRevogado.getRevokedAt()).isEqualTo(original);
            verify(clinicInviteRepository, never()).save(any());
        }
    }
}

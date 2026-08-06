package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.ClinicInviteService;
import br.com.petfy.healthcare.service.ClinicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetServiceImplTest {

    @Mock
    private VetRepository vetRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ClinicService clinicService;

    @Mock
    private ClinicInviteService clinicInviteService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentVetProvider currentVetProvider;

    @InjectMocks
    private VetServiceImpl vetService;

    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID VET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "vet@clinica.com.br";
    private static final String HASH = "$2a$10$hashDeMentira";
    private static final String TOKEN = "token-de-convite";

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private Vet vet() {
        return Vet.builder().vetId(VET_ID).name("Dra. Marina").email(EMAIL)
                .password(HASH).crmv("SP-12345").clinic(clinic()).build();
    }

    private ClinicInvite convite() {
        return ClinicInvite.builder()
                .clinicInviteId(UUID.randomUUID())
                .clinic(clinic())
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    private VetRequestDTO comConvite() {
        return VetRequestDTO.builder()
                .name("Dra. Marina").email(EMAIL).password("s3nhaForte")
                .crmv("SP-12345").inviteToken(TOKEN).build();
    }

    private void emailLivre() {
        when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("register com convite")
    class RegisterComConvite {

        @Test
        @DisplayName("deve vincular o vet a clinica do convite")
        void deveVincularAClinicaDoConvite() {
            emailLivre();
            when(clinicInviteService.validate(TOKEN, EMAIL)).thenReturn(convite());
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            var result = vetService.register(comConvite());

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getClinicName()).isEqualTo("Clinica Bicho Feliz");
        }

        @Test
        @DisplayName("deve consumir o convite, que e de uso unico")
        void deveConsumirOConvite() {
            var invite = convite();
            emailLivre();
            when(clinicInviteService.validate(TOKEN, EMAIL)).thenReturn(invite);
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            vetService.register(comConvite());

            verify(clinicInviteService).markAccepted(invite, VET_ID);
        }

        @Test
        @DisplayName("deve validar o convite antes de gravar a conta")
        void deveValidarAntesDeGravar() {
            emailLivre();
            when(clinicInviteService.validate(TOKEN, EMAIL))
                    .thenThrow(new PetfyHealthcareException("Invite not found or no longer valid",
                            111, HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> vetService.register(comConvite()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Invite not found or no longer valid");

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("o cadastro nao deve mais aceitar apontar direto para uma clinica")
        void cadastroNaoDeveAceitarClinicIdCru() {
            assertThat(VetRequestDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("clinicId")
                    .contains("inviteToken");
        }
    }

    @Nested
    @DisplayName("register criando clinica")
    class RegisterCriandoClinica {

        @Test
        @DisplayName("deve cadastrar a clinica junto quando o vet e o primeiro dela")
        void deveCadastrarClinicaJunto() {
            var novaClinica = ClinicRequestDTO.builder().name("Clinica Nova").build();
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte").clinic(novaClinica).build();

            emailLivre();
            when(clinicService.createClinic(novaClinica))
                    .thenReturn(ClinicResponseDTO.builder().clinicId(CLINIC_ID).build());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            var result = vetService.register(request);

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            verify(clinicInviteService, never()).markAccepted(any(), any());
        }
    }

    @Nested
    @DisplayName("register - regras comuns")
    class RegrasComuns {

        @Test
        @DisplayName("nao deve persistir a senha em texto puro")
        void naoDevePersistirSenhaEmTextoPuro() {
            emailLivre();
            when(clinicInviteService.validate(TOKEN, EMAIL)).thenReturn(convite());
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            vetService.register(comConvite());

            var captor = ArgumentCaptor.forClass(Vet.class);
            verify(vetRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo(HASH).isNotEqualTo("s3nhaForte");
        }

        @Test
        @DisplayName("o response de vet nao deve carregar o campo password")
        void responseNaoDeveCarregarPassword() {
            assertThat(VetResponseDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .doesNotContain("password");
        }

        @Test
        @DisplayName("deve recusar quando nao vem nem convite nem clinica nova")
        void deveRecusarSemNenhumDosDois() {
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte").build();

            emailLivre();

            assertThatThrownBy(() -> vetService.register(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar quando vem convite e clinica nova ao mesmo tempo")
        void deveRecusarComOsDois() {
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte")
                    .inviteToken(TOKEN)
                    .clinic(ClinicRequestDTO.builder().name("Outra").build())
                    .build();

            emailLivre();

            assertThatThrownBy(() -> vetService.register(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar email ja usado por outro vet")
        void deveRecusarEmailJaUsadoPorVet() {
            when(vetRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> vetService.register(comConvite()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Email already registered")
                    .extracting("code", "httpStatus")
                    .containsExactly(108, HttpStatus.CONFLICT);

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar email ja usado por um tutor - senao o login fica ambiguo")
        void deveRecusarEmailJaUsadoPorTutor() {
            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(personRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(Person.builder().personId(UUID.randomUUID()).email(EMAIL).build()));

            assertThatThrownBy(() -> vetService.register(comConvite()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Email already registered");

            verify(vetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentVet")
    class GetCurrentVet {

        @Test
        @DisplayName("deve devolver o vet autenticado com a clinica dele")
        void deveDevolverVetAutenticado() {
            when(currentVetProvider.require()).thenReturn(vet());

            var result = vetService.getCurrentVet();

            assertThat(result.getVetId()).isEqualTo(VET_ID);
            assertThat(result.getClinicName()).isEqualTo("Clinica Bicho Feliz");
        }
    }
}

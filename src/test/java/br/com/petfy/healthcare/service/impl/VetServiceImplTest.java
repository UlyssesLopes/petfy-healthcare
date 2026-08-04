package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
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
    private OwnerRepository ownerRepository;

    @Mock
    private ClinicService clinicService;

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

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private Vet vet() {
        return Vet.builder().vetId(VET_ID).name("Dra. Marina").email(EMAIL)
                .password(HASH).crmv("SP-12345").clinic(clinic()).build();
    }

    private VetRequestDTO entrandoEmClinicaExistente() {
        return VetRequestDTO.builder()
                .name("Dra. Marina").email(EMAIL).password("s3nhaForte")
                .crmv("SP-12345").clinicId(CLINIC_ID).build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("deve vincular o vet a clinica informada")
        void deveVincularVetAClinicaInformada() {
            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            var result = vetService.register(entrandoEmClinicaExistente());

            assertThat(result.getVetId()).isEqualTo(VET_ID);
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            assertThat(result.getClinicName()).isEqualTo("Clinica Bicho Feliz");
        }

        @Test
        @DisplayName("nao deve persistir a senha em texto puro")
        void naoDevePersistirSenhaEmTextoPuro() {
            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            vetService.register(entrandoEmClinicaExistente());

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
        @DisplayName("deve cadastrar a clinica junto quando o vet e o primeiro dela")
        void deveCadastrarClinicaJunto() {
            var novaClinica = ClinicRequestDTO.builder().name("Clinica Nova").build();
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte").clinic(novaClinica).build();

            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(clinicService.createClinic(novaClinica))
                    .thenReturn(ClinicResponseDTO.builder().clinicId(CLINIC_ID).build());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(passwordEncoder.encode("s3nhaForte")).thenReturn(HASH);
            when(vetRepository.save(any(Vet.class))).thenReturn(vet());

            var result = vetService.register(request);

            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
            verify(clinicService).createClinic(novaClinica);
        }

        @Test
        @DisplayName("deve recusar quando nao vem nem clinicId nem clinic")
        void deveRecusarSemClinica() {
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte").build();

            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetService.register(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve recusar quando vem clinicId e clinic ao mesmo tempo")
        void deveRecusarComOsDois() {
            var request = VetRequestDTO.builder()
                    .name("Dra. Marina").email(EMAIL).password("s3nhaForte")
                    .clinicId(CLINIC_ID)
                    .clinic(ClinicRequestDTO.builder().name("Outra").build())
                    .build();

            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

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

            assertThatThrownBy(() -> vetService.register(entrandoEmClinicaExistente()))
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
            when(ownerRepository.findByEmail(EMAIL))
                    .thenReturn(Optional.of(Owner.builder().ownerId(UUID.randomUUID()).email(EMAIL).build()));

            assertThatThrownBy(() -> vetService.register(entrandoEmClinicaExistente()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Email already registered");

            verify(vetRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(vetRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(ownerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vetService.register(entrandoEmClinicaExistente()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

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

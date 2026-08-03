package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineServiceImplTest {

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private VaccineServiceImpl vaccineService;

    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private Vaccine existingVaccine() {
        return Vaccine.builder()
                .vaccineId(VACCINE_ID)
                .pet(pet())
                .clinic(clinic())
                .vaccineName("Antirrabica")
                .applicationDate(LocalDate.of(2025, 6, 1))
                .nextDoseDate(LocalDate.of(2026, 6, 1))
                .description("Dose anual")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createVaccine")
    class CreateVaccine {

        @Test
        @DisplayName("deve vincular a vacina ao pet e a clinica informados")
        void deveVincularAoPetEClinica() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(existingVaccine());

            var request = VaccineRequestDTO.builder()
                    .petId(PET_ID)
                    .clinicId(CLINIC_ID)
                    .vaccineName("Antirrabica")
                    .applicationDate(LocalDate.of(2025, 6, 1))
                    .build();

            var result = vaccineService.createVaccine(request);

            assertThat(result.getVaccineId()).isEqualTo(VACCINE_ID);
            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve criar a vacina sem clinica quando clinicId nao e informado")
        void deveCriarSemClinica() {
            var semClinica = Vaccine.builder()
                    .vaccineId(VACCINE_ID)
                    .pet(pet())
                    .vaccineName("Antirrabica")
                    .build();

            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(semClinica);

            var request = VaccineRequestDTO.builder().petId(PET_ID).vaccineName("Antirrabica").build();
            var result = vaccineService.createVaccine(request);

            assertThat(result.getClinicId()).isNull();
            verifyNoInteractions(clinicRepository);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getClinic()).isNull();
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem salvar quando o pet nao existe")
        void deveLancarQuandoPetNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.createVaccine(VaccineRequestDTO.builder().petId(PET_ID).build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            var request = VaccineRequestDTO.builder().petId(PET_ID).clinicId(CLINIC_ID).build();

            assertThatThrownBy(() -> vaccineService.createVaccine(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(103, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateVaccine")
    class UpdateVaccine {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(existingVaccine()));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var request = VaccineRequestDTO.builder().description("Reforco").build();
            var result = vaccineService.updateVaccine(VACCINE_ID, request);

            assertThat(result.getDescription()).isEqualTo("Reforco");
            assertThat(result.getVaccineName()).isEqualTo("Antirrabica");
            assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("deve lancar VACCINE_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.updateVaccine(VACCINE_ID, VaccineRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(104, HttpStatus.NOT_FOUND);

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getVaccineById")
    class GetVaccineById {

        @Test
        @DisplayName("deve retornar a vacina quando existe")
        void deveRetornarQuandoExiste() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(existingVaccine()));

            var result = vaccineService.getVaccineById(VACCINE_ID);

            assertThat(result.getVaccineName()).isEqualTo("Antirrabica");
        }

        @Test
        @DisplayName("deve lancar VACCINE_NOT_FOUND quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.getVaccineById(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");
        }
    }

    @Nested
    @DisplayName("listAllVaccines")
    class ListAllVaccines {

        @Test
        @DisplayName("deve mapear todas as vacinas retornadas pelo repositorio")
        void deveMapearTodasAsVacinas() {
            when(vaccineRepository.findAll()).thenReturn(List.of(existingVaccine()));

            assertThat(vaccineService.listAllVaccines()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("deleteVaccine")
    class DeleteVaccine {

        @Test
        @DisplayName("deve remover a vacina quando existe")
        void deveRemoverQuandoExiste() {
            var vaccine = existingVaccine();
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vaccine));

            vaccineService.deleteVaccine(VACCINE_ID);

            verify(vaccineRepository).delete(vaccine);
        }

        @Test
        @DisplayName("deve lancar VACCINE_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.deleteVaccine(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).delete(any());
        }
    }
}

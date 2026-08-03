package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
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

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private VaccineServiceImpl vaccineService;

    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO_OWNER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Owner owner(UUID id) {
        return Owner.builder().ownerId(id).email("ulysses@petfy.com.br").build();
    }

    private Pet petDe(UUID ownerId) {
        return Pet.builder().petId(PET_ID).name("Rex").owner(owner(ownerId)).build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private Vaccine vacinaDe(UUID ownerId) {
        return Vaccine.builder()
                .vaccineId(VACCINE_ID)
                .pet(petDe(ownerId))
                .clinic(clinic())
                .vaccineName("Antirrabica")
                .applicationDate(LocalDate.of(2025, 6, 1))
                .nextDoseDate(LocalDate.of(2026, 6, 1))
                .description("Dose anual")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    @Nested
    @DisplayName("createVaccine")
    class CreateVaccine {

        @Test
        @DisplayName("deve vincular a vacina ao pet e a clinica informados")
        void deveVincularAoPetEClinica() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(vacinaDe(OWNER_ID));

            var request = VaccineRequestDTO.builder()
                    .petId(PET_ID).clinicId(CLINIC_ID).vaccineName("Antirrabica")
                    .applicationDate(LocalDate.of(2025, 6, 1)).build();

            var result = vaccineService.createVaccine(request);

            assertThat(result.getVaccineId()).isEqualTo(VACCINE_ID);
            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve criar a vacina sem clinica quando clinicId nao e informado")
        void deveCriarSemClinica() {
            var semClinica = Vaccine.builder()
                    .vaccineId(VACCINE_ID).pet(petDe(OWNER_ID)).vaccineName("Antirrabica").build();

            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(vaccineRepository.save(any(Vaccine.class))).thenReturn(semClinica);

            var result = vaccineService.createVaccine(
                    VaccineRequestDTO.builder().petId(PET_ID).vaccineName("Antirrabica").build());

            assertThat(result.getClinicId()).isNull();
            verifyNoInteractions(clinicRepository);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getClinic()).isNull();
        }

        @Test
        @DisplayName("nao deve permitir registrar vacina em pet de outro dono")
        void naoDevePermitirVacinaEmPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().petId(PET_ID).vaccineName("Antirrabica").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem salvar quando o pet nao existe")
        void deveLancarQuandoPetNaoExiste() {
            autenticadoComo(OWNER_ID);
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
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vaccineService.createVaccine(
                    VaccineRequestDTO.builder().petId(PET_ID).clinicId(CLINIC_ID).build()))
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
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacinaDe(OWNER_ID)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = vaccineService.updateVaccine(VACCINE_ID,
                    VaccineRequestDTO.builder().description("Reforco").build());

            assertThat(result.getDescription()).isEqualTo("Reforco");
            assertThat(result.getVaccineName()).isEqualTo("Antirrabica");
            assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve permitir alterar vacina de pet de outro dono")
        void naoDevePermitirAlterarVacinaDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacinaDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> vaccineService.updateVaccine(VACCINE_ID,
                    VaccineRequestDTO.builder().description("Invadido").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar VACCINE_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            autenticadoComo(OWNER_ID);
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
        @DisplayName("deve retornar a vacina quando o pet e do owner autenticado")
        void deveRetornarQuandoDoProprioOwner() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacinaDe(OWNER_ID)));

            assertThat(vaccineService.getVaccineById(VACCINE_ID).getVaccineName()).isEqualTo("Antirrabica");
        }

        @Test
        @DisplayName("deve responder VACCINE_NOT_FOUND para vacina de pet de outro dono")
        void deveResponderNotFoundParaVacinaDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacinaDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> vaccineService.getVaccineById(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");
        }
    }

    @Nested
    @DisplayName("listAllVaccines")
    class ListAllVaccines {

        @Test
        @DisplayName("deve listar apenas as vacinas dos pets do owner autenticado")
        void deveListarApenasDoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findByPetOwnerOwnerId(OWNER_ID)).thenReturn(List.of(vacinaDe(OWNER_ID)));

            assertThat(vaccineService.listAllVaccines()).hasSize(1);
            verify(vaccineRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("deleteVaccine")
    class DeleteVaccine {

        @Test
        @DisplayName("deve remover a vacina do proprio owner")
        void deveRemoverDoProprioOwner() {
            var vaccine = vacinaDe(OWNER_ID);
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vaccine));

            vaccineService.deleteVaccine(VACCINE_ID);

            verify(vaccineRepository).delete(vaccine);
        }

        @Test
        @DisplayName("nao deve permitir remover vacina de pet de outro dono")
        void naoDevePermitirRemoverDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacinaDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> vaccineService.deleteVaccine(VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).delete(any());
        }
    }
}

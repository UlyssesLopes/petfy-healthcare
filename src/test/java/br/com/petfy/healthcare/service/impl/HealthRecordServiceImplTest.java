package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
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
class HealthRecordServiceImplTest {

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID PET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private Pet pet() {
        return Pet.builder().petId(PET_ID).name("Rex").build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private HealthRecord existingRecord() {
        return HealthRecord.builder()
                .healthRecordId(RECORD_ID)
                .pet(pet())
                .clinic(clinic())
                .eventType("Consulta")
                .eventDate(LocalDate.of(2025, 6, 1))
                .description("Retorno de rotina")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    @Nested
    @DisplayName("createHealthRecord")
    class CreateHealthRecord {

        @Test
        @DisplayName("deve vincular o registro ao pet e a clinica informados")
        void deveVincularAoPetEClinica() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(existingRecord());

            var request = HealthRecordRequestDTO.builder()
                    .petId(PET_ID)
                    .clinicId(CLINIC_ID)
                    .eventType("Consulta")
                    .eventDate(LocalDate.of(2025, 6, 1))
                    .build();

            var result = healthRecordService.createHealthRecord(request);

            assertThat(result.getHealthRecordId()).isEqualTo(RECORD_ID);
            assertThat(result.getPetId()).isEqualTo(PET_ID);
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);

            var captor = ArgumentCaptor.forClass(HealthRecord.class);
            verify(healthRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("deve criar o registro sem clinica quando clinicId nao e informado")
        void deveCriarSemClinica() {
            var semClinica = HealthRecord.builder()
                    .healthRecordId(RECORD_ID)
                    .pet(pet())
                    .eventType("Consulta")
                    .build();

            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(semClinica);

            var request = HealthRecordRequestDTO.builder().petId(PET_ID).eventType("Consulta").build();
            var result = healthRecordService.createHealthRecord(request);

            assertThat(result.getClinicId()).isNull();
            verifyNoInteractions(clinicRepository);
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem salvar quando o pet nao existe")
        void deveLancarQuandoPetNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            var request = HealthRecordRequestDTO.builder().petId(PET_ID).eventType("Consulta").build();

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            var request = HealthRecordRequestDTO.builder()
                    .petId(PET_ID).clinicId(CLINIC_ID).eventType("Consulta").build();

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(request))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(healthRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getHealthRecordById")
    class GetHealthRecordById {

        @Test
        @DisplayName("deve retornar o registro quando existe")
        void deveRetornarQuandoExiste() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(existingRecord()));

            var result = healthRecordService.getHealthRecordById(RECORD_ID);

            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getDescription()).isEqualTo("Retorno de rotina");
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.getHealthRecordById(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(105, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("listHealthRecordsByPet")
    class ListHealthRecordsByPet {

        @Test
        @DisplayName("deve retornar o historico do pet")
        void deveRetornarHistoricoDoPet() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(PET_ID))
                    .thenReturn(List.of(existingRecord()));

            var result = healthRecordService.listHealthRecordsByPet(PET_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetId()).isEqualTo(PET_ID);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando o pet existe e ainda nao tem historico")
        void deveRetornarListaVaziaQuandoPetSemHistorico() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(pet()));
            when(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(PET_ID)).thenReturn(List.of());

            assertThat(healthRecordService.listHealthRecordsByPet(PET_ID)).isEmpty();
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND quando o pet nao existe, em vez de devolver lista vazia")
        void deveLancarQuandoPetNaoExiste() {
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.listHealthRecordsByPet(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(healthRecordRepository);
        }
    }

    @Nested
    @DisplayName("listAllHealthRecords")
    class ListAllHealthRecords {

        @Test
        @DisplayName("deve mapear todos os registros retornados pelo repositorio")
        void deveMapearTodosOsRegistros() {
            when(healthRecordRepository.findAll()).thenReturn(List.of(existingRecord()));

            assertThat(healthRecordService.listAllHealthRecords()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("updateHealthRecord")
    class UpdateHealthRecord {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(existingRecord()));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var request = HealthRecordRequestDTO.builder().description("Alta").build();
            var result = healthRecordService.updateHealthRecord(RECORD_ID, request);

            assertThat(result.getDescription()).isEqualTo("Alta");
            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getEventDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.updateHealthRecord(RECORD_ID, HealthRecordRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteHealthRecord")
    class DeleteHealthRecord {

        @Test
        @DisplayName("deve remover o registro quando existe")
        void deveRemoverQuandoExiste() {
            var record = existingRecord();
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record));

            healthRecordService.deleteHealthRecord(RECORD_ID);

            verify(healthRecordRepository).delete(record);
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.deleteHealthRecord(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).delete(any());
        }
    }
}

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @Mock
    private HealthRecordCorrectionLog healthRecordCorrectionLog;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
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

    private HealthRecord registroDe(UUID ownerId) {
        return HealthRecord.builder()
                .healthRecordId(RECORD_ID)
                .pet(petDe(ownerId))
                .clinic(clinic())
                .eventType("Consulta")
                .eventDate(LocalDate.of(2025, 6, 1))
                .description("Retorno de rotina")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    private void autenticadoComo(UUID ownerId) {
        when(currentOwnerProvider.require()).thenReturn(owner(ownerId));
    }

    @Nested
    @DisplayName("createHealthRecord")
    class CreateHealthRecord {

        @Test
        @DisplayName("deve vincular o registro ao pet e a clinica informados")
        void deveVincularAoPetEClinica() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(registroDe(OWNER_ID));

            var request = HealthRecordRequestDTO.builder()
                    .petId(PET_ID).clinicId(CLINIC_ID).eventType("Consulta")
                    .eventDate(LocalDate.of(2025, 6, 1)).build();

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
                    .healthRecordId(RECORD_ID).pet(petDe(OWNER_ID)).eventType("Consulta").build();

            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(semClinica);

            var result = healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().petId(PET_ID).eventType("Consulta").build());

            assertThat(result.getClinicId()).isNull();
            verifyNoInteractions(clinicRepository);
        }

        @Test
        @DisplayName("nao deve permitir criar registro em pet de outro dono")
        void naoDevePermitirRegistroEmPetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().petId(PET_ID).eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND sem salvar quando o pet nao existe")
        void deveLancarQuandoPetNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().petId(PET_ID).eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(102, HttpStatus.NOT_FOUND);

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().petId(PET_ID).clinicId(CLINIC_ID).eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(healthRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getHealthRecordById")
    class GetHealthRecordById {

        @Test
        @DisplayName("deve retornar o registro quando o pet e do owner autenticado")
        void deveRetornarQuandoDoProprioOwner() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registroDe(OWNER_ID)));

            var result = healthRecordService.getHealthRecordById(RECORD_ID);

            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getDescription()).isEqualTo("Retorno de rotina");
        }

        @Test
        @DisplayName("deve responder HEALTH_RECORD_NOT_FOUND para registro de pet de outro dono")
        void deveResponderNotFoundParaRegistroDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registroDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> healthRecordService.getHealthRecordById(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND com 404 quando nao existe")
        void deveLancarQuandoNaoExiste() {
            autenticadoComo(OWNER_ID);
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
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(PET_ID))
                    .thenReturn(List.of(registroDe(OWNER_ID)));

            var result = healthRecordService.listHealthRecordsByPet(PET_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetId()).isEqualTo(PET_ID);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando o pet existe e ainda nao tem historico")
        void deveRetornarListaVaziaQuandoPetSemHistorico() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OWNER_ID)));
            when(healthRecordRepository.findByPetPetIdOrderByEventDateDesc(PET_ID)).thenReturn(List.of());

            assertThat(healthRecordService.listHealthRecordsByPet(PET_ID)).isEmpty();
        }

        @Test
        @DisplayName("nao deve expor o historico de pet de outro dono")
        void naoDeveExporHistoricoDePetDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(petRepository.findById(PET_ID)).thenReturn(Optional.of(petDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> healthRecordService.listHealthRecordsByPet(PET_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Pet not found");

            verifyNoInteractions(healthRecordRepository);
        }

        @Test
        @DisplayName("deve lancar PET_NOT_FOUND quando o pet nao existe, em vez de devolver lista vazia")
        void deveLancarQuandoPetNaoExiste() {
            autenticadoComo(OWNER_ID);
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
        @DisplayName("deve listar apenas os registros dos pets do owner autenticado")
        void deveListarApenasDoOwnerAutenticado() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(0, 20);
            when(healthRecordRepository.findByPetOwnerOwnerIdOrderByEventDateDesc(OWNER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(registroDe(OWNER_ID))));

            assertThat(healthRecordService.listAllHealthRecords(pageable).getContent()).hasSize(1);
            verify(healthRecordRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("updateHealthRecord")
    class UpdateHealthRecord {

        @Test
        @DisplayName("deve preservar os campos nao enviados no request")
        void devePreservarCamposNaoEnviados() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registroDe(OWNER_ID)));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var result = healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().description("Alta").build());

            assertThat(result.getDescription()).isEqualTo("Alta");
            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getEventDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("nao deve permitir alterar registro de pet de outro dono")
        void naoDevePermitirAlterarDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registroDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().description("Invadido").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteHealthRecord")
    class DeleteHealthRecord {

        @Test
        @DisplayName("deve remover o registro do proprio owner")
        void deveRemoverDoProprioOwner() {
            var registro = registroDe(OWNER_ID);
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro));

            healthRecordService.deleteHealthRecord(RECORD_ID);

            verify(healthRecordRepository).delete(registro);
        }

        @Test
        @DisplayName("nao deve permitir remover registro de pet de outro dono")
        void naoDevePermitirRemoverDeOutroDono() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registroDe(OUTRO_OWNER_ID)));

            assertThatThrownBy(() -> healthRecordService.deleteHealthRecord(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND sem remover quando nao existe")
        void deveLancarSemRemoverQuandoNaoExiste() {
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.deleteHealthRecord(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).delete(any());
        }
    }
}

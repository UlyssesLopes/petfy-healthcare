package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
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
    private AnimalAccessGuard animalAccessGuard;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    @Mock
    private HealthRecordCorrectionLog healthRecordCorrectionLog;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Person person(UUID id) {
        return Person.builder().personId(id).email("ulysses@petfy.com.br").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").tutors(PetTutores.titular(person(OWNER_ID))).build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(CLINIC_ID).name("Clinica Bicho Feliz").build();
    }

    private HealthRecord registro() {
        return HealthRecord.builder()
                .healthRecordId(RECORD_ID)
                .animal(animal())
                .clinic(clinic())
                .eventType("Consulta")
                .eventDate(LocalDate.of(2025, 6, 1))
                .description("Retorno de rotina")
                .creationDate(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    private void autenticadoComo(UUID personId) {
        when(currentPersonProvider.require()).thenReturn(person(personId));
    }

    /** Se a pessoa autenticada chega ao animal do registro - decisao do guard. */
    private void alcancaOAnimal(boolean alcanca) {
        when(animalAccessGuard.alcanca(ANIMAL_ID)).thenReturn(alcanca);
    }

    private PetfyHealthcareException animalNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    @Nested
    @DisplayName("createHealthRecord")
    class CreateHealthRecord {

        @Test
        @DisplayName("deve vincular o registro ao animal e a clinica informados")
        void deveVincularAoAnimalEClinica() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.of(clinic()));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(registro());

            var request = HealthRecordRequestDTO.builder()
                    .animalId(ANIMAL_ID).clinicId(CLINIC_ID).eventType("Consulta")
                    .eventDate(LocalDate.of(2025, 6, 1)).build();

            var result = healthRecordService.createHealthRecord(request);

            assertThat(result.getHealthRecordId()).isEqualTo(RECORD_ID);
            assertThat(result.getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);

            var captor = ArgumentCaptor.forClass(HealthRecord.class);
            verify(healthRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getCreationDate()).isNotNull();
        }

        @Test
        @DisplayName("deve criar o registro sem clinica quando clinicId nao e informado")
        void deveCriarSemClinica() {
            var semClinica = HealthRecord.builder()
                    .healthRecordId(RECORD_ID).animal(animal()).eventType("Consulta").build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.save(any(HealthRecord.class))).thenReturn(semClinica);

            var result = healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().animalId(ANIMAL_ID).eventType("Consulta").build());

            assertThat(result.getClinicId()).isNull();
            verifyNoInteractions(clinicRepository);
        }

        /**
         * Animal inalcancavel e animal inexistente sao a mesma resposta - quem decide e
         * o guard. O que cabe ao servico e nao gravar nada quando ele recusa.
         */
        @Test
        @DisplayName("recusa do guard sobe intacta e nada e salvo")
        void recusaDoGuardNaoSalva() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().animalId(ANIMAL_ID).eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar CLINIC_NOT_FOUND sem salvar quando a clinica informada nao existe")
        void deveLancarQuandoClinicaNaoExiste() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(clinicRepository.findById(CLINIC_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().animalId(ANIMAL_ID).clinicId(CLINIC_ID).eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Clinic not found");

            verify(healthRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getHealthRecordById")
    class GetHealthRecordById {

        @Test
        @DisplayName("deve retornar o registro quando o animal e alcancavel")
        void deveRetornarQuandoAlcancavel() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(true);

            var result = healthRecordService.getHealthRecordById(RECORD_ID);

            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getDescription()).isEqualTo("Retorno de rotina");
        }

        /**
         * O registro existe, mas o animal dele esta fora do alcance: a resposta e a
         * mesma de registro inexistente, senao o status vira confirmacao de que
         * aquele id existe.
         */
        @Test
        @DisplayName("registro de animal fora do alcance responde HEALTH_RECORD_NOT_FOUND")
        void registroForaDoAlcanceResponde404() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> healthRecordService.getHealthRecordById(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found")
                    .extracting("code", "httpStatus")
                    .containsExactly(105, HttpStatus.NOT_FOUND);
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

            verify(animalAccessGuard, never()).alcanca(any());
        }
    }

    @Nested
    @DisplayName("listHealthRecordsByAnimal")
    class ListHealthRecordsByAnimal {

        @Test
        @DisplayName("deve retornar o historico do animal")
        void deveRetornarHistoricoDoAnimal() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(registro()));

            var result = healthRecordService.listHealthRecordsByAnimal(ANIMAL_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando o animal existe e ainda nao tem historico")
        void deveRetornarListaVaziaQuandoAnimalSemHistorico() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID)).thenReturn(List.of());

            assertThat(healthRecordService.listHealthRecordsByAnimal(ANIMAL_ID)).isEmpty();
        }

        /**
         * A checagem vem antes da consulta: animal fora do alcance nao pode virar
         * lista vazia, que seria indistinguivel de animal sem historico e ja diria
         * que o animal existe.
         */
        @Test
        @DisplayName("recusa do guard impede a consulta ao historico")
        void recusaDoGuardNaoConsulta() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenThrow(animalNaoEncontrado());

            assertThatThrownBy(() -> healthRecordService.listHealthRecordsByAnimal(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verifyNoInteractions(healthRecordRepository);
        }
    }

    @Nested
    @DisplayName("listAllHealthRecords")
    class ListAllHealthRecords {

        @Test
        @DisplayName("deve listar apenas os registros dos animals em que a pessoa e tutora")
        void deveListarApenasDosAnimalsEmQueETutora() {
            autenticadoComo(OWNER_ID);
            var pageable = PageRequest.of(0, 20);
            when(healthRecordRepository.findByAnimalTutorsPersonPersonIdOrderByEventDateDesc(OWNER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(registro())));

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
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(true);
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var result = healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().description("Alta").build());

            assertThat(result.getDescription()).isEqualTo("Alta");
            assertThat(result.getEventType()).isEqualTo("Consulta");
            assertThat(result.getEventDate()).isEqualTo(LocalDate.of(2025, 6, 1));
            assertThat(result.getUpdateDate()).isNotNull();
        }

        @Test
        @DisplayName("registro de animal fora do alcance nao e alterado")
        void registroForaDoAlcanceNaoEAlterado() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().description("Invadido").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
            verifyNoInteractions(healthRecordCorrectionLog);
        }

        @Test
        @DisplayName("deve lancar HEALTH_RECORD_NOT_FOUND sem salvar quando nao existe")
        void deveLancarSemSalvarQuandoNaoExiste() {
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
        @DisplayName("deve remover o registro alcancavel")
        void deveRemoverOAlcancavel() {
            var registro = registro();
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro));
            alcancaOAnimal(true);

            healthRecordService.deleteHealthRecord(RECORD_ID);

            verify(healthRecordRepository).delete(registro);
        }

        @Test
        @DisplayName("registro de animal fora do alcance nao e removido")
        void registroForaDoAlcanceNaoERemovido() {
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(false);

            assertThatThrownBy(() -> healthRecordService.deleteHealthRecord(RECORD_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).delete(any());
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

    /**
     * O nivel que cada operacao exige do guard. Antes da V15 cada metodo comparava
     * o dono aqui dentro; o que sobrou ao servico e pedir o nivel certo.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("registrar atendimento exige escrita")
        void registrarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            healthRecordService.createHealthRecord(
                    HealthRecordRequestDTO.builder().animalId(ANIMAL_ID).eventType("Consulta").build());

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        /**
         * Ler o historico e leitura. Exigir escrita aqui esconderia o historico de
         * quem acompanha o animal sem editar - o VIEWER existe exatamente para isso.
         */
        @Test
        @DisplayName("ler o historico do animal exige so leitura")
        void lerHistoricoExigeSoLeitura() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID)).thenReturn(List.of());

            healthRecordService.listHealthRecordsByAnimal(ANIMAL_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireEscrita(any());
        }

        /**
         * Ler, corrigir e apagar o registro passam por {@code alcanca}: quem chega
         * ao animal chega ao historico dele. O nivel fino fica nos endpoints de animal.
         */
        @Test
        @DisplayName("mover o registro para outro animal exige escrita no destino")
        void moverExigeEscritaNoDestino() {
            var destinoId = UUID.fromString("99999999-9999-9999-9999-999999999999");
            autenticadoComo(OWNER_ID);
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(registro()));
            alcancaOAnimal(true);
            when(animalAccessGuard.requireEscrita(destinoId)).thenReturn(
                    Animal.builder().animalId(destinoId).name("Bob").tutors(PetTutores.titular(person(OWNER_ID))).build());
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            healthRecordService.updateHealthRecord(RECORD_ID,
                    HealthRecordRequestDTO.builder().animalId(destinoId).build());

            verify(animalAccessGuard).requireEscrita(destinoId);
        }
    }
}

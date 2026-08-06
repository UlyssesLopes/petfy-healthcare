package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.AccessedResource;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.ClinicActivityNotifier;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineFactory;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetPetServiceImplTest {

    @Mock
    private PetClinicAccessRepository petClinicAccessRepository;

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private CurrentProfessionalProvider currentProfessionalProvider;

    @Mock
    private VaccineCatalogRepository vaccineCatalogRepository;

    @Mock
    private ClinicActivityNotifier clinicActivityNotifier;

    @Mock
    private VaccineCorrectionLog vaccineCorrectionLog;

    @Mock
    private HealthRecordRepository healthRecordRepository;

    @Mock
    private HealthRecordCorrectionLog healthRecordCorrectionLog;

    @Mock
    private SensitiveAccessLogger sensitiveAccessLogger;

    private VetPetServiceImpl service;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CLINIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OUTRA_CLINIC_ID = UUID.fromString("aaaaaaaa-5555-5555-5555-555555555555");
    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @BeforeEach
    void setUp() {
        // factory real: o que interessa aqui e que a vacina saia carimbada com a
        // clinica certa, e nao repetir a regra de montagem num mock
        service = new VetPetServiceImpl(petClinicAccessRepository, vaccineRepository,
                currentProfessionalProvider, new VaccineFactory(vaccineCatalogRepository),
                clinicActivityNotifier, vaccineCorrectionLog,
                healthRecordRepository, healthRecordCorrectionLog, sensitiveAccessLogger);
        ReflectionTestUtils.setField(service, "correctionWindowDays", 7);
    }

    private Clinic clinic(UUID id) {
        return Clinic.builder().clinicId(id).name("Clinica Bicho Feliz").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").type("Cachorro").weight(12.5)
                .tutors(br.com.petfy.healthcare.PetTutores.titular(Person.builder().personId(UUID.randomUUID()).name("Ulysses").email("ulysses@petfy.com.br").phone("11999999999").build()))
                .build();
    }

    private PetClinicAccess acesso(LocalDateTime revokedAt) {
        return PetClinicAccess.builder()
                .petClinicAccessId(UUID.randomUUID())
                .animal(animal())
                .clinic(clinic(CLINIC_ID))
                .grantedAt(LocalDateTime.now().minusDays(3))
                .revokedAt(revokedAt)
                .build();
    }

    private void vetDaClinica(UUID clinicId) {
        when(currentProfessionalProvider.require()).thenReturn(Person.builder()
                .personId(UUID.randomUUID()).name("Dra. Marina").clinic(clinic(clinicId)).build());
    }

    @Nested
    @DisplayName("listAccessibleAnimals")
    class ListAccessibleAnimals {

        @Test
        @DisplayName("deve listar apenas os animals com concessao ativa para a clinica do vet")
        void deveListarAnimalsComConcessaoAtiva() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(CLINIC_ID))
                    .thenReturn(List.of(acesso(null)));

            var result = service.listAccessibleAnimals();

            assertThat(result).singleElement().satisfies(p -> {
                assertThat(p.getAnimalId()).isEqualTo(ANIMAL_ID);
                assertThat(p.getName()).isEqualTo("Rex");
                assertThat(p.getPersonName()).isEqualTo("Ulysses");
                assertThat(p.getAccessGrantedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("nao deve expor os dados de contato do tutor")
        void naoDeveExporContatoDoTutor() {
            assertThat(VetPetDTO.class.getDeclaredFields())
                    .extracting(Field::getName)
                    .contains("personName")
                    .doesNotContain("personEmail", "personPhone", "personAddress", "personId");
        }

        @Test
        @DisplayName("deve devolver lista vazia quando a clinica nao foi autorizada por ninguem")
        void deveDevolverListaVaziaSemAutorizacoes() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(CLINIC_ID))
                    .thenReturn(List.of());

            assertThat(service.listAccessibleAnimals()).isEmpty();
        }
    }

    @Nested
    @DisplayName("registerVaccine")
    class RegisterVaccine {

        private VaccineRequestDTO request() {
            return VaccineRequestDTO.builder()
                    .vaccineName("V10")
                    .applicationDate(LocalDate.of(2026, 8, 1))
                    .build();
        }

        @Test
        @DisplayName("deve registrar a vacina no animal autorizado")
        void deveRegistrarVacinaNoAnimalAutorizado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.registerVaccine(ANIMAL_ID, request());

            assertThat(result.getVaccineName()).isEqualTo("V10");
            assertThat(result.getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("deve carimbar a vacina com a clinica do vet, e nao com a do payload")
        void deveCarimbarComAClinicaDoVet() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var comOutraClinicaNoCorpo = VaccineRequestDTO.builder()
                    .vaccineName("V10")
                    .clinicId(OUTRA_CLINIC_ID)
                    .applicationDate(LocalDate.of(2026, 8, 1))
                    .build();

            service.registerVaccine(ANIMAL_ID, comOutraClinicaNoCorpo);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getClinic().getClinicId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve usar o animalId do path, ignorando o que vier no corpo")
        void deveUsarAnimalIdDoPath() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var comOutroAnimalNoCorpo = VaccineRequestDTO.builder()
                    .animalId(UUID.randomUUID())
                    .vaccineName("V10")
                    .build();

            service.registerVaccine(ANIMAL_ID, comOutroAnimalNoCorpo);

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(vaccineRepository).save(captor.capture());
            assertThat(captor.getValue().getAnimal().getAnimalId()).isEqualTo(ANIMAL_ID);
        }

        @Test
        @DisplayName("deve avisar o tutor de que a clinica registrou algo no animal dele")
        void deveAvisarOTutor() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            service.registerVaccine(ANIMAL_ID, request());

            var captor = ArgumentCaptor.forClass(Vaccine.class);
            verify(clinicActivityNotifier).vaccineRecorded(captor.capture());
            assertThat(captor.getValue().getVaccineName()).isEqualTo("V10");
        }

        @Test
        @DisplayName("nao deve avisar quando o registro foi recusado")
        void naoDeveAvisarQuandoRegistroRecusado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerVaccine(ANIMAL_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class);

            verify(clinicActivityNotifier, never()).vaccineRecorded(any());
        }

        @Test
        @DisplayName("nao deve registrar em animal sem concessao para a clinica do vet")
        void naoDeveRegistrarSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerVaccine(ANIMAL_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve registrar quando a concessao foi revogada")
        void naoDeveRegistrarComConcessaoRevogada() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(LocalDateTime.now().minusDays(1))));

            assertThatThrownBy(() -> service.registerVaccine(ANIMAL_ID, request()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("correctVaccine")
    class CorrectVaccine {

        private Vaccine registrada(UUID clinicId, LocalDateTime registradaEm) {
            return Vaccine.builder()
                    .vaccineId(VACCINE_ID)
                    .vaccineName("V10")
                    .applicationDate(LocalDate.of(2026, 8, 1))
                    .description("original")
                    // clinicId nulo significa vacina lancada pelo tutor, sem clinica
                    .clinic(clinicId != null ? clinic(clinicId) : null)
                    .animal(animal())
                    .creationDate(registradaEm)
                    .build();
        }

        private void baseTem(Vaccine vaccine) {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vaccine));
        }

        @Test
        @DisplayName("deve corrigir registro da propria clinica dentro da janela")
        void deveCorrigirDentroDaJanela() {
            baseTem(registrada(CLINIC_ID, LocalDateTime.now().minusDays(1)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build());

            assertThat(result.getVaccineName()).isEqualTo("V8");
        }

        @Test
        @DisplayName("deve preservar os campos nao enviados")
        void devePreservarCamposNaoEnviados() {
            baseTem(registrada(CLINIC_ID, LocalDateTime.now().minusDays(1)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().description("corrigido").build());

            assertThat(result.getDescription()).isEqualTo("corrigido");
            assertThat(result.getVaccineName()).isEqualTo("V10");
            assertThat(result.getApplicationDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        }

        @Test
        @DisplayName("deve gravar o rastro ANTES de alterar, senao guarda o estado novo")
        void deveGravarRastroAntesDeAlterar() {
            var vaccine = registrada(CLINIC_ID, LocalDateTime.now().minusDays(1));
            baseTem(vaccine);
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            // captura o estado no instante da chamada ao recorder
            var nomeNoMomentoDoRastro = new String[1];
            doAnswer(invocation -> {
                nomeNoMomentoDoRastro[0] = ((Vaccine) invocation.getArgument(0)).getVaccineName();
                return null;
            }).when(vaccineCorrectionLog).recordByProfessional(any(), any(), any());

            service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build());

            assertThat(nomeNoMomentoDoRastro[0]).isEqualTo("V10");
        }

        @Test
        @DisplayName("deve avisar o tutor de que a clinica mexeu no registro")
        void deveAvisarOTutorDaCorrecao() {
            baseTem(registrada(CLINIC_ID, LocalDateTime.now().minusDays(1)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build());

            verify(clinicActivityNotifier).vaccineCorrected(any(Vaccine.class));
        }

        @Test
        @DisplayName("nao deve corrigir depois de fechada a janela")
        void naoDeveCorrigirForaDaJanela() {
            baseTem(registrada(CLINIC_ID, LocalDateTime.now().minusDays(8)));

            assertThatThrownBy(() -> service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(112, HttpStatus.CONFLICT);

            verify(vaccineRepository, never()).save(any());
            verify(vaccineCorrectionLog, never()).recordByProfessional(any(), any(), any());
        }

        @Test
        @DisplayName("nao deve corrigir registro de outra clinica")
        void naoDeveCorrigirDeOutraClinica() {
            baseTem(registrada(OUTRA_CLINIC_ID, LocalDateTime.now().minusDays(1)));

            assertThatThrownBy(() -> service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve corrigir vacina lancada pelo proprio tutor")
        void naoDeveCorrigirVacinaDoTutor() {
            baseTem(registrada(null, LocalDateTime.now().minusDays(1)));

            assertThatThrownBy(() -> service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve corrigir vacina de animal sem concessao ativa")
        void naoDeveCorrigirSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.correctVaccine(ANIMAL_ID, VACCINE_ID,
                    VaccineRequestDTO.builder().vaccineName("V8").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(vaccineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listCorrections")
    class ListCorrections {

        @Test
        @DisplayName("deve devolver o rastro de qualquer vacina do animal autorizado")
        void deveDevolverRastroDoAnimalAutorizado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(
                    Vaccine.builder().vaccineId(VACCINE_ID).animal(animal()).build()));
            when(vaccineCorrectionLog.list(VACCINE_ID)).thenReturn(List.of());

            assertThat(service.listCorrections(ANIMAL_ID, VACCINE_ID)).isEmpty();
            verify(vaccineCorrectionLog).list(VACCINE_ID);
        }

        @Test
        @DisplayName("nao deve devolver rastro de animal sem concessao ativa")
        void naoDeveDevolverSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listCorrections(ANIMAL_ID, VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(vaccineCorrectionLog, never()).list(any());
        }

        @Test
        @DisplayName("nao deve devolver rastro de vacina que nao e daquele animal")
        void naoDeveDevolverDeVacinaDeOutroAnimal() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(
                    Vaccine.builder().vaccineId(VACCINE_ID)
                            .animal(Animal.builder().animalId(UUID.randomUUID()).build()).build()));

            assertThatThrownBy(() -> service.listCorrections(ANIMAL_ID, VACCINE_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Vaccine not found");

            verify(vaccineCorrectionLog, never()).list(any());
        }
    }

    @Nested
    @DisplayName("historico de saude")
    class HistoricoDeSaude {

        private HealthRecord registro(UUID clinicId, LocalDateTime registradoEm) {
            return HealthRecord.builder()
                    .healthRecordId(RECORD_ID)
                    .animal(animal())
                    .clinic(clinicId != null ? clinic(clinicId) : null)
                    .eventType("Consulta")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .description("original")
                    .creationDate(registradoEm)
                    .build();
        }

        private void comAcesso() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
        }

        @Test
        @DisplayName("deve registrar o atendimento carimbado com a clinica do vet")
        void deveRegistrarComAClinicaDoVet() {
            comAcesso();
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var comOutraClinicaNoCorpo = HealthRecordRequestDTO.builder()
                    .clinicId(OUTRA_CLINIC_ID)
                    .eventType("Cirurgia")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .build();

            var result = service.registerHealthRecord(ANIMAL_ID, comOutraClinicaNoCorpo);

            assertThat(result.getEventType()).isEqualTo("Cirurgia");
            assertThat(result.getClinicId()).isEqualTo(CLINIC_ID);
        }

        @Test
        @DisplayName("deve avisar o tutor do atendimento registrado")
        void deveAvisarOTutorDoAtendimento() {
            comAcesso();
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            service.registerHealthRecord(ANIMAL_ID,
                    HealthRecordRequestDTO.builder().eventType("Consulta").build());

            verify(clinicActivityNotifier).healthRecordRecorded(any(HealthRecord.class));
        }

        @Test
        @DisplayName("nao deve registrar atendimento em animal sem concessao ativa")
        void naoDeveRegistrarSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registerHealthRecord(ANIMAL_ID,
                    HealthRecordRequestDTO.builder().eventType("Consulta").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve corrigir atendimento da propria clinica dentro da janela")
        void deveCorrigirDentroDaJanela() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(CLINIC_ID, LocalDateTime.now().minusDays(1))));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var result = service.correctHealthRecord(ANIMAL_ID, RECORD_ID,
                    HealthRecordRequestDTO.builder().description("corrigido").build());

            assertThat(result.getDescription()).isEqualTo("corrigido");
            assertThat(result.getEventType()).isEqualTo("Consulta");
            verify(healthRecordCorrectionLog).recordByProfessional(any(HealthRecord.class), any(), any());
            verify(clinicActivityNotifier).healthRecordCorrected(any(HealthRecord.class));
        }

        @Test
        @DisplayName("deve gravar o rastro ANTES de alterar, senao guarda o estado novo")
        void deveGravarRastroAntesDeAlterar() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(CLINIC_ID, LocalDateTime.now().minusDays(1))));
            when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(i -> i.getArgument(0));

            var tipoNoMomentoDoRastro = new String[1];
            doAnswer(invocation -> {
                tipoNoMomentoDoRastro[0] = ((HealthRecord) invocation.getArgument(0)).getEventType();
                return null;
            }).when(healthRecordCorrectionLog).recordByProfessional(any(), any(), any());

            service.correctHealthRecord(ANIMAL_ID, RECORD_ID,
                    HealthRecordRequestDTO.builder().eventType("Cirurgia").build());

            assertThat(tipoNoMomentoDoRastro[0]).isEqualTo("Consulta");
        }

        @Test
        @DisplayName("nao deve corrigir atendimento depois de fechada a janela")
        void naoDeveCorrigirForaDaJanela() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(CLINIC_ID, LocalDateTime.now().minusDays(8))));

            assertThatThrownBy(() -> service.correctHealthRecord(ANIMAL_ID, RECORD_ID,
                    HealthRecordRequestDTO.builder().eventType("Cirurgia").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(112, HttpStatus.CONFLICT);

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve corrigir atendimento lancado pelo proprio tutor")
        void naoDeveCorrigirAtendimentoDoTutor() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(null, LocalDateTime.now().minusDays(1))));

            assertThatThrownBy(() -> service.correctHealthRecord(ANIMAL_ID, RECORD_ID,
                    HealthRecordRequestDTO.builder().eventType("Cirurgia").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("nao deve corrigir atendimento de outra clinica")
        void naoDeveCorrigirDeOutraClinica() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(OUTRA_CLINIC_ID, LocalDateTime.now().minusDays(1))));

            assertThatThrownBy(() -> service.correctHealthRecord(ANIMAL_ID, RECORD_ID,
                    HealthRecordRequestDTO.builder().eventType("Cirurgia").build()))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Health record not found");

            verify(healthRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve listar o historico do animal autorizado")
        void deveListarHistoricoDoAnimalAutorizado() {
            comAcesso();
            when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(registro(CLINIC_ID, LocalDateTime.now())));

            assertThat(service.listHealthRecords(ANIMAL_ID)).hasSize(1);
        }

        @Test
        @DisplayName("nao deve expor o historico de animal sem concessao")
        void naoDeveExporHistoricoSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listHealthRecords(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(healthRecordRepository, never()).findByAnimalAnimalIdOrderByEventDateDesc(any());
        }

        @Test
        @DisplayName("deve devolver o rastro do atendimento do animal autorizado")
        void deveDevolverRastroDoAtendimento() {
            comAcesso();
            when(healthRecordRepository.findById(RECORD_ID))
                    .thenReturn(Optional.of(registro(CLINIC_ID, LocalDateTime.now())));
            when(healthRecordCorrectionLog.list(RECORD_ID)).thenReturn(List.of());

            assertThat(service.listHealthRecordCorrections(ANIMAL_ID, RECORD_ID)).isEmpty();
            verify(healthRecordCorrectionLog).list(RECORD_ID);
        }
    }

    @Nested
    @DisplayName("listVaccines")
    class ListVaccines {

        @Test
        @DisplayName("deve listar as vacinas do animal autorizado")
        void deveListarVacinasDoAnimalAutorizado() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(Vaccine.builder()
                            .vaccineId(UUID.randomUUID()).vaccineName("V10").animal(animal()).build()));

            assertThat(service.listVaccines(ANIMAL_ID)).hasSize(1);
        }

        @Test
        @DisplayName("nao deve expor o historico de animal sem concessao")
        void naoDeveExporHistoricoSemConcessao() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listVaccines(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .hasMessage("Animal not found");

            verify(vaccineRepository, never()).findByAnimalAnimalIdOrderByApplicationDateDesc(any());
        }
    }

    /**
     * Toda leitura do veterinario deixa rastro.
     *
     * Ate aqui havia rastro de escrita - correcao grava quem alterou - e nenhum de
     * leitura: o vet podia abrir o historico completo e o tutor nunca saberia. Estes
     * casos existem para que uma rota de leitura nova nao entre sem gancho, o que
     * seria invisivel em revisao.
     */
    @Nested
    @DisplayName("registro de leitura")
    class RegistroDeLeitura {

        @Test
        @DisplayName("listar vacinas registra o acesso")
        void listarVacinasRegistra() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(ANIMAL_ID)).thenReturn(List.of());

            service.listVaccines(ANIMAL_ID);

            verify(sensitiveAccessLogger).vetLeu(any(Person.class), any(Animal.class),
                    eq(AccessedResource.VACCINES));
        }

        @Test
        @DisplayName("listar o historico de saude registra o acesso")
        void listarHistoricoRegistra() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(ANIMAL_ID)).thenReturn(List.of());

            service.listHealthRecords(ANIMAL_ID);

            verify(sensitiveAccessLogger).vetLeu(any(Person.class), any(Animal.class),
                    eq(AccessedResource.HEALTH_RECORDS));
        }

        /**
         * O acesso recusado nao entra no log: nao houve leitura de dado nenhum, e
         * registrar tentativa barrada encheria o log de um animal que a clinica nem
         * alcanca - inclusive de animalId que ela usou para adivinhar.
         */
        @Test
        @DisplayName("acesso recusado nao gera registro de leitura")
        void acessoRecusadoNaoRegistra() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.listVaccines(ANIMAL_ID))
                    .isInstanceOf(PetfyHealthcareException.class);

            verifyNoInteractions(sensitiveAccessLogger);
        }

        /**
         * Escrita nao passa por aqui: registrar e corrigir ja deixam rastro proprio em
         * vaccine_corrections e avisam o tutor por e-mail na hora. Duplicar no log de
         * leitura contaria o mesmo fato duas vezes.
         */
        @Test
        @DisplayName("registrar vacina nao entra no log de leitura")
        void escritaNaoEntraNoLogDeLeitura() {
            vetDaClinica(CLINIC_ID);
            when(petClinicAccessRepository.findByAnimalAnimalIdAndClinicClinicId(ANIMAL_ID, CLINIC_ID))
                    .thenReturn(Optional.of(acesso(null)));
            when(vaccineRepository.save(any(Vaccine.class))).thenAnswer(i -> i.getArgument(0));

            service.registerVaccine(ANIMAL_ID, VaccineRequestDTO.builder()
                    .vaccineName("Antirrabica").applicationDate(LocalDate.now()).build());

            verifyNoInteractions(sensitiveAccessLogger);
        }
    }
}

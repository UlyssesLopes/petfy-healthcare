package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.HealthRecordCorrection;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.AnimalShare;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.AnimalWeightHistory;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.service.AnimalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Apagar o animal contra Postgres de verdade.
 *
 * Oito entidades apontam para {@code animals} e o schema nao tem
 * {@code ON DELETE CASCADE} em lugar nenhum, entao quem recusa cada delete e a
 * chave estrangeira - que so existe no banco. Com repositorio mockado, apagar um
 * animal "funciona" mesmo quando ele tem carteira inteira pendurada.
 *
 * O caso coberto aqui e o normal, e nao o extremo: animal com vacina, historico,
 * peso, antiparasitario, link de compartilhamento, acesso de clinica e as
 * correcoes de cada um. Um animal sem nada disso e um animal cadastrado ha cinco
 * minutos.
 */
@SpringBootTest
@Transactional
@DisplayName("apagar animal contra Postgres real")
class AnimalDeletionContainerTest extends PostgresContainerTest {

    @Autowired private AnimalService animalService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private VetRepository vetRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private HealthRecordCorrectionRepository healthRecordCorrectionRepository;
    @Autowired private AnimalShareRepository animalShareRepository;
    @Autowired private PetClinicAccessRepository petClinicAccessRepository;
    @Autowired private AnimalWeightHistoryRepository animalWeightHistoryRepository;
    @Autowired private AntiparasiticRepository antiparasiticRepository;

    private Person ulysses;
    private Animal rex;
    private Clinic bichoFeliz;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.saveAndFlush(Person.builder()
                .name("Ulysses")
                .email("ulysses-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        petTutorRepository.saveAndFlush(PetTutor.builder()
                .animal(rex).person(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());

        bichoFeliz = clinicRepository.saveAndFlush(Clinic.builder().name("Clinica Bicho Feliz").build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ulysses.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    /** A carteira que qualquer animal com algum uso tem. */
    private void comHistoricoCompleto() {
        Vaccine vacina = vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(rex).clinic(bichoFeliz).vaccineName("Antirrabica")
                .applicationDate(LocalDate.now().minusMonths(6))
                .nextDoseDate(LocalDate.now().plusMonths(6))
                .creationDate(LocalDateTime.now()).build());

        vaccineCorrectionRepository.saveAndFlush(VaccineCorrection.builder()
                .vaccine(vacina).correctedByPerson(ulysses).previousVaccineName("Antirabica")
                .correctedAt(LocalDateTime.now()).build());

        HealthRecord atendimento = healthRecordRepository.saveAndFlush(HealthRecord.builder()
                .category(HealthEventCategory.CONSULTA)
                    .animal(rex).clinic(bichoFeliz).eventType("Consulta")
                .eventDate(LocalDate.now().minusMonths(2))
                .creationDate(LocalDateTime.now()).build());

        healthRecordCorrectionRepository.saveAndFlush(HealthRecordCorrection.builder()
                .healthRecord(atendimento).correctedByPerson(ulysses).previousEventType("Retorno")
                .correctedAt(LocalDateTime.now()).build());

        animalWeightHistoryRepository.saveAndFlush(AnimalWeightHistory.builder()
                .animal(rex).weight(12.5).measuredAt(LocalDate.now().minusMonths(1))
                .creationDate(LocalDateTime.now()).build());

        antiparasiticRepository.saveAndFlush(Antiparasitic.builder()
                .animal(rex).name("Vermifugo").kind(AntiparasiticKind.DEWORMER)
                .applicationDate(LocalDate.now().minusMonths(3))
                .creationDate(LocalDateTime.now()).updateDate(LocalDateTime.now()).build());

        animalShareRepository.saveAndFlush(AnimalShare.builder()
                .animal(rex).tokenHash("hash-" + UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .creationDate(LocalDateTime.now()).build());

        petClinicAccessRepository.saveAndFlush(PetClinicAccess.builder()
                .animal(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());
    }

    /**
     * O caso que o teste de mock dava como funcionando.
     */
    @Test
    @DisplayName("apagar o animal leva a carteira inteira junto")
    void apagarAnimalComHistoricoCompleto() {
        comHistoricoCompleto();

        animalService.deleteAnimal(rex.getAnimalId());
        animalRepository.flush();

        assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();

        UUID animalId = rex.getAnimalId();
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId)).isEmpty();
        assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(animalId)).isEmpty();
        assertThat(animalWeightHistoryRepository.findByAnimalAnimalIdOrderByMeasuredAtDesc(animalId)).isEmpty();
        assertThat(antiparasiticRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animalId)).isEmpty();
        assertThat(animalShareRepository.findByAnimalOrderByCreationDateDesc(rex)).isEmpty();
        assertThat(petClinicAccessRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animalId)).isEmpty();
    }

    /**
     * O tutor continua de pe: apagar o animal nao e apagar a conta. Vale dizer porque
     * a limpeza passa por persons nas correcoes, e um delete a mais ali levaria a
     * conta junto.
     */
    @Test
    @DisplayName("apagar o animal nao apaga o tutor nem a clinica")
    void apagarAnimalNaoApagaTutorNemClinica() {
        comHistoricoCompleto();

        animalService.deleteAnimal(rex.getAnimalId());
        animalRepository.flush();

        assertThat(personRepository.findById(ulysses.getPersonId())).isPresent();
        assertThat(clinicRepository.findById(bichoFeliz.getClinicId())).isPresent();
    }

    /** Animal recem-cadastrado, sem nada pendurado: o caso que ja funcionava. */
    @Test
    @DisplayName("apagar animal sem historico continua funcionando")
    void apagarAnimalSemHistorico() {
        animalService.deleteAnimal(rex.getAnimalId());
        animalRepository.flush();

        assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
    }

    /**
     * O animal de outro tutor nao pode ser tocado pela limpeza: os deletes sao por
     * animalId, e um deles escrito com o filtro errado levaria a carteira do animal do
     * vizinho.
     */
    @Test
    @DisplayName("a limpeza nao passa do animal apagado")
    void limpezaNaoPassaDoAnimalApagado() {
        comHistoricoCompleto();

        Animal nina = animalRepository.saveAndFlush(Animal.builder()
                .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
        petTutorRepository.saveAndFlush(PetTutor.builder()
                .animal(nina).person(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(nina).vaccineName("V10").applicationDate(LocalDate.now())
                .creationDate(LocalDateTime.now()).build());

        animalService.deleteAnimal(rex.getAnimalId());
        animalRepository.flush();

        assertThat(animalRepository.findById(nina.getAnimalId())).isPresent();
        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(nina.getAnimalId()))
                .extracting(Vaccine::getVaccineName).containsExactly("V10");
    }

}

package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.AnimalShare;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As consultas por UUID, contra Postgres.
 *
 * Todas estas voltavam vazias no H2 por causa do BINARY(255) de tamanho fixo, o
 * que impedia afirmar qualquer coisa sobre as linhas que elas trazem. Como sao
 * a base do escopo por dono e do acesso do veterinario, "provavelmente funciona
 * no Postgres" era a maior divida da suite: se alguma trouxesse de menos, o
 * tutor deixaria de ver os proprios dados; de mais, veria os de outro.
 */
@SpringBootTest
@Transactional
class UuidQueriesContainerTest extends PostgresContainerTest {

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private ClinicRepository clinicRepository;
    @Autowired private VaccineRepository vaccineRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private AnimalShareRepository animalShareRepository;
    @Autowired private PetClinicAccessRepository petClinicAccessRepository;
    @Autowired private ClinicInviteRepository clinicInviteRepository;
    @Autowired private VaccineCorrectionRepository vaccineCorrectionRepository;

    private Person ulysses;
    private Person maria;
    private Animal rex;
    private Animal nina;
    private Clinic bichoFeliz;
    private Person marina;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.save(Person.builder()
                .name("Ulysses").email("ulysses-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());
        maria = personRepository.save(Person.builder()
                .name("Maria").email("maria-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash").build());

        rex = animalComTitular("Rex", ulysses);
        nina = animalComTitular("Nina", maria);

        bichoFeliz = clinicRepository.save(Clinic.builder().name("Clinica Bicho Feliz").build());

        marina = personRepository.save(Person.builder()
                .name("Dra. Marina").email("marina-" + UUID.randomUUID() + "@vet.com.br")
                .password("hash").clinic(bichoFeliz).build());
    }

    /**
     * O animal e o vinculo sao gravados em duas chamadas, como em
     * {@code AnimalServiceImpl.createAnimal}: {@code Animal.tutors} e {@code mappedBy} sem
     * cascade, entao salvar o animal nao grava tutor nenhum. Montar o vinculo so em
     * memoria - o que a suite fazia enquanto {@code Animal} tinha {@code person} -
     * deixaria toda consulta por tutor voltando vazia, e o teste diria que o
     * escopo por dono nao traz nada quando o que falta e a linha no banco.
     */
    private Animal animalComTitular(String nome, Person titular) {
        Animal animal = animalRepository.save(Animal.builder().name(nome).species(Species.CANINA).build());

        PetTutor vinculo = petTutorRepository.save(PetTutor.builder()
                .animal(animal)
                .person(titular)
                .role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now())
                .build());

        animal.setTutors(new ArrayList<>(List.of(vinculo)));
        return animal;
    }

    private Vaccine vacina(Animal animal, String nome, LocalDate proximaDose) {
        return vaccineRepository.save(Vaccine.builder()
                .animal(animal).vaccineName(nome)
                .applicationDate(LocalDate.now().minusYears(1))
                .nextDoseDate(proximaDose).build());
    }

    @Nested
    @DisplayName("escopo por dono")
    class EscopoPorDono {

        @Test
        @DisplayName("animals devem ser filtrados pelo dono, sem vazar os do outro")
        void animalsFiltradosPeloDono() {
            var doUlysses = animalRepository.findByTutorsPersonPersonId(ulysses.getPersonId());

            assertThat(doUlysses).extracting(Animal::getName).containsExactly("Rex");
            assertThat(animalRepository.findByTutorsPersonPersonId(maria.getPersonId()))
                    .extracting(Animal::getName).containsExactly("Nina");
        }

        /**
         * O caso que so passa a existir depois da V15: o mesmo animal aparece para os
         * dois tutores, e nenhum dos dois alcanca o animal de terceiro. E a promessa
         * do passo 8 verificada contra o Postgres, e nao contra mock.
         */
        @Test
        @DisplayName("animal com dois tutores aparece para os dois, e para mais ninguem")
        void animalCompartilhadoApareceParaOsDoisTutores() {
            petTutorRepository.save(PetTutor.builder()
                    .animal(rex).person(maria).role(PetTutorRole.EDITOR)
                    .invitedBy(ulysses).creationDate(LocalDateTime.now()).build());

            assertThat(animalRepository.findByTutorsPersonPersonId(ulysses.getPersonId()))
                    .extracting(Animal::getName).containsExactly("Rex");
            assertThat(animalRepository.findByTutorsPersonPersonId(maria.getPersonId()))
                    .extracting(Animal::getName).containsExactlyInAnyOrder("Rex", "Nina");

            var estranho = personRepository.save(Person.builder()
                    .name("Estranho").email("estranho-" + UUID.randomUUID() + "@petfy.com.br")
                    .password("hash").build());
            assertThat(animalRepository.findByTutorsPersonPersonId(estranho.getPersonId())).isEmpty();
        }

        /** O co-tutor EDITOR ve a carteira do animal compartilhado, nao so o cadastro. */
        @Test
        @DisplayName("vacinas do animal compartilhado aparecem para o co-tutor")
        void vacinasDoAnimalCompartilhadoAparecemParaOCoTutor() {
            petTutorRepository.save(PetTutor.builder()
                    .animal(rex).person(maria).role(PetTutorRole.EDITOR)
                    .invitedBy(ulysses).creationDate(LocalDateTime.now()).build());
            vacina(rex, "V10", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findByAnimalTutorsPersonPersonId(maria.getPersonId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("vacinas devem ser filtradas pelo dono do animal")
        void vacinasFiltradasPeloDonoDoAnimal() {
            vacina(rex, "V10", LocalDate.now().plusDays(10));
            vacina(nina, "V8", LocalDate.now().plusDays(10));

            assertThat(vaccineRepository.findByAnimalTutorsPersonPersonId(ulysses.getPersonId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("V10");
        }

        @Test
        @DisplayName("historico deve ser filtrado pelo dono do animal, do mais recente ao mais antigo")
        void historicoFiltradoEOrdenado() {
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Antiga").eventDate(LocalDate.now().minusDays(30)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Recente").eventDate(LocalDate.now().minusDays(1)).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(nina).eventType("De outro dono").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findByAnimalTutorsPersonPersonIdOrderByEventDateDesc(ulysses.getPersonId()))
                    .extracting(HealthRecord::getEventType)
                    .containsExactly("Recente", "Antiga");
        }

        @Test
        @DisplayName("historico por animal deve trazer so o daquele animal")
        void historicoPorAnimal() {
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(rex).eventType("Consulta").eventDate(LocalDate.now()).build());
            healthRecordRepository.save(HealthRecord.builder()
                    .category(HealthEventCategory.CONSULTA)
                    .animal(nina).eventType("Cirurgia").eventDate(LocalDate.now()).build());

            assertThat(healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(rex.getAnimalId()))
                    .extracting(HealthRecord::getEventType).containsExactly("Consulta");
        }

        @Test
        @DisplayName("vacinas por animal devem vir da mais recente para a mais antiga")
        void vacinasPorAnimalOrdenadas() {
            vaccineRepository.save(Vaccine.builder().animal(rex).vaccineName("Antiga")
                    .applicationDate(LocalDate.now().minusYears(2)).build());
            vaccineRepository.save(Vaccine.builder().animal(rex).vaccineName("Recente")
                    .applicationDate(LocalDate.now().minusDays(5)).build());

            assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(rex.getAnimalId()))
                    .extracting(Vaccine::getVaccineName).containsExactly("Recente", "Antiga");
        }
    }

    @Nested
    @DisplayName("acesso do veterinario")
    class AcessoDoVeterinario {

        @Test
        @DisplayName("concessao deve ser encontrada pelo par animal e clinica")
        void concessaoEncontradaPeloPar() {
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .animal(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());

            assertThat(petClinicAccessRepository
                    .findByAnimalAnimalIdAndClinicClinicId(rex.getAnimalId(), bichoFeliz.getClinicId()))
                    .isPresent();

            assertThat(petClinicAccessRepository
                    .findByAnimalAnimalIdAndClinicClinicId(nina.getAnimalId(), bichoFeliz.getClinicId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("a clinica deve enxergar so os animals com concessao ativa")
        void clinicaEnxergaApenasConcessoesAtivas() {
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .animal(rex).clinic(bichoFeliz).grantedAt(LocalDateTime.now()).build());
            petClinicAccessRepository.save(PetClinicAccess.builder()
                    .animal(nina).clinic(bichoFeliz).grantedAt(LocalDateTime.now())
                    .revokedAt(LocalDateTime.now()).build());

            assertThat(petClinicAccessRepository
                    .findByClinicClinicIdAndRevokedAtIsNull(bichoFeliz.getClinicId()))
                    .extracting(a -> a.getAnimal().getName())
                    .containsExactly("Rex");
        }

        @Test
        @DisplayName("convites devem ser filtrados pela clinica")
        void convitesFiltradosPelaClinica() {
            clinicInviteRepository.save(br.com.petfy.healthcare.domain.entity.ClinicInvite.builder()
                    .clinic(bichoFeliz).createdBy(marina).tokenHash("hash-" + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(clinicInviteRepository
                    .findByClinicClinicIdOrderByCreationDateDesc(bichoFeliz.getClinicId()))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("busca por token e email")
    class BuscaPorTokenEEmail {

        @Test
        @DisplayName("link de compartilhamento deve ser encontrado pelo hash do token")
        void linkEncontradoPeloHash() {
            var hash = "hash-" + UUID.randomUUID();
            animalShareRepository.save(AnimalShare.builder()
                    .animal(rex).tokenHash(hash)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(animalShareRepository.findByTokenHash(hash)).isPresent();
            assertThat(animalShareRepository.findByTokenHash("outro-hash")).isEmpty();
        }

        @Test
        @DisplayName("links devem ser filtrados pelo animal")
        void linksFiltradosPeloAnimal() {
            animalShareRepository.save(AnimalShare.builder()
                    .animal(rex).tokenHash("hash-" + UUID.randomUUID())
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .creationDate(LocalDateTime.now()).build());

            assertThat(animalShareRepository.findByAnimalOrderByCreationDateDesc(rex)).hasSize(1);
            assertThat(animalShareRepository.findByAnimalOrderByCreationDateDesc(nina)).isEmpty();
        }

        @Test
        @DisplayName("tutor e veterinario devem ser encontrados por email - a base do login")
        void loginEncontraTutorEVeterinario() {
            assertThat(personRepository.findByEmail(ulysses.getEmail())).isPresent();
            assertThat(personRepository.findByEmail(marina.getEmail())).isPresent();
            assertThat(personRepository.existsByEmail(marina.getEmail())).isTrue();
            assertThat(personRepository.findByEmail("ninguem@petfy.com.br")).isEmpty();
        }
    }

    @Nested
    @DisplayName("agenda e rastro")
    class AgendaERastro {

        @Test
        @DisplayName("a rotina de lembretes deve pegar vencidas e as ate o limite, nao alem")
        void rotinaDeLembretesPegaAsCertas() {
            vacina(rex, "Vencida", LocalDate.now().minusDays(5));
            vacina(rex, "No limite", LocalDate.now().plusDays(30));
            vacina(rex, "Alem", LocalDate.now().plusDays(31));

            assertThat(vaccineRepository.findByNextDoseDateLessThanEqual(LocalDate.now().plusDays(30)))
                    .extracting(Vaccine::getVaccineName)
                    .containsExactlyInAnyOrder("Vencida", "No limite");
        }

        @Test
        @DisplayName("o rastro deve ser filtrado pela vacina, da correcao mais recente para a mais antiga")
        void rastroFiltradoEOrdenado() {
            var vacina = vacina(rex, "V10", LocalDate.now().plusDays(10));
            var outra = vacina(rex, "V8", LocalDate.now().plusDays(10));

            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(vacina).correctedBy(ulysses).previousVaccineName("Antiga")
                    .correctedAt(LocalDateTime.now().minusDays(2)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(vacina).correctedBy(marina).correctedInClinic(marina.getClinic()).previousVaccineName("Recente")
                    .correctedAt(LocalDateTime.now().minusHours(1)).build());
            vaccineCorrectionRepository.save(VaccineCorrection.builder()
                    .vaccine(outra).correctedBy(ulysses).previousVaccineName("De outra vacina")
                    .correctedAt(LocalDateTime.now()).build());

            assertThat(vaccineCorrectionRepository
                    .findByVaccineVaccineIdOrderByCorrectedAtDesc(vacina.getVaccineId()))
                    .extracting(VaccineCorrection::getPreviousVaccineName)
                    .containsExactly("Recente", "Antiga");
        }
    }
}

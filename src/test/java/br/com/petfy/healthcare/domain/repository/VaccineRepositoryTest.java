package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diferente das demais queries do projeto, esta filtra por data e nao por UUID -
 * entao escapa da limitacao do H2 com BINARY(255) descrita no
 * HealthRecordRepositoryTest, e aqui da para conferir as linhas que voltam mesmo
 * sem Docker.
 *
 * Vale a pena porque e a query que alimenta a rotina de lembretes: se ela
 * trouxer de menos, o tutor nao e avisado; se trouxer de mais, vira spam. O
 * UuidQueriesContainerTest repete o caso contra Postgres.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VaccineRepositoryTest {

    @Autowired
    private VaccineRepository vaccineRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private PersonRepository personRepository;

    private static final LocalDate HOJE = LocalDate.now();

    private Animal rex;

    @BeforeEach
    void setUp() {
        Person person = personRepository.save(Person.builder()
                .name("Ulysses").email("ulysses@petfy.com.br").password("hash").build());

        rex = animalRepository.save(Animal.builder().name("Rex").tutors(br.com.petfy.healthcare.PetTutores.titular(person)).species(Species.CANINA).build());
    }

    private void gravarVacina(String nome, LocalDate proximaDose) {
        vaccineRepository.save(Vaccine.builder()
                .animal(rex).vaccineName(nome).nextDoseDate(proximaDose).build());
    }

    @Test
    @DisplayName("deve trazer as doses vencidas e as que caem ate o limite")
    void deveTrazerVencidasEAteOLimite() {
        gravarVacina("Vencida", HOJE.minusDays(10));
        gravarVacina("Hoje", HOJE);
        gravarVacina("No limite", HOJE.plusDays(30));

        var result = vaccineRepository.findByNextDoseDateLessThanEqual(HOJE.plusDays(30));

        assertThat(result).extracting(Vaccine::getVaccineName)
                .containsExactlyInAnyOrder("Vencida", "Hoje", "No limite");
    }

    @Test
    @DisplayName("nao deve trazer dose alem do limite - senao o lembrete sai cedo demais")
    void naoDeveTrazerAlemDoLimite() {
        gravarVacina("Um dia depois", HOJE.plusDays(31));
        gravarVacina("Daqui a um ano", HOJE.plusYears(1));

        assertThat(vaccineRepository.findByNextDoseDateLessThanEqual(HOJE.plusDays(30))).isEmpty();
    }

    @Test
    @DisplayName("nao deve trazer dose sem data de proxima aplicacao")
    void naoDeveTrazerDoseSemProximaData() {
        gravarVacina("Dose unica", null);

        assertThat(vaccineRepository.findByNextDoseDateLessThanEqual(HOJE.plusDays(30))).isEmpty();
    }

    @Test
    @DisplayName("deve devolver o animal e o dono junto, que e do que o lembrete precisa")
    void deveDevolverAnimalEDonoJunto() {
        gravarVacina("V10", HOJE.minusDays(1));

        var result = vaccineRepository.findByNextDoseDateLessThanEqual(HOJE);

        assertThat(result).singleElement().satisfies(vaccine -> {
            assertThat(vaccine.getAnimal().getName()).isEqualTo("Rex");
            assertThat(vaccine.getAnimal().getHolder().orElseThrow().getEmail()).isEqualTo("ulysses@petfy.com.br");
        });
    }
}

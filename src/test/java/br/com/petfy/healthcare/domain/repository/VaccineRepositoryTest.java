package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
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
 * HealthRecordRepositoryTest, e aqui da para conferir as linhas que voltam, e
 * nao apenas que a query e traduzida.
 *
 * Vale a pena porque e a query que alimenta a rotina de lembretes: se ela
 * trouxer de menos, o tutor nao e avisado; se trouxer de mais, vira spam.
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
    private PetRepository petRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    private static final LocalDate HOJE = LocalDate.now();

    private Pet rex;

    @BeforeEach
    void setUp() {
        Owner owner = ownerRepository.save(Owner.builder()
                .name("Ulysses").email("ulysses@petfy.com.br").password("hash").build());

        rex = petRepository.save(Pet.builder().name("Rex").owner(owner).build());
    }

    private void gravarVacina(String nome, LocalDate proximaDose) {
        vaccineRepository.save(Vaccine.builder()
                .pet(rex).vaccineName(nome).nextDoseDate(proximaDose).build());
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
    @DisplayName("deve devolver o pet e o dono junto, que e do que o lembrete precisa")
    void deveDevolverPetEDonoJunto() {
        gravarVacina("V10", HOJE.minusDays(1));

        var result = vaccineRepository.findByNextDoseDateLessThanEqual(HOJE);

        assertThat(result).singleElement().satisfies(vaccine -> {
            assertThat(vaccine.getPet().getName()).isEqualTo("Rex");
            assertThat(vaccine.getPet().getOwner().getEmail()).isEqualTo("ulysses@petfy.com.br");
        });
    }
}

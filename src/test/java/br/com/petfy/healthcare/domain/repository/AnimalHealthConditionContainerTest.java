package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalHealthCondition;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import br.com.petfy.healthcare.domain.entity.PetTutor;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O prontuario estruturado contra Postgres de verdade.
 *
 * Tres coisas que so o banco decide: o CHECK que impede gravidade em condicao cronica, a
 * obrigatoriedade da categoria no atendimento, e a ordenacao com {@code NULLS FIRST} - que
 * nao existe na gramatica de nome derivado do Spring Data e por isso virou JPQL explicito.
 */
@SpringBootTest
@DisplayName("prontuario estruturado contra Postgres real")
class AnimalHealthConditionContainerTest extends PostgresContainerTest {

    @Autowired private AnimalHealthConditionRepository animalHealthConditionRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Animal rex;

    @BeforeEach
    void setUp() {
        Owner ulysses = ownerRepository.saveAndFlush(Owner.builder()
                .name("Ulysses")
                .email("prontuario-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        petTutorRepository.saveAndFlush(PetTutor.builder()
                .animal(rex).owner(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());
    }

    private AnimalHealthCondition.AnimalHealthConditionBuilder condicao() {
        return AnimalHealthCondition.builder()
                .animal(rex)
                .description("Anestesico local")
                .creationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("o que o banco recusa")
    class OQueOBancoRecusa {

        /**
         * A regra existe nos dois lugares e nao e redundancia: o servico responde 400 em vez
         * de 500, e o banco impede que um insert direto contorne. Este teste cobre o
         * segundo.
         */
        @Test
        @Transactional
        @DisplayName("gravidade em condicao cronica e recusada pelo CHECK")
        void gravidadeEmCronicaERecusada() {
            var invalida = condicao()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .severity(AnimalHealthConditionSeverity.GRAVE)
                    .build();

            assertThatThrownBy(() -> animalHealthConditionRepository.saveAndFlush(invalida))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @Transactional
        @DisplayName("gravidade em alergia e aceita")
        void gravidadeEmAlergiaEAceita() {
            var salva = animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .severity(AnimalHealthConditionSeverity.GRAVE)
                    .build());

            assertThat(salva.getAnimalHealthConditionId()).isNotNull();
        }

        @Test
        @Transactional
        @DisplayName("condicao cronica sem gravidade e aceita")
        void cronicaSemGravidadeEAceita() {
            var salva = animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Cardiopatia")
                    .build());

            assertThat(salva.getSeverity()).isNull();
        }

        /**
         * A categoria e NOT NULL depois da V19: atendimento sem ela voltaria a ser a lista
         * de texto que a migration existe para superar.
         */
        @Test
        @Transactional
        @DisplayName("atendimento sem categoria e recusado")
        void atendimentoSemCategoriaERecusado() {
            assertThatThrownBy(() -> healthRecordRepository.saveAndFlush(
                    br.com.petfy.healthcare.domain.entity.HealthRecord.builder()
                            .animal(rex)
                            .eventType("Consulta")
                            .eventDate(LocalDate.now())
                            .creationDate(LocalDateTime.now())
                            .build()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        /** O DEFAULT da V19 sai depois do backfill: nao pode virar valor silencioso. */
        @Test
        @DisplayName("a coluna category nao tem default no schema")
        void categoryNaoTemDefault() {
            String defaultDaColuna = jdbcTemplate.query(
                    "select column_default from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'health_records' "
                            + "and column_name = 'category'",
                    rs -> rs.next() ? rs.getString(1) : "COLUNA_AUSENTE");

            assertThat(defaultDaColuna).isNull();
        }
    }

    /**
     * A ordenacao e o motivo de a consulta ser JPQL e nao nome derivado - e por isso precisa
     * de teste contra banco: {@code NULLS FIRST} nao e comportamento do Spring Data, e do
     * SQL.
     */
    @Nested
    @DisplayName("ordenacao")
    class Ordenacao {

        @Test
        @Transactional
        @DisplayName("ativas vem antes das encerradas")
        void ativasVemAntes() {
            animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.CONDICAO_CRONICA)
                    .description("Giardiase, resolvida")
                    .resolvedAt(LocalDate.now().minusMonths(6))
                    .creationDate(LocalDateTime.now().minusMonths(12))
                    .build());

            animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Dipirona")
                    .severity(AnimalHealthConditionSeverity.MODERADA)
                    .creationDate(LocalDateTime.now().minusDays(1))
                    .build());

            List<AnimalHealthCondition> ordenadas =
                    animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(rex.getAnimalId());

            assertThat(ordenadas).hasSize(2);
            assertThat(ordenadas.get(0).getDescription()).isEqualTo("Dipirona");
            assertThat(ordenadas.get(0).isAtiva()).isTrue();
            assertThat(ordenadas.get(1).isAtiva()).isFalse();
        }

        @Test
        @Transactional
        @DisplayName("entre ativas, a mais recente vem primeiro")
        void entreAtivasAMaisRecente() {
            animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Antiga")
                    .creationDate(LocalDateTime.now().minusYears(1))
                    .build());

            animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.ALERGIA)
                    .description("Recente")
                    .creationDate(LocalDateTime.now())
                    .build());

            assertThat(animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(rex.getAnimalId()))
                    .extracting(AnimalHealthCondition::getDescription)
                    .containsExactly("Recente", "Antiga");
        }

        @Test
        @Transactional
        @DisplayName("condicao de outro animal nao aparece")
        void condicaoDeOutroAnimalNaoAparece() {
            Animal nina = animalRepository.saveAndFlush(Animal.builder()
                    .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

            animalHealthConditionRepository.saveAndFlush(AnimalHealthCondition.builder()
                    .animal(nina).kind(AnimalHealthConditionKind.ALERGIA).description("Da Nina")
                    .creationDate(LocalDateTime.now()).build());

            animalHealthConditionRepository.saveAndFlush(condicao()
                    .kind(AnimalHealthConditionKind.ALERGIA).description("Do Rex").build());

            assertThat(animalHealthConditionRepository.findByAnimalOrdenadasPorRelevancia(rex.getAnimalId()))
                    .extracting(AnimalHealthCondition::getDescription)
                    .containsExactly("Do Rex");
        }
    }

    @Nested
    @DisplayName("os campos clinicos do animal")
    class CamposClinicosDoAnimal {

        @Test
        @Transactional
        @DisplayName("numero do microchip, castracao e data sao gravados")
        void camposSaoGravados() {
            rex.setMicrochipNumber("982000123456789");
            rex.setMicrochip(true);
            rex.setCastrated(true);
            rex.setCastratedAt(LocalDate.now().minusYears(2));

            animalRepository.saveAndFlush(rex);

            var recarregado = animalRepository.findById(rex.getAnimalId()).orElseThrow();

            assertThat(recarregado.getMicrochipNumber()).isEqualTo("982000123456789");
            assertThat(recarregado.getCastrated()).isTrue();
            assertThat(recarregado.getCastratedAt()).isNotNull();
        }

        /** O indice parcial existe para a busca por chip de animal perdido. */
        @Test
        @DisplayName("o indice de microchip existe e e parcial")
        void indiceDeMicrochipEParcial() {
            String definicao = jdbcTemplate.query(
                    "select indexdef from pg_indexes where schemaname = 'public' "
                            + "and indexname = 'idx_animals_microchip_number'",
                    rs -> rs.next() ? rs.getString(1) : null);

            assertThat(definicao)
                    .as("indice de microchip ausente: a busca por chip de animal perdido "
                            + "varreria a tabela inteira")
                    .isNotNull()
                    .contains("WHERE");
        }
    }

}

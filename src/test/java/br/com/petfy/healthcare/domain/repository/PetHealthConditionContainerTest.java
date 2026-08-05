package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetHealthCondition;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionSeverity;
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
class PetHealthConditionContainerTest extends PostgresContainerTest {

    @Autowired private PetHealthConditionRepository petHealthConditionRepository;
    @Autowired private HealthRecordRepository healthRecordRepository;
    @Autowired private OwnerRepository ownerRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private PetTutorRepository petTutorRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Pet rex;

    @BeforeEach
    void setUp() {
        Owner ulysses = ownerRepository.saveAndFlush(Owner.builder()
                .name("Ulysses")
                .email("prontuario-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = petRepository.saveAndFlush(Pet.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        petTutorRepository.saveAndFlush(PetTutor.builder()
                .pet(rex).owner(ulysses).role(PetTutorRole.HOLDER)
                .creationDate(LocalDateTime.now()).build());
    }

    private PetHealthCondition.PetHealthConditionBuilder condicao() {
        return PetHealthCondition.builder()
                .pet(rex)
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
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
                    .description("Diabetes")
                    .severity(PetHealthConditionSeverity.GRAVE)
                    .build();

            assertThatThrownBy(() -> petHealthConditionRepository.saveAndFlush(invalida))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @Transactional
        @DisplayName("gravidade em alergia e aceita")
        void gravidadeEmAlergiaEAceita() {
            var salva = petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.ALERGIA)
                    .severity(PetHealthConditionSeverity.GRAVE)
                    .build());

            assertThat(salva.getPetHealthConditionId()).isNotNull();
        }

        @Test
        @Transactional
        @DisplayName("condicao cronica sem gravidade e aceita")
        void cronicaSemGravidadeEAceita() {
            var salva = petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
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
                            .pet(rex)
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
            petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.CONDICAO_CRONICA)
                    .description("Giardiase, resolvida")
                    .resolvedAt(LocalDate.now().minusMonths(6))
                    .creationDate(LocalDateTime.now().minusMonths(12))
                    .build());

            petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.ALERGIA)
                    .description("Dipirona")
                    .severity(PetHealthConditionSeverity.MODERADA)
                    .creationDate(LocalDateTime.now().minusDays(1))
                    .build());

            List<PetHealthCondition> ordenadas =
                    petHealthConditionRepository.findByPetOrdenadasPorRelevancia(rex.getPetId());

            assertThat(ordenadas).hasSize(2);
            assertThat(ordenadas.get(0).getDescription()).isEqualTo("Dipirona");
            assertThat(ordenadas.get(0).isAtiva()).isTrue();
            assertThat(ordenadas.get(1).isAtiva()).isFalse();
        }

        @Test
        @Transactional
        @DisplayName("entre ativas, a mais recente vem primeiro")
        void entreAtivasAMaisRecente() {
            petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.ALERGIA)
                    .description("Antiga")
                    .creationDate(LocalDateTime.now().minusYears(1))
                    .build());

            petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.ALERGIA)
                    .description("Recente")
                    .creationDate(LocalDateTime.now())
                    .build());

            assertThat(petHealthConditionRepository.findByPetOrdenadasPorRelevancia(rex.getPetId()))
                    .extracting(PetHealthCondition::getDescription)
                    .containsExactly("Recente", "Antiga");
        }

        @Test
        @Transactional
        @DisplayName("condicao de outro pet nao aparece")
        void condicaoDeOutroPetNaoAparece() {
            Pet nina = petRepository.saveAndFlush(Pet.builder()
                    .name("Nina").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

            petHealthConditionRepository.saveAndFlush(PetHealthCondition.builder()
                    .pet(nina).kind(PetHealthConditionKind.ALERGIA).description("Da Nina")
                    .creationDate(LocalDateTime.now()).build());

            petHealthConditionRepository.saveAndFlush(condicao()
                    .kind(PetHealthConditionKind.ALERGIA).description("Do Rex").build());

            assertThat(petHealthConditionRepository.findByPetOrdenadasPorRelevancia(rex.getPetId()))
                    .extracting(PetHealthCondition::getDescription)
                    .containsExactly("Do Rex");
        }
    }

    @Nested
    @DisplayName("os campos clinicos do pet")
    class CamposClinicosDoPet {

        @Test
        @Transactional
        @DisplayName("numero do microchip, castracao e data sao gravados")
        void camposSaoGravados() {
            rex.setMicrochipNumber("982000123456789");
            rex.setMicrochip(true);
            rex.setCastrated(true);
            rex.setCastratedAt(LocalDate.now().minusYears(2));

            petRepository.saveAndFlush(rex);

            var recarregado = petRepository.findById(rex.getPetId()).orElseThrow();

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
                            + "and indexname = 'idx_pets_microchip_number'",
                    rs -> rs.next() ? rs.getString(1) : null);

            assertThat(definicao)
                    .as("indice de microchip ausente: a busca por chip de animal perdido "
                            + "varreria a tabela inteira")
                    .isNotNull()
                    .contains("WHERE");
        }
    }

}

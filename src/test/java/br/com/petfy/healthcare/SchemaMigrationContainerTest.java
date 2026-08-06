package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O teste que valida as migrations.
 *
 * Subir este contexto ja e a verificacao principal: o Flyway aplica todas as
 * migrations num Postgres limpo e o Hibernate roda ddl-auto=validate contra o
 * resultado. Se algum tipo, nome de coluna ou tabela divergir do mapeamento, o
 * contexto nao sobe - que e exatamente a falha que aconteceria na primeira
 * subida em producao.
 *
 * Ate aqui os tipos das migrations eram conferidos comparando com o DDL que o
 * Hibernate gera. Bom, mas indireto: comparava o que o Hibernate PRODUZ com o
 * que a migration diz, sem nunca executar as duas coisas juntas.
 */
@SpringBootTest
class SchemaMigrationContainerTest extends PostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VaccineCatalogRepository vaccineCatalogRepository;

    /**
     * O numero esperado vem de contar os arquivos, e nao de uma constante: com
     * numero fixo o teste quebraria a cada migration nova, o que treina quem
     * mantem o projeto a atualizar o valor sem olhar - e um dia esconderia uma
     * migration que realmente falhou.
     */
    @Test
    @DisplayName("todas as migrations do projeto devem ter sido aplicadas com sucesso")
    void todasAsMigrationsDevemTerSidoAplicadas() throws Exception {
        int arquivos = new PathMatchingResourcePatternResolver()
                .getResources("classpath:db/migration/V*.sql").length;

        Integer aplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(arquivos).isPositive();
        assertThat(aplicadas).isEqualTo(arquivos);
    }

    @Test
    @DisplayName("nenhuma migration pode constar como falha")
    void nenhumaMigrationPodeTerFalhado() {
        Integer falhas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);

        assertThat(falhas).isZero();
    }

    @Test
    @DisplayName("todas as tabelas do dominio devem existir")
    void todasAsTabelasDevemExistir() {
        List<String> tabelas = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tabelas).contains(
                "owners", "clinics", "animals", "vaccines", "health_records",
                "vaccine_catalog", "animal_shares", "vets", "pet_clinic_access",
                "clinic_invites", "vaccine_corrections",
                "pet_tutors", "pet_tutor_invites", "consent_records", "sensitive_access_log",
                "attachments", "animal_health_conditions");
    }

    @Test
    @DisplayName("o UUID deve ser tipo nativo no Postgres, e nao binario como no H2")
    void uuidDeveSerTipoNativo() {
        List<String> tipos = jdbcTemplate.queryForList(
                "select data_type from information_schema.columns "
                        + "where table_schema = 'public' and column_name = 'animal_id'",
                String.class);

        assertThat(tipos).isNotEmpty().containsOnly("uuid");
    }

    @Test
    @DisplayName("o catalogo de vacinas deve vir semeado pela migration")
    void catalogoDeveVirSemeado() {
        var catalogo = vaccineCatalogRepository.findAllByOrderBySpeciesAscNameAsc();

        assertThat(catalogo).hasSize(11);
        assertThat(catalogo).extracting("code").contains("V10", "ANTIRRABICA_C", "V3_FELINA");
        assertThat(catalogo).allSatisfy(entrada ->
                assertThat(entrada.getSpecies())
                        .isIn(br.com.petfy.healthcare.domain.entity.Species.CANINA,
                              br.com.petfy.healthcare.domain.entity.Species.FELINA));
    }

    @Test
    @DisplayName("as chaves estrangeiras devem estar criadas")
    void chavesEstrangeirasDevemExistir() {
        List<String> constraints = jdbcTemplate.queryForList(
                "select constraint_name from information_schema.table_constraints "
                        + "where constraint_type = 'FOREIGN KEY' and table_schema = 'public'",
                String.class);

        assertThat(constraints).contains(
                "fk_vaccines_animal", "fk_health_records_animal",
                "fk_vets_clinic", "fk_pet_clinic_access_pet", "fk_clinic_invites_clinic",
                "fk_vaccine_corrections_vaccine",
                "fk_pet_tutors_pet", "fk_pet_tutors_owner", "fk_pet_tutor_invites_pet",
                "fk_consent_records_owner", "fk_sensitive_access_log_animal");
    }

    /**
     * O que a V15 promete, conferido contra o banco e nao contra o mapeamento.
     *
     * A parte perigosa dessa migration nao e criar tabela: e trocar a fonte de
     * verdade sobre quem manda no animal. Se {@code animals.owner_id} sobrevivesse, ou
     * se o banco aceitasse dois titulares, a regra de acesso passaria a ter duas
     * respostas possiveis para a mesma pergunta.
     */
    @Nested
    @DisplayName("V15 - multi-tutor")
    class MultiTutor {

        /**
         * A coluna antiga tem de ter saido. Mantida ao lado de pet_tutors, "quem e
         * o dono" teria duas respostas, e o dia em que divergissem seria alguem
         * enxergando animal que nao e seu.
         */
        @Test
        @DisplayName("animals.owner_id nao pode mais existir")
        void ownerIdDeveTerSaidoDeAnimals() {
            List<String> colunas = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'animals'",
                    String.class);

            assertThat(colunas).isNotEmpty().doesNotContain("owner_id");
        }

        /**
         * A coluna continua chamando {@code pet_id}, e nao {@code animal_id}, depois
         * da V20. Nao e esquecimento: {@code pet_tutors} se dissolve em custodia e
         * acesso no P2 da Fase 6, e renomear coluna de tabela marcada para morrer e
         * trabalho que se joga fora. O descasamento com {@code animals.animal_id}
         * marca justamente o que ainda e o mundo velho.
         */
        @Test
        @DisplayName("o vinculo de tutor deve existir com papel obrigatorio")
        void petTutorsDeveTerPapelObrigatorio() {
            List<String> obrigatorias = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'pet_tutors' "
                            + "and is_nullable = 'NO'",
                    String.class);

            assertThat(obrigatorias).contains("pet_tutor_id", "pet_id", "owner_id", "role", "creation_date");
        }

        /**
         * O convite nasce com o vinculo do titular ausente, entao invited_by e o
         * unico campo de relacao que pode ser nulo: os vinculos que a migration
         * criou no backfill nao tem convite atras deles.
         */
        @Test
        @DisplayName("invited_by_owner_id deve aceitar nulo, para os vinculos do backfill")
        void invitedByDeveAceitarNulo() {
            String nullable = jdbcTemplate.queryForObject(
                    "select is_nullable from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'pet_tutors' "
                            + "and column_name = 'invited_by_owner_id'",
                    String.class);

            assertThat(nullable).isEqualTo("YES");
        }

        @Test
        @DisplayName("o indice parcial de um titular por animal deve existir")
        void indiceDeUmTitularPorAnimalDeveExistir() {
            List<String> indices = jdbcTemplate.queryForList(
                    "select indexname from pg_indexes "
                            + "where schemaname = 'public' and tablename = 'pet_tutors'",
                    String.class);

            assertThat(indices).contains("uk_pet_tutors_um_holder_por_pet",
                    "idx_pet_tutors_pet", "idx_pet_tutors_owner");
        }

        /**
         * O indice parcial em acao. Um animal sem titular ficaria sem ninguem que
         * pudesse convidar ou apagar; com dois, os dois se removeriam mutuamente.
         * Por isso a garantia e do banco, e nao so do servico.
         */
        @Test
        @DisplayName("o banco recusa um segundo titular no mesmo animal")
        void bancoRecusaSegundoTitular() {
            UUID donoA = inserirOwner("titular-a");
            UUID donoB = inserirOwner("titular-b");
            UUID animalId = inserirAnimal();

            inserirTutor(animalId, donoA, "HOLDER");

            assertThatThrownBy(() -> inserirTutor(animalId, donoB, "HOLDER"))
                    .isInstanceOf(DuplicateKeyException.class);

            // o mesmo animal aceita quantos co-tutores quiser: a restricao e so sobre
            // HOLDER, e e por isso que ela e um indice parcial
            inserirTutor(animalId, donoB, "EDITOR");

            assertThat(contarTutores(animalId)).isEqualTo(2);

            limpar(animalId);
        }

        @Test
        @DisplayName("o banco recusa a mesma pessoa duas vezes no mesmo animal")
        void bancoRecusaTutorDuplicado() {
            UUID dono = inserirOwner("tutor-reanimalido");
            UUID animalId = inserirAnimal();

            inserirTutor(animalId, dono, "HOLDER");

            assertThatThrownBy(() -> inserirTutor(animalId, dono, "VIEWER"))
                    .isInstanceOf(DuplicateKeyException.class);

            limpar(animalId);
        }

        private UUID inserirOwner(String prefixo) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update(
                    "insert into owners (owner_id, name, email, password) values (?, ?, ?, ?)",
                    id, "Teste", prefixo + "-" + id + "@petfy.com.br", "hash");
            return id;
        }

        private UUID inserirAnimal() {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update(
                    "insert into animals (animal_id, name, species, creation_date) values (?, ?, ?, ?)",
                    id, "Rex", "CANINA", Timestamp.valueOf(LocalDateTime.now()));
            return id;
        }

        private void inserirTutor(UUID animalId, UUID ownerId, String role) {
            jdbcTemplate.update(
                    "insert into pet_tutors (pet_tutor_id, pet_id, owner_id, role, creation_date) "
                            + "values (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), animalId, ownerId, role, Timestamp.valueOf(LocalDateTime.now()));
        }

        private Integer contarTutores(UUID animalId) {
            return jdbcTemplate.queryForObject(
                    "select count(*) from pet_tutors where pet_id = ?", Integer.class, animalId);
        }

        /** O container e compartilhado entre as classes: o animal e os vinculos saem daqui. */
        private void limpar(UUID animalId) {
            jdbcTemplate.update("delete from pet_tutors where pet_id = ?", animalId);
            jdbcTemplate.update("delete from animals where animal_id = ?", animalId);
        }
    }
}

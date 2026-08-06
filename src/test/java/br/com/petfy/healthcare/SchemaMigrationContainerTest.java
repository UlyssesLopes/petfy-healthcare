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
                "persons", "organizations", "animals", "vaccines", "health_records",
                "vaccine_catalog", "grants", "grant_scopes", "professional_credentials",
                "organization_invites", "vaccine_corrections",
                "custodies", "pet_tutor_invites", "consent_records", "sensitive_access_log",
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
                "fk_memberships_person", "fk_memberships_organization", "fk_professional_credentials_person", "fk_grants_animal", "fk_organization_invites_organization",
                "fk_vaccine_corrections_vaccine",
                "fk_custodies_animal", "fk_custodies_holder_person", "fk_pet_tutor_invites_pet",
                "fk_consent_records_person", "fk_sensitive_access_log_animal");
    }

    /**
     * O que a V15 promete, conferido contra o banco e nao contra o mapeamento.
     *
     * A parte perigosa dessa migration nao e criar tabela: e trocar a fonte de
     * verdade sobre quem manda no animal. Se {@code animals.person_id} sobrevivesse, ou
     * se o banco aceitasse dois titulares, a regra de acesso passaria a ter duas
     * respostas possiveis para a mesma pergunta.
     */
    /**
     * O que a V24 promete, conferido contra o banco e nao contra o mapeamento.
     *
     * <b>Substituiu o bloco "V15 - multi-tutor".</b> Aquele bloco exercitava
     * {@code pet_tutors}, que a V24 derrubou - e o invariante que ele protegia nao
     * desapareceu, mudou de lugar: onde havia "exatamente um HOLDER por animal", ha
     * "no maximo uma custodia em curso por animal". Apagar os casos teria perdido a
     * garantia; adapta-los ao nome novo e o que os mantem verdadeiros.
     */
    @Nested
    @DisplayName("V24 - custodia")
    class Custodia {

        @Test
        @DisplayName("pet_tutors nao pode mais existir")
        void petTutorsDeveTerSaido() {
            List<String> tabelas = jdbcTemplate.queryForList(
                    "select table_name from information_schema.tables where table_schema = 'public'",
                    String.class);

            assertThat(tabelas).isNotEmpty().doesNotContain("pet_tutors");
        }

        @Test
        @DisplayName("a custodia deve existir com natureza e inicio obrigatorios")
        void custodiaDeveTerNaturezaEInicio() {
            List<String> obrigatorias = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'custodies' "
                            + "and is_nullable = 'NO'",
                    String.class);

            assertThat(obrigatorias).contains("custody_id", "animal_id", "nature", "started_at");
        }

        /**
         * Encerramento e sucessor sao opcionais na coluna porque custodia em curso nao
         * os tem. As regras sobre eles vivem em CHECK e no servico.
         */
        @Test
        @DisplayName("fim, motivo e sucessor devem aceitar nulo")
        void fimEsucessorDevemAceitarNulo() {
            List<String> opcionais = jdbcTemplate.queryForList(
                    "select column_name from information_schema.columns "
                            + "where table_schema = 'public' and table_name = 'custodies' "
                            + "and is_nullable = 'YES'",
                    String.class);

            assertThat(opcionais).contains("ended_at", "end_reason", "successor_custody_id");
        }

        @Test
        @DisplayName("o indice parcial de uma custodia em curso por animal deve existir")
        void indiceDeUmaEmCursoPorAnimalDeveExistir() {
            List<String> indices = jdbcTemplate.queryForList(
                    "select indexname from pg_indexes "
                            + "where schemaname = 'public' and tablename = 'custodies'",
                    String.class);

            assertThat(indices).contains("uk_custodies_uma_em_curso_por_animal",
                    "idx_custodies_animal", "idx_custodies_holder_person");
        }

        /**
         * O indice parcial em acao, e e o coracao deste bloco.
         *
         * Um animal com duas custodias em curso teria dois responsaveis, e a pergunta
         * "quem responde por este animal" passaria a ter duas respostas - o dia em que
         * divergissem seria alguem decidindo sobre um animal que nao e seu. Por isso a
         * garantia e do banco, e nao so do servico: mock nao tem indice.
         */
        @Test
        @DisplayName("o banco recusa uma segunda custodia em curso no mesmo animal")
        void bancoRecusaSegundaCustodiaEmCurso() {
            UUID pessoaA = inserirPerson("responsavel-a");
            UUID pessoaB = inserirPerson("responsavel-b");
            UUID animalId = inserirAnimal();

            inserirCustodia(animalId, pessoaA, null);

            assertThatThrownBy(() -> inserirCustodia(animalId, pessoaB, null))
                    .isInstanceOf(DuplicateKeyException.class);

            // encerrada nao conta: o historico tem quantas custodias quiser, e e ele que
            // conta a vida do animal. E por isso que o indice e parcial
            inserirCustodia(animalId, pessoaB, "OBITO");

            assertThat(contarCustodias(animalId)).isEqualTo(2);

            limpar(animalId);
        }

        /** Encerrar sem dizer por que deixaria a linha do tempo do animal sem o fato. */
        @Test
        @DisplayName("o banco recusa custodia encerrada sem motivo")
        void bancoRecusaFimSemMotivo() {
            UUID pessoa = inserirPerson("sem-motivo");
            UUID animalId = inserirAnimal();

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into custodies (custody_id, animal_id, holder_person_id, nature, "
                            + "started_at, ended_at) values (?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), animalId, pessoa, "DEFINITIVA",
                    Timestamp.valueOf(LocalDateTime.now()), Timestamp.valueOf(LocalDateTime.now())))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            limpar(animalId);
        }

        /**
         * Exatamente um responsavel por linha: nem os dois, nem nenhum. Sem este CHECK
         * seria possivel gravar custodia sem ninguem respondendo pelo animal - o oposto
         * do que ela existe para dizer.
         */
        @Test
        @DisplayName("o banco recusa custodia sem responsavel")
        void bancoRecusaCustodiaSemResponsavel() {
            UUID animalId = inserirAnimal();

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into custodies (custody_id, animal_id, nature, started_at) "
                            + "values (?, ?, ?, ?)",
                    UUID.randomUUID(), animalId, "DEFINITIVA", Timestamp.valueOf(LocalDateTime.now())))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

            limpar(animalId);
        }

        private UUID inserirPerson(String prefixo) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update(
                    "insert into persons (person_id, name, email, password) values (?, ?, ?, ?)",
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

        private void inserirCustodia(UUID animalId, UUID personId, String motivoDeFim) {
            jdbcTemplate.update(
                    "insert into custodies (custody_id, animal_id, holder_person_id, nature, "
                            + "started_at, ended_at, end_reason) values (?, ?, ?, ?, ?, ?, ?)",
                    UUID.randomUUID(), animalId, personId, "DEFINITIVA",
                    Timestamp.valueOf(LocalDateTime.now()),
                    motivoDeFim == null ? null : Timestamp.valueOf(LocalDateTime.now()),
                    motivoDeFim);
        }

        private Integer contarCustodias(UUID animalId) {
            return jdbcTemplate.queryForObject(
                    "select count(*) from custodies where animal_id = ?", Integer.class, animalId);
        }

        /** O container e compartilhado entre as classes: o animal e as custodias saem daqui. */
        private void limpar(UUID animalId) {
            jdbcTemplate.update("delete from custodies where animal_id = ?", animalId);
            jdbcTemplate.update("delete from animals where animal_id = ?", animalId);
        }
    }

}

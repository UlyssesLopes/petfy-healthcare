package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste que valida as migrations.
 *
 * Subir este contexto ja e a verificacao principal: o Flyway aplica as oito
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

    @Test
    @DisplayName("as migrations devem produzir um schema que o mapeamento aceita")
    void migrationsDevemProduzirSchemaValido() {
        // chegar aqui ja significa Flyway aplicado e validate aprovado
        Integer aplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);

        assertThat(aplicadas).isEqualTo(8);
    }

    @Test
    @DisplayName("todas as tabelas do dominio devem existir")
    void todasAsTabelasDevemExistir() {
        List<String> tabelas = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tabelas).contains(
                "owners", "clinics", "pets", "vaccines", "health_records",
                "vaccine_catalog", "pet_shares", "vets", "pet_clinic_access",
                "clinic_invites", "vaccine_corrections");
    }

    @Test
    @DisplayName("o UUID deve ser tipo nativo no Postgres, e nao binario como no H2")
    void uuidDeveSerTipoNativo() {
        List<String> tipos = jdbcTemplate.queryForList(
                "select data_type from information_schema.columns "
                        + "where table_schema = 'public' and column_name = 'pet_id'",
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
                assertThat(entrada.getSpecies()).isIn("CANINA", "FELINA"));
    }

    @Test
    @DisplayName("as chaves estrangeiras devem estar criadas")
    void chavesEstrangeirasDevemExistir() {
        List<String> constraints = jdbcTemplate.queryForList(
                "select constraint_name from information_schema.table_constraints "
                        + "where constraint_type = 'FOREIGN KEY' and table_schema = 'public'",
                String.class);

        assertThat(constraints).contains(
                "fk_pets_owner", "fk_vaccines_pet", "fk_health_records_pet",
                "fk_vets_clinic", "fk_pet_clinic_access_pet", "fk_clinic_invites_clinic",
                "fk_vaccine_corrections_vaccine");
    }
}

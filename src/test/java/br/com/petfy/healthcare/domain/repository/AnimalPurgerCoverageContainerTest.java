package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste que impede o bug de voltar.
 *
 * Apagar animal e apagar conta ja falharam tres vezes pelo mesmo motivo: uma tabela
 * nova passou a apontar para {@code animals} e a limpeza nao foi atualizada. O
 * {@code AnimalPurger} resolveu a divergencia entre as duas listas, mas sozinho ele
 * nao impede que a lista fique <b>desatualizada</b> - so que ela fique
 * inconsistente entre si.
 *
 * Entao a lista deixa de ser mantida de memoria: este teste pergunta ao
 * <b>proprio banco</b> quem referencia {@code animals} e exige que cada tabela esteja
 * declarada como coberta. Uma tabela nova apontando para {@code animals} quebra aqui
 * ate entrar no purger - que e a protecao que o ON DELETE CASCADE daria, sem
 * perder a exclusao visivel em codigo nem a regra condicional de que um animal com
 * outro tutor sobrevive.
 *
 * <b>Por que nao dava para fazer isso com mock:</b> a lista de quem aponta para
 * {@code animals} mora no schema, e o schema so existe quando o Flyway roda.
 */
@SpringBootTest
@DisplayName("cobertura do AnimalPurger contra o schema real")
class AnimalPurgerCoverageContainerTest extends PostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * O que o {@link br.com.petfy.healthcare.service.AnimalPurger} apaga hoje.
     *
     * Manter isto a mao e deliberado: a lista tem de ser escrita por alguem que
     * decidiu o que fazer com a tabela nova. Deriva-la do purger por reflexao
     * transformaria o teste em tautologia - ele passaria a conferir que o purger
     * faz o que o purger faz.
     */
    private static final Set<String> COBERTAS_PELO_PURGER = Set.of(
            "attachments",
            "vaccines",
            "vaccine_corrections",
            "health_records",
            "health_record_corrections",
            "animal_weight_history",
            "antiparasitics",
            // as duas anteriores - animal_shares e pet_organization_access - se dissolveram
            // em grants no P2a. grant_scopes entra porque alcanca animals pela neta:
            // aponta para grants, que aponta para animals
            "grants",
            "grant_scopes",
            "custodies",
            "pet_tutor_invites",
            "sensitive_access_log",
            "animal_health_conditions",
            // care_instruction_fulfillments alcanca animals pela neta, como as correcoes:
            // aponta para care_instructions, que aponta para animals
            "care_instructions",
            "care_instruction_fulfillments",
            // filha direta, e sem neta: nada aponta para observations, entao o purger
            // apaga em massa em vez de por entidade
            "observations");

    /**
     * Quem chega a {@code animals}, direta ou indiretamente.
     *
     * A busca e <b>transitiva</b> de proposito. Olhar so as FKs diretas deixaria de
     * fora as netas - {@code vaccine_corrections} aponta para {@code vaccines}, e
     * nao para {@code animals} - e sao justamente elas que tem de sair primeiro na
     * ordem de delete. Uma correcao de antiparasitario adicionada amanha entraria
     * aqui pelo mesmo caminho, sem ninguem lembrar de ajustar a consulta.
     */
    private List<String> tabelasQueAlcancamAnimals() {
        return jdbcTemplate.queryForList(
                "with recursive fk as ("
                        + "  select tc.table_name as filha, ccu.table_name as mae"
                        + "  from information_schema.table_constraints tc"
                        + "  join information_schema.constraint_column_usage ccu"
                        + "    on tc.constraint_name = ccu.constraint_name"
                        + "   and tc.table_schema = ccu.table_schema"
                        + "  where tc.constraint_type = 'FOREIGN KEY'"
                        + "    and tc.table_schema = 'public'"
                        + "), alcanca as ("
                        + "  select filha from fk where mae = 'animals'"
                        // union, e nao union all: dedup e o que termina a recursao
                        // se houver ciclo de FK entre duas tabelas
                        + "  union"
                        + "  select f.filha from fk f join alcanca a on f.mae = a.filha"
                        + ") select distinct filha from alcanca where filha <> 'animals'",
                String.class);
    }

    /**
     * A falha aqui nao e "o teste quebrou": e uma tabela nova pendurada no animal sem
     * ninguem ter decidido o que acontece com ela quando o animal morre. A resposta e
     * entrar no purger e nesta lista - nunca so nesta lista.
     */
    @Test
    @DisplayName("toda tabela que aponta para animals tem de estar coberta pelo purger")
    void todaTabelaQueApontaParaAnimalsEstaCoberta() {
        List<String> alcancamAnimals = tabelasQueAlcancamAnimals();

        assertThat(alcancamAnimals)
                .as("o schema tem de ter FKs para animals; consulta vazia significa que "
                        + "a query parou de funcionar, e nao que o animal ficou sem filhos")
                .isNotEmpty();

        assertThat(alcancamAnimals)
                .as("tabela nova apontando para animals sem entrar no AnimalPurger: apagar o "
                        + "animal e apagar a conta vao responder 500 quando ela tiver linha")
                .allSatisfy(tabela -> assertThat(COBERTAS_PELO_PURGER).contains(tabela));
    }

    /**
     * O contrario tambem importa: uma tabela que saiu do schema e ficou na lista
     * deixa o purger emitindo delete para tabela que nao existe, e a proxima pessoa
     * a ler a lista acredita nela.
     */
    @Test
    @DisplayName("a lista do purger nao pode ter tabela que nao aponta mais para animals")
    void listaNaoTemTabelaObsoleta() {
        List<String> alcancamAnimals = tabelasQueAlcancamAnimals();

        assertThat(COBERTAS_PELO_PURGER)
                .as("tabela na lista do purger que nao aponta mais para animals")
                .allSatisfy(coberta -> assertThat(alcancamAnimals).contains(coberta));
    }

    /**
     * A escolha do projeto, afirmada e nao suposta: a limpeza mora em codigo. Se
     * alguem adicionar {@code ON DELETE CASCADE} numa migration, passam a existir
     * duas fontes de verdade sobre a mesma regra - e a que estiver errada nunca
     * aparece, porque a outra encobre.
     */
    @Test
    @DisplayName("nenhuma FK para animals pode ter ON DELETE CASCADE")
    void nenhumaFkParaAnimalsTemCascade() {
        List<String> comCascade = jdbcTemplate.queryForList(
                "select tc.constraint_name "
                        + "from information_schema.table_constraints tc "
                        + "join information_schema.referential_constraints rc "
                        + "  on tc.constraint_name = rc.constraint_name "
                        + "join information_schema.constraint_column_usage ccu "
                        + "  on tc.constraint_name = ccu.constraint_name "
                        + "where tc.constraint_type = 'FOREIGN KEY' "
                        + "  and tc.table_schema = 'public' "
                        + "  and ccu.table_name = 'animals' "
                        + "  and rc.delete_rule <> 'NO ACTION'",
                String.class);

        assertThat(comCascade)
                .as("a limpeza do animal e feita em codigo, no AnimalPurger, para ficar "
                        + "visivel e testavel - ver o javadoc do PersonServiceImpl")
                .isEmpty();
    }

}

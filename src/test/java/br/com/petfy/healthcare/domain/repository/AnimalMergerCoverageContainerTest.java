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
 * O teste que obriga alguem a DECIDIR, e nao a lembrar.
 *
 * A uniao de cadastros (Tela 32) tem uma pergunta por tabela que aponta para {@code animals}: <b>o
 * que aponta para o animal absorvido move para o sobrevivente, ou fica?</b> A resposta nao e
 * mecanica — evento move porque a vida registrada e uma so, e vinculo nao move porque custodia e
 * acesso sao relacoes que <i>cada cadastro</i> estabeleceu com pessoas diferentes.
 *
 * <b>E exatamente o mesmo defeito que o {@code AnimalPurgerCoverageContainerTest} existe para
 * impedir</b>, e ele ja aconteceu tres vezes com a exclusao: uma tabela nova passa a apontar para
 * {@code animals} e ninguem atualiza a lista. Ali o sintoma era um 500; aqui seria pior e
 * silencioso — a uniao terminaria "com sucesso" deixando eventos para tras no cadastro absorvido,
 * e ninguem descobriria ate alguem procurar uma vacina que sumiu do historico.
 *
 * <b>Por que nao da para fazer isto com mock:</b> a lista de quem aponta para {@code animals} mora
 * no schema, e o schema so existe quando o Flyway roda.
 */
@SpringBootTest
@DisplayName("cobertura do AnimalMerger contra o schema real")
class AnimalMergerCoverageContainerTest extends PostgresContainerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * A vida registrada: o que a linha do tempo mostra.
     *
     * "Nenhum evento e descartado" e a promessa que a tela faz a quem decide, e esta lista e ela.
     */
    private static final Set<String> MOVEM = Set.of(
            // O custo E da vida do animal: "o custo do Code" nao muda porque descobriram que
            // havia dois cadastros dele. Se ficasse para tras, unir os cadastros faria metade do
            // que o tutor gastou desaparecer da conta — sem aviso nenhum.
            "animal_costs",
            "attachments",
            "vaccines",
            "health_records",
            "animal_weight_history",
            "antiparasitics",
            "animal_health_conditions",
            "care_instructions",
            "observations");

    /**
     * O que fica com o cadastro absorvido, cada um por uma razao propria.
     *
     * <b>Nao e "o resto".</b> Cada tabela aqui foi olhada e recusada, e o comentario diz por que —
     * uma lista de excecoes sem motivo vira o lugar onde a proxima tabela e jogada sem pensar.
     */
    private static final Set<String> FICAM = Set.of(
            // O animal ficaria com dois responsaveis, e "quem responde por ele" nao admite dois.
            "custodies",
            // "O acesso da sua clinica continua sendo o que ele concedeu." Mover daria a quem
            // PEDIU a uniao acesso ao animal inteiro, que ninguem lhe deu.
            "grants",
            // Matricula ocupa vaga: mover dobraria a ocupacao, e a Tela 17 esperaria o mesmo
            // animal duas vezes na segunda de manha.
            "enrollments",
            // Convite pendente e uma conversa comecada sobre AQUELE cadastro.
            "pet_tutor_invites",
            // Reescrever a quem um log de acesso se refere seria falsificar uma auditoria.
            "sensitive_access_log");

    /**
     * As netas, que seguem o pai.
     *
     * Correcao aponta para vacina e para atendimento, cumprimento aponta para orientacao, presenca
     * aponta para matricula, escopo aponta para concessao. Mover o pai as leva junto; move-las
     * tambem seria mover duas vezes. E o pedido de uniao aponta para os dois animals por
     * definicao — ele e o registro DA uniao, e nao algo que pende de um dos lados.
     */
    private static final Set<String> SEGUEM_O_PAI = Set.of(
            "vaccine_corrections",
            "health_record_corrections",
            "care_instruction_fulfillments",
            "attendances",
            "grant_scopes",
            "animal_merge_requests");

    @Test
    @DisplayName("toda tabela que aponta para animals deve estar classificada como move ou fica")
    void todaTabelaClassificada() {
        Set<String> classificadas = new java.util.HashSet<>(MOVEM);
        classificadas.addAll(FICAM);
        classificadas.addAll(SEGUEM_O_PAI);

        assertThat(tabelasQueAlcancamAnimals())
                .as("tabela pendurada no animal sem ninguem ter decidido se ela move na uniao "
                        + "de cadastros — ver o javadoc do AnimalMerger")
                .allSatisfy(tabela -> assertThat(classificadas).contains(tabela));
    }

    /**
     * O contrario tambem importa: uma tabela que saiu do schema e ficou na lista faz o merger
     * emitir update para tabela que nao existe, e a proxima pessoa a ler a lista acredita nela.
     */
    @Test
    @DisplayName("as listas nao podem ter tabela que nao aponta mais para animals")
    void listasNaoTemTabelaObsoleta() {
        List<String> alcancamAnimals = tabelasQueAlcancamAnimals();

        assertThat(MOVEM)
                .as("tabela na lista de MOVEM que nao aponta mais para animals")
                .allSatisfy(tabela -> assertThat(alcancamAnimals).contains(tabela));

        assertThat(FICAM)
                .as("tabela na lista de FICAM que nao aponta mais para animals")
                .allSatisfy(tabela -> assertThat(alcancamAnimals).contains(tabela));
    }

    /**
     * As duas listas nao podem se cruzar.
     *
     * Uma tabela em MOVEM e em FICAM ao mesmo tempo passaria nos dois testes acima e diria coisas
     * opostas sobre o mesmo dado — e quem lesse a lista escolheria a que confirmasse o que ja
     * achava.
     */
    @Test
    @DisplayName("nenhuma tabela pode estar em duas listas ao mesmo tempo")
    void listasNaoSeCruzam() {
        assertThat(MOVEM).doesNotContainAnyElementsOf(FICAM);
        assertThat(MOVEM).doesNotContainAnyElementsOf(SEGUEM_O_PAI);
        assertThat(FICAM).doesNotContainAnyElementsOf(SEGUEM_O_PAI);
    }

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
                        + "  union"
                        + "  select f.filha from fk f join alcanca a on f.mae = a.filha"
                        + ") select distinct filha from alcanca where filha <> 'animals'",
                String.class);
    }

}

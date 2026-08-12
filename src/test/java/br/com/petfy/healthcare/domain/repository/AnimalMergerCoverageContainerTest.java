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
            // O AVISTAMENTO MOVE, e e o caso que mais parece "vinculo" desta lista.
            //
            // Ele parece vinculo porque fala de uma pessoa vendo um animal num dia. Mas o que ele
            // registra e um fato do ANIMAL: ele estava vivo e na praca naquele dia. Se ficasse
            // para tras, unir dois cadastros do mesmo gato faria "visto por ultimo" saltar para
            // "ha 22 dias" no instante da uniao — e a colonia sairia procurando um gato que
            // alguem viu hoje de manha.
            //
            // <b>E o unico dos que movem com indice unico</b> — (animal, pessoa, dia) —, e a
            // colisao e o caso TIPICO da uniao: dois cadastros do mesmo gato existem porque duas
            // pessoas o registraram, e as duas o veem no mesmo dia. O merger tem um passo proprio
            // para isso (`desfazerAvistamentosEmDuplicata`), sem o qual a uniao inteira falharia
            // por violacao de chave.
            "animal_sightings",
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
            "sensitive_access_log",
            // O OBITO FICA, e ele e o caso que mais parece "evento" desta lista inteira.
            //
            // Duas razoes, e a segunda e a que decide. A primeira e mecanica: a chave primaria da
            // tabela E o animal, entao mover a linha e reescrever a identidade dela — e se os dois
            // cadastros tiverem obito, o insert colide.
            //
            // A segunda e de significado: o obito nao e um fato solto, e o que ENCERROU uma
            // custodia especifica, daquele cadastro. A custodia fica; mover so o obito deixaria o
            // registro do fim apontando para um animal cuja custodia nunca terminou.
            //
            // A consequencia — um cadastro absorvido levando o obito consigo — quase nao alcanca a
            // realidade: unir exige quem responde pelo animal, e por um animal morto ninguem
            // responde. O aceite da uniao bate em `requireCustodia` antes de chegar aqui.
            "animal_deaths",
            // O PEDIDO DE CONCORDANCIA FICA, pela mesma razao do pedido de uniao: ele nao e um
            // fato do animal, e uma pergunta feita a um grupo especifico sobre um cadastro
            // especifico. Move-lo faria um pedido de adocao pendente reaparecer apontando para um
            // cadastro que quem pediu nunca viu — e a segunda pessoa concordaria com outra coisa.
            "group_approvals",
            // O ENCAMINHAMENTO FICA, e aqui a razao e mecanica antes de ser de significado: ele
            // aponta para o animal E para o grant que o aceite produziu — e grants FICA. Mover o
            // encaminhamento sem mover a concessao deixaria um pedido autorizado apontando para um
            // acesso de outro cadastro, e a tela do especialista anunciaria um prazo que vale para
            // um animal diferente do que ela mostra.
            //
            // O significado confirma: um encaminhamento e uma pergunta que uma clinica fez a um
            // tutor sobre AQUELE cadastro — "piora da claudicacao nos ultimos 3 meses" foi escrito
            // olhando o historico daquele cadastro. Move-lo faria a pergunta reaparecer sobre um
            // conjunto de eventos que quem a escreveu nunca viu.
            "referrals",
            // O APADRINHAMENTO FICA. Ele aponta para uma linha de `animal_costs`, que MOVE — e isso
            // parece motivo para mover junto. Nao e: apadrinhamento nao e fato do animal, e um
            // acordo entre uma pessoa e a organizacao que respondia por AQUELE cadastro. Mover
            // faria o padrinho aparecer bancando um animal cujo abrigo ele nunca escolheu, e o
            // `source_cost_id` continua valido de qualquer forma: o custo levou o id consigo.
            "sponsorships");

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
            // O dia combinado e do combinado, que e da matricula — e a matricula FICA. Mover os
            // dias sem mover a matricula deixaria uma turma esperando um animal na segunda e a
            // matricula dele em outro cadastro.
            "enrollment_weekdays",
            "grant_scopes",
            // O escopo do encaminhamento segue o encaminhamento, como o escopo da concessao segue a
            // concessao. E ele alcanca `animals` so por transitividade — nao ha coluna de animal ali.
            "referral_scopes",
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

package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TimelineRepository extends JpaRepository<TimelineEntry, UUID> {

    /**
     * A linha do tempo de um animal, do mais recente para o mais antigo.
     *
     * <b>Ordena por {@code occurredAt}, e nao por {@code recordedAt}</b> - e por isso
     * que a vacina de 2019 cadastrada hoje aparece em 2019. O desempate por
     * {@code recordedAt} existe para a ordem ser estavel entre paginas: sem ele, dois
     * eventos do mesmo dia poderiam trocar de lugar entre a pagina 1 e a 2, e o cliente
     * veria um item duas vezes e outro nunca.
     *
     * <b>Paginada</b> porque a linha do tempo de um animal de dez anos e a maior
     * colecao que este produto vai servir - e a unica que cresce para sempre.
     */
    @Query("select t from TimelineEntry t where t.animalId = :animalId "
            + "order by t.occurredAt desc, t.recordedAt desc")
    Page<TimelineEntry> findDoAnimal(@Param("animalId") UUID animalId, Pageable pageable);

    /**
     * A mesma linha, recortada por quem registrou — os dois filtros da Tela 30.
     *
     * <b>Os dois recortes existem porque respondem perguntas diferentes numa consulta.</b> "So
     * desta clinica" e "o que nos ja sabiamos deste animal antes de hoje"; "so o que eu registrei"
     * e "o que EU vi com meus olhos", que e o que a veterinaria confere antes de contradizer um
     * colega. Um filtro de cliente sobre a pagina carregada responderia errado as duas: a pagina
     * tem vinte itens e a vida do animal tem centenas.
     *
     * <b>Nulo desliga o filtro</b>, em vez de existir uma consulta por combinacao. Com dois
     * recortes seriam quatro metodos, e o quarto — os dois ao mesmo tempo — e exatamente o que a
     * tela permite marcar.
     *
     * <b>O escopo NAO entra aqui</b>, e continua sendo aplicado depois de paginar, como sempre
     * foi: filtrar por autor e recortar o que a pessoa pediu, e mascarar por escopo e esconder o
     * que ela nao alcanca. Misturar os dois faria um evento fora do escopo desaparecer quando ela
     * marcasse "so desta clinica" — e sumir diria que o animal nunca foi atendido.
     */
    /*
     * O TERCEIRO RECORTE, "desde quando", chegou com a hospedagem (Tela 47).
     *
     * "Isto e o que aconteceu com ele desde que saiu de casa" — e a pergunta que o tutor em viagem
     * faz, e ela e de JANELA, nao de autor. Filtrar no cliente seria o erro que o comentario acima
     * ja nomeia por outro motivo: a pagina tem vinte itens e a estadia pode ter mais.
     *
     * <b>Este NAO desliga com nulo, e os outros dois desligam.</b> A diferenca nao e de gosto: o
     * Postgres recusa a consulta com {@code could not determine data type of parameter} quando o
     * {@code :desde is null} recebe um nulo sem tipo — os dois de cima escapam porque o Hibernate os
     * associa a colunas {@code uuid}, e este nao tem a quem se associar dentro do {@code is null}.
     *
     * Entao quem chama passa uma DATA-PISO para dizer "sem recorte", como o
     * {@code NENHUMA_ORGANIZACAO} do servico faz para o outro filtro. O truque e o mesmo, e a razao
     * tambem: um sentinela explicito e melhor que uma consulta que so quebra em producao.
     */
    @Query("select t from TimelineEntry t where t.animalId = :animalId "
            + "and (:organizationId is null or t.organizationId = :organizationId) "
            + "and (:recordedByPersonId is null or t.recordedByPersonId = :recordedByPersonId) "
            + "and t.occurredAt >= :desde "
            + "order by t.occurredAt desc, t.recordedAt desc")
    Page<TimelineEntry> findDoAnimalFiltrada(@Param("animalId") UUID animalId,
                                             @Param("organizationId") UUID organizationId,
                                             @Param("recordedByPersonId") UUID recordedByPersonId,
                                             @Param("desde") LocalDateTime desde,
                                             Pageable pageable);

    /**
     * Quando cada pessoa contribuiu por ultimo neste animal.
     *
     * <b>E o que faz a rede de quem cuida parecer viva</b> (DESIGN 5.4): "cada pessoa
     * aparece com iniciais, papel e a ultima contribuicao - registrou hoje, registrou
     * ontem". Sem isso a rede e uma lista de nomes, e nao mostra que alguem esta cuidando.
     *
     * <b>Agregada, e nao uma consulta por pessoa.</b> Uma rede de cinco pessoas custaria
     * cinco consultas, e e o mesmo N+1 que a V29 acabou de tirar da linha do tempo.
     *
     * Ordena por {@code recordedAt} e nao por {@code occurredAt} de proposito: a pergunta
     * e "quando esta pessoa apareceu por aqui", que e digitacao. Quem lanca hoje a vacina
     * de 2019 contribuiu hoje.
     */
    @Query("select t.recordedByPersonId as pessoaId, max(t.recordedAt) as em "
            + "from TimelineEntry t "
            + "where t.animalId = :animalId and t.recordedByPersonId is not null "
            + "group by t.recordedByPersonId")
    List<UltimaContribuicao> ultimaContribuicaoPorPessoa(@Param("animalId") UUID animalId);

    /** O mesmo, por organizacao: "a Clinica Norte registrou ontem". */
    @Query("select t.organizationId as pessoaId, max(t.recordedAt) as em "
            + "from TimelineEntry t "
            + "where t.animalId = :animalId and t.organizationId is not null "
            + "group by t.organizationId")
    List<UltimaContribuicao> ultimaContribuicaoPorOrganizacao(@Param("animalId") UUID animalId);

    /**
     * O TAMANHO da vida registrada deste cadastro — o que a Tela 32 poe lado a lado.
     *
     * <b>"147 eventos, desde 14/02/2019" contra "1, hoje" e o que faz quem decide entender qual
     * dos dois e o cadastro real do animal.</b> Sem isso a comparacao mostraria dois nomes e duas
     * racas, e a pergunta "qual devo manter" ficaria sem a resposta que ela realmente tem.
     *
     * <b>Uma consulta agregada, e nao a lista.</b> Um cadastro de dez anos tem centenas de
     * eventos, e trazer todos para contar em memoria — duas vezes, uma por lado — seria pagar a
     * vida inteira do animal para escrever quatro numeros.
     */
    @Query("select count(t) as eventos, min(t.occurredAt) as primeiro, "
            + "count(distinct t.recordedByPersonId) as pessoas, "
            + "count(distinct t.organizationId) as organizacoes "
            + "from TimelineEntry t where t.animalId = :animalId")
    Tamanho tamanhoDe(@Param("animalId") UUID animalId);

    /**
     * Quanto existe de cada tipo de evento, e desde quando — o "o que vai junto" da Tela 45.
     *
     * <b>Sobre a linha do tempo, e nao contando cada tabela.</b> A alternativa era uma contagem por
     * origem — atendimentos em {@code health_records}, pesagens em {@code animal_weight_history},
     * observacoes em {@code observations} — e seriam seis consultas para responder a mesma pergunta,
     * com um risco que a agregada nao tem: <b>o numero mostrado na caixa seria de uma fonte, e o
     * que o especialista abre depois vem da linha do tempo mascarada por escopo.</b> Contar aqui faz
     * a promessa da tela ("12 atendimentos desde 2019") e a entrega serem literalmente a mesma
     * consulta, com o mesmo mapeamento de {@link br.com.petfy.healthcare.domain.entity.TimelineEventType}
     * para escopo.
     *
     * Devolve por TIPO e nao por escopo porque o mapeamento vive no enum, onde ja esta documentado —
     * dobra-lo em JPQL criaria uma segunda tabela de verdade sobre que escopo alcanca o que.
     */
    @Query("select t.eventType as tipo, count(t) as eventos, min(t.occurredAt) as primeiro "
            + "from TimelineEntry t where t.animalId = :animalId "
            + "group by t.eventType")
    List<PorTipo> contagemPorTipo(@Param("animalId") UUID animalId);

    /**
     * O tamanho da vida registrada NUM PERIODO — "61 registros no ano, por 5 pessoas e 3
     * organizacoes" (Tela 48).
     *
     * <b>E o {@code tamanhoDe} com janela</b>, e nao uma consulta nova por capricho: o resumo anual e
     * sobre o ano, e reusar a contagem total diria "147 eventos" num documento que se chama "o ano do
     * Code". Um filtro em memoria sobre a linha inteira leria a vida de dez anos para contar doze
     * meses.
     */
    @Query("select count(t) as eventos, min(t.occurredAt) as primeiro, "
            + "count(distinct t.recordedByPersonId) as pessoas, "
            + "count(distinct t.organizationId) as organizacoes "
            + "from TimelineEntry t where t.animalId = :animalId "
            + "and t.occurredAt between :de and :ate")
    Tamanho tamanhoNoPeriodo(@Param("animalId") UUID animalId,
                             @Param("de") LocalDateTime de,
                             @Param("ate") LocalDateTime ate);

    /**
     * "Quem cuidou dele este ano", com quantos registros cada um.
     *
     * <b>Agrupa por NOME, e nao por id</b>, e a escolha e deliberada. A view ja carrega
     * {@code recorded_by_name} e {@code organization_name} desde a V29 — foi o que tirou o N+1 da
     * linha do tempo —, e agrupar por id obrigaria uma consulta a mais so para descobrir como chamar
     * cada linha do resumo.
     *
     * <b>A dupla (pessoa, organizacao) e a unidade</b>, porque e assim que o desenho escreve:
     * "Rafaela Lopes, pela Creche Quintal" e "Juliana Dias, co-tutora" sao duas linhas, e a mesma
     * pessoa registrando por si e pela creche sao duas contribuicoes diferentes — foi ela quem
     * decidiu assinar de cada jeito.
     */
    @Query("select t.recordedByName as pessoa, t.organizationName as organizacao, "
            + "count(t) as registros "
            + "from TimelineEntry t where t.animalId = :animalId "
            + "and t.occurredAt between :de and :ate "
            + "and (t.recordedByName is not null or t.organizationName is not null) "
            + "group by t.recordedByName, t.organizationName "
            + "order by count(t) desc")
    List<QuemCuidou> quemCuidouNoPeriodo(@Param("animalId") UUID animalId,
                                         @Param("de") LocalDateTime de,
                                         @Param("ate") LocalDateTime ate);

    /** Projecao da consulta acima. */
    interface QuemCuidou {

        String getPessoa();

        String getOrganizacao();

        long getRegistros();

    }

    /** Projecao da consulta acima. */
    interface PorTipo {

        br.com.petfy.healthcare.domain.entity.TimelineEventType getTipo();

        long getEventos();

        LocalDateTime getPrimeiro();

    }

    /** Projecao da consulta acima. */
    interface Tamanho {

        long getEventos();

        LocalDateTime getPrimeiro();

        long getPessoas();

        long getOrganizacoes();

    }

    /** Projecao das duas consultas acima. O id e de pessoa ou de organizacao, conforme a consulta. */
    interface UltimaContribuicao {

        UUID getPessoaId();

        LocalDateTime getEm();

    }

}

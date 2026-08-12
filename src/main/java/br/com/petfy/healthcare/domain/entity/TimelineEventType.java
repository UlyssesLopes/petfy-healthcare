package br.com.petfy.healthcare.domain.entity;

/**
 * O tipo de um evento na linha do tempo.
 *
 * <b>Nao substitui a especializacao de cada tabela.</b> Ele diz de que natureza e a
 * entrada, para o cliente saber que recurso buscar quando quiser o detalhe - a vacina
 * continua tendo validade e proxima dose, a condicao continua tendo gravidade e
 * encerramento. O tipo e o indice, nao o conteudo.
 *
 * <b>Cada valor mapeia um escopo de concessao</b>, e e isso que permite filtrar a
 * linha do tempo por quem esta lendo: quem recebeu so CARTEIRA ve vacina e
 * antiparasitario, e nao ve atendimento.
 */
public enum TimelineEventType {

    VACINA(GrantScope.CARTEIRA),

    ANTIPARASITARIO(GrantScope.CARTEIRA),

    ATENDIMENTO(GrantScope.PRONTUARIO),

    /**
     * O que alguem viu, e nao o que alguem concluiu (3.11).
     *
     * <b>A distincao com ato clinico e "a mais importante do produto"</b> (DESIGN 5.5), e
     * ela nao se faz por quem escreveu: observacao e um tipo proprio porque e um fato de
     * natureza diferente. Observacao nunca vira ato clinico sozinha - pode ser
     * referenciada por um como evidencia, e e isso que faz o que a creche viu chegar a
     * quem pode diagnosticar.
     */
    OBSERVACAO(GrantScope.OBSERVACOES),

    PESAGEM(GrantScope.PESO),

    CONDICAO(GrantScope.CONDICOES),

    ANEXO(GrantScope.ANEXOS),

    /**
     * Alguem mandou fazer algo com o animal: prescricao, medicacao, tema de casa.
     *
     * Vai no escopo do prontuario porque orientacao clinica <b>e</b> informacao
     * clinica - saber que o animal toma anticonvulsivante diz o que ele tem.
     */
    ORIENTACAO(GrantScope.PRONTUARIO),

    /**
     * Alguem cumpriu uma orientacao.
     *
     * Entrada propria, e nao a mesma da orientacao: mandar e fazer sao fatos
     * diferentes, e juntar os dois perderia o historico de aderencia - o dado que o
     * veterinario nao tem quando o tratamento nao funciona.
     */
    CUMPRIMENTO(GrantScope.PRONTUARIO),

    /**
     * Dois cadastros do mesmo animal viraram um (Tela 32).
     *
     * <b>Nao e dado de saude — e ato administrativo</b>, e por isso a view o marca com
     * {@code is_health_data = false}. Mas ele precisa aparecer: sem ele, quem le ve 147 eventos
     * assinados por gente de tres organizacoes mais um lote que surgiu de repente vindo de um
     * cadastro que nao existe em lugar nenhum — e conclui que houve erro de sistema.
     *
     * <b>O ESCOPO AQUI E UM COMPROMISSO, e vale dizer qual.</b> O modelo de concessao exige que
     * todo tipo aponte para um escopo, e nao tem "sempre visivel". A uniao e sobre a IDENTIDADE
     * do animal, que e o que a carteira carrega — entao ela entra na {@code CARTEIRA}, que e
     * tambem o escopo mais concedido. <b>A consequencia registrada:</b> uma concessao que tenha
     * so {@code OBSERVACOES}, ou so {@code PESO}, nao vera a uniao, e para ela o historico vai
     * parecer ter dobrado sozinho. Resolver de verdade pede um escopo que nao existe hoje.
     */
    UNIAO(GrantScope.CARTEIRA),

    /**
     * O animal morreu, e a linha do tempo fecha (Tela 33).
     *
     * <b>Nao e dado de saude</b>, e a view o marca com {@code is_health_data = false} pela mesma
     * razao da uniao: quem tem escopo restrito precisa ver que a vida terminou sem que isso lhe
     * abra o prontuario. Esconder o obito de quem so alcanca a carteira faria a creche continuar
     * esperando o animal na segunda-feira.
     *
     * <b>O escopo aqui e o mesmo compromisso da UNIAO</b>, e vale repetir qual: todo tipo precisa
     * apontar para um escopo, e nao existe "sempre visivel". O fim entra na {@code CARTEIRA} — o
     * escopo mais concedido, e o que carrega a identidade do animal. Quem tenha so
     * {@code OBSERVACOES} ou so {@code PESO} nao vera o obito, e para essa pessoa a linha do tempo
     * simplesmente para.
     */
    OBITO(GrantScope.CARTEIRA);

    private final GrantScope escopo;

    TimelineEventType(GrantScope escopo) {
        this.escopo = escopo;
    }

    /** O escopo que uma concessao precisa ter para alcancar este tipo de evento. */
    public GrantScope escopoExigido() {
        return escopo;
    }

}

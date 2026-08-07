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
    CUMPRIMENTO(GrantScope.PRONTUARIO);

    private final GrantScope escopo;

    TimelineEventType(GrantScope escopo) {
        this.escopo = escopo;
    }

    /** O escopo que uma concessao precisa ter para alcancar este tipo de evento. */
    public GrantScope escopoExigido() {
        return escopo;
    }

}

package br.com.petfy.healthcare.domain.entity;

/**
 * Por que uma custodia terminou.
 *
 * <b>A divisao que importa aqui e entre motivo com sucessor e motivo terminal.</b>
 * O quarto invariante do produto diz que nenhuma custodia termina sem sucessor - e
 * a excecao nao e um caso esquecido, e o fim da propria necessidade de sucessor:
 * animal morto nao precisa de alguem que responda por ele.
 *
 * E por isso que este enum existe em vez de um campo de texto: o banco precisa
 * poder recusar uma custodia encerrada sem destino, e nao ha como checar isso
 * contra texto livre.
 */
public enum CustodyEndReason {

    /** Passou para outro tutor. Exige sucessor. */
    TRANSFERENCIA,

    /** Adocao: o abrigo cadastra o adotante, e e isso que encerra a custodia dele. */
    ADOCAO,

    /** O lar transitorio devolveu a quem o entregou. Exige sucessor. */
    DEVOLUCAO,

    /** Entregue a uma organizacao. Exige sucessor. */
    ENTREGA,

    /** O animal morreu. A linha do tempo encerra, e nao some. */
    OBITO,

    /**
     * O animal foi perdido.
     *
     * Estado distinto de custodia encerrada com sucessor, e e onde o microchip vale
     * mais: quem encontra o animal precisa poder chegar a este registro.
     */
    PERDA;

    /**
     * Se este motivo dispensa sucessor.
     *
     * So obito e perda dispensam. Todo o resto e repasse, e repasse sem destino
     * deixaria o animal orfao de registro - que e exatamente o que o invariante
     * proibe.
     */
    public boolean dispensaSucessor() {
        return this == OBITO || this == PERDA;
    }

}

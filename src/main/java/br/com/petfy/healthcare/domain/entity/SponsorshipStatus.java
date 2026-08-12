package br.com.petfy.healthcare.domain.entity;

/** Em que pe esta um apadrinhamento (Tela 46). */
public enum SponsorshipStatus {

    /** O padrinho banca, e o abrigo conta com isso. */
    ATIVO,

    /**
     * O padrinho pediu para parar, e ainda banca por trinta dias.
     *
     * <b>Este estado existe por causa de uma frase do desenho:</b> <i>"Pode parar quando quiser, sem
     * justificar. O abrigo e avisado com 30 dias para se organizar."</i> Sem ele, encerrar seria
     * imediato — e o abrigo descobriria no dia em que o remedio nao fosse comprado.
     */
    ENCERRAMENTO_PEDIDO,

    /**
     * Acabou.
     *
     * <b>Nao apaga a linha.</b> O abrigo precisa saber que aquele custo deixou de ser coberto, e o
     * padrinho precisa poder ver o que bancou — apagar transformaria "parei em maio" em "nunca
     * ajudei".
     */
    ENCERRADO

}

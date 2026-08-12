package br.com.petfy.healthcare.domain.entity;

/**
 * De onde o valor veio.
 *
 * <b>Existe para o tutor LER agrupado</b> — "Creche Quintal · mensalidade" separado de "diaria
 * avulsa de 22/07" —, e nao para o produto tratar cada um de um jeito. Nenhuma regra do dominio
 * olha para este campo; quem olha e a tela.
 */
public enum AnimalCostKind {

    /** Saiu de um registro de atendimento (Tela 40). */
    ATENDIMENTO,

    /**
     * Saiu de uma dose de vacina ou de antiparasitario.
     *
     * <b>Tipo proprio e nao ATENDIMENTO, porque uma dose nao e um atendimento</b> — ela mora na
     * `vaccines`, com catalogo e data de proxima dose proprios, e o tutor que registra a dose que
     * estava vencendo nao registrou consulta nenhuma. Como o `kind` existe para o tutor LER
     * agrupado, chamar dose de atendimento erraria justamente na leitura.
     *
     * <b>E e ele que da preco a previsao da Tela 38:</b> ligado a dose pelo `sourceVaccineId`, o
     * valor de hoje precifica o reforco do ano que vem.
     */
    VACINA,

    /** O combinado da creche, que se repete todo mes (Tela 41). */
    CRECHE_MENSALIDADE,

    /**
     * A diaria de quem veio fora dos dias combinados (Tela 41).
     *
     * <b>Ela entra sozinha:</b> a creche marca a entrada num dia fora da combinacao e o evento de
     * entrada carrega o valor. "Ninguem digitou nada."
     */
    CRECHE_DIARIA,

    /**
     * O que o tutor comprou por fora (Tela 42).
     *
     * <b>O unico lancamento manual do produto.</b> Tudo que acontece numa organizacao entra
     * sozinho, porque alguem ja estava registrando o evento; racao e coisas de mercado nao tem
     * organizacao atras — so existem se o tutor lancar.
     */
    COMPRA

}

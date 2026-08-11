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

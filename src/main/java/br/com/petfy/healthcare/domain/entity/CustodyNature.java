package br.com.petfy.healthcare.domain.entity;

/**
 * Por que alguem responde por um animal.
 *
 * Nao e o mesmo que nivel de acesso: as quatro naturezas abaixo tem o mesmo
 * alcance sobre o registro. O que muda e a expectativa de duracao e o que se
 * espera no encerramento.
 */
public enum CustodyNature {

    /** O tutor comum. Sem fim previsto. */
    DEFINITIVA,

    /**
     * Lar transitorio. Tem prazo combinado, e o fim dele nao dispensa sucessor -
     * quem recebeu devolve para alguem, nunca para lugar nenhum.
     */
    TRANSITORIA,

    /** Abrigo ou ONG respondendo pelo animal enquanto ele nao tem tutor. */
    INSTITUCIONAL,

    /**
     * Custodia que nasce sem antecessor: animal de rua, resgate, ninhada.
     *
     * E o unico caso em que nao ha custodia anterior, e e por isso que ela existe
     * como natureza propria em vez de virar uma DEFINITIVA sem historia.
     */
    RESGATE

}

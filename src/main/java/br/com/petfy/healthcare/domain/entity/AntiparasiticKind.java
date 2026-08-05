package br.com.petfy.healthcare.domain.entity;

/**
 * Tipo de antiparasitario. Nao e String livre pelo mesmo motivo de {@link Species}:
 * "vermifugo", "vermifugo oral" e "DEWORMER" sao a mesma coisa para o tutor e
 * tres valores distintos para o banco, e quem consome a lista precisa agrupar.
 *
 * Sao dois porque sao dois os recorrentes que o tutor esquece. Um terceiro tipo
 * e um passo deliberado - novo valor aqui e nova entrada no catalogo.
 */
public enum AntiparasiticKind {
    /** Vermifugo. */
    DEWORMER,
    /** Antipulgas e carrapatos. */
    FLEA_TICK
}

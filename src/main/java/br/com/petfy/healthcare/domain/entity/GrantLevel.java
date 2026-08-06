package br.com.petfy.healthcare.domain.entity;

/**
 * O que um acesso permite fazer dentro do escopo concedido.
 *
 * Herda os dois niveis nao-titulares do PetTutorRole: HOLDER migra para custodia
 * no P2b, e EDITOR e VIEWER viram isto. Nivel e escopo sao perguntas diferentes -
 * o nivel diz se escreve, o escopo diz sobre o que.
 */
public enum GrantLevel {

    /** Le o que o escopo permite, e nao escreve nada. */
    VIEWER,

    /** Le e escreve dentro do escopo. */
    EDITOR;

    /** Hierarquia pela ordem da declaracao, como no PetTutorRole. */
    public boolean permite(GrantLevel exigido) {
        return this.ordinal() >= exigido.ordinal();
    }

}

package br.com.petfy.healthcare.domain.entity;

/**
 * O que cada tutor pode fazer com um pet.
 *
 * Sao tres, e nao dois, porque "dividir o pet" nao e uma coisa so: a esposa que
 * cuida junto precisa registrar vacina, e a avo que so quer acompanhar nao
 * precisa poder editar nada. Quem decide qual dos dois casos e o titular, um por
 * pet.
 *
 * A ordem da declaracao e a hierarquia: quem tem um papel pode tudo que os
 * seguintes podem - ver {@link #permite(PetTutorRole)}.
 */
public enum PetTutorRole {

    /**
     * Dono do pet. Faz tudo que o EDITOR faz, e mais o que ninguem mais faz:
     * convidar e remover tutores, mudar o papel de cada um, transferir a
     * titularidade e apagar o pet.
     *
     * Existe exatamente um por pet, garantido por indice unico parcial.
     */
    HOLDER,

    /** Le e escreve: registra vacina, corrige peso, edita o cadastro do pet. */
    EDITOR,

    /** So le. Acompanha a carteira e a agenda, nao altera nada. */
    VIEWER;

    /**
     * Se este papel alcanca o nivel exigido. Como a hierarquia e a ordem da
     * declaracao, "alcancar" e vir antes ou ser igual.
     */
    public boolean permite(PetTutorRole exigido) {
        return this.ordinal() <= exigido.ordinal();
    }

    public boolean isHolder() {
        return this == HOLDER;
    }

    public boolean podeEditar() {
        return permite(EDITOR);
    }

}

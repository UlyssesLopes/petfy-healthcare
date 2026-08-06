package br.com.petfy.healthcare;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.security.ProfessionalContext;

/**
 * Monta o contexto profissional nos testes.
 *
 * Substituiu {@code Person.builder().organization(x)}. A troca nao e de sintaxe: o
 * campo dizia que a pessoa <i>pertence</i> a uma organizacao, no singular e para
 * sempre. O contexto diz em nome de quem ela age <b>nesta requisicao</b> - e admite
 * que a resposta seja "ninguem", que e o veterinario autonomo.
 */
public final class Contextos {

    private Contextos() {
    }

    /** Atuando em nome de uma organizacao. */
    public static ProfessionalContext por(Person pessoa, Organization organizacao) {
        return new ProfessionalContext(pessoa, organizacao);
    }

    /**
     * Atuando por si.
     *
     * Existe como metodo proprio para o teste dizer isso em voz alta: e o autonomo, e
     * nao um contexto que alguem esqueceu de preencher.
     */
    public static ProfessionalContext autonomo(Person pessoa) {
        return new ProfessionalContext(pessoa, null);
    }

}

package br.com.petfy.healthcare.domain.dto;

/**
 * Em nome de quem uma pessoa pode agir.
 *
 * Nao e papel, e nao vem de campo nenhum do cadastro: a secao 9.3 do PRODUTO e dura
 * sobre isso - a pessoa chega numa area por causa do que ela <b>tem</b>, e o papel
 * deixou de existir no modelo. {@link #PESSOA} e o profissional autonomo, que tem
 * credencial e nenhum vinculo.
 */
public enum ContextKind {

    /** Atuando por si. E o autonomo, e nao um cadastro incompleto. */
    PESSOA,

    /** Atuando em nome de uma organizacao com que a pessoa tem vinculo ativo. */
    ORGANIZACAO

}

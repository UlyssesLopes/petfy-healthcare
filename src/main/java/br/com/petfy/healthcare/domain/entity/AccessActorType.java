package br.com.petfy.healthcare.domain.entity;

/**
 * Quem acessou o dado de saude.
 *
 * So terceiros entram no log - ver V17 para por que leitura de tutor e de co-tutor
 * fica de fora. {@code OWNER} nao existe aqui hoje justamente por isso; se um dia
 * valer registrar, e um valor novo e um gancho, sem mudanca de schema.
 */
public enum AccessActorType {

    /** Veterinario de clinica com acesso concedido pelo tutor. */
    VET,

    /**
     * Quem abriu o link publico de carteira. Nao tem conta, entao nao tem id nem
     * nome - o que o log guarda dele e a evidencia de rede, que e tudo que existe.
     */
    SHARE_LINK

}

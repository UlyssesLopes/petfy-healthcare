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
    SHARE_LINK,

    /**
     * Quem digitou o microchip na busca de animal encontrado (Tela 34).
     *
     * <b>Tambem sem id e sem nome, e ali isso e uma promessa e nao uma limitacao:</b> "nao
     * guardamos quem fez a busca". Quem acha um animal na rua as 23h nao vai criar conta, e
     * exigir identificacao para devolver um bicho ao dono e o atrito que faz a pessoa desistir.
     *
     * O que fica e a evidencia de rede — a mesma do link publico —, e ela existe por dois
     * motivos: e o que permite ao tutor saber que o animal foi procurado, e e o unico rastro de
     * uma varredura de numeros, caso alguem tente.
     */
    MICROCHIP_SEARCH

}

package br.com.petfy.healthcare.domain.entity;

/**
 * A funcao de uma pessoa dentro de uma organizacao.
 *
 * <b>Funcao nao e capacidade.</b> A organizacao decide o que pode ser feito - ver
 * {@link OrganizationCapability} -, e a funcao decide quem, entre os membros, exerce
 * o que: a creche registra observacao, mas quem registra e o monitor; o abrigo
 * registra ato clinico, mas so atraves do vet membro.
 *
 * <b>E funcao nao e credencial.</b> Ser VETERINARIO aqui diz o papel na organizacao;
 * o que autoriza ato clinico e a credencial profissional da pessoa, que existe
 * independente de organizacao - e por isso que o vet autonomo atende sem ser membro
 * de nada.
 */
public enum MembershipRole {

    /** Pratica ato clinico, quando a organizacao tem a capacidade e ele a credencial. */
    VETERINARIO,

    /** Registra observacao e conteudo. Da creche, principalmente. */
    MONITOR,

    /** Ajuda no cuidado sem funcao clinica. Do abrigo. */
    VOLUNTARIO,

    /** Mantem o cadastro da organizacao e convida membros. */
    ADMINISTRADOR

}

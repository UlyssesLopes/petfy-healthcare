package br.com.petfy.healthcare.domain.entity;

/**
 * O quanto de um animal um acesso alcanca.
 *
 * <b>Por que escopo existe.</b> Ate o P2 o acesso era tudo ou nada, e isso
 * significava que matricular o cachorro numa creche entregava a ela o historico
 * completo de doencas do animal. A creche precisa de alergia, vacinacao,
 * medicacao em curso e contato de emergencia - nao do prontuario inteiro.
 *
 * <b>E e o que torna possivel o cartao de emergencia</b> (decisao 13): sem
 * quebra-vidro, a compensacao e o tutor preparar de vespera um compartilhamento
 * com escopo minimo. Sem escopo, esse cartao seria o prontuario inteiro num link.
 */
public enum GrantScope {

    /** Vacinas e antiparasitarios, com a situacao de cada dose. */
    CARTEIRA,

    /** Alergias e condicoes cronicas. O que a creche precisa e o socorro tambem. */
    CONDICOES,

    /** Atendimentos, categoria e diagnostico. O prontuario clinico. */
    PRONTUARIO,

    /** A serie de pesagens. */
    PESO,

    /** Os arquivos: laudo, exame, carteirinha de papel. */
    ANEXOS,

    /** Nome e telefone de quem responde pelo animal. */
    CONTATO

}

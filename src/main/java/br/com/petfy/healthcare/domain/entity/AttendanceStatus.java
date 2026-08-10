package br.com.petfy.healthcare.domain.entity;

/**
 * O estado do dia de um animal na creche.
 *
 * <b>ESPERADO existe sem ninguem ter marcado nada</b>, e e o que faz a Tela 17 poder dizer "14
 * esperados · 6 ja chegaram" as 7h34 de uma segunda-feira. Sem esse estado, a operacao comecaria
 * o dia com uma lista vazia e teria de adivinhar quem falta.
 */
public enum AttendanceStatus {

    /** Tem matricula ativa e ainda nao chegou. */
    ESPERADO,

    /** Chegou. */
    PRESENTE,

    /** Chegou e foi embora. */
    SAIU,

    /** Nao vem hoje — "tutor avisou que nao vem". */
    FALTA
}

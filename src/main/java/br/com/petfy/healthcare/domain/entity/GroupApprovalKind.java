package br.com.petfy.healthcare.domain.entity;

/**
 * O que, num grupo sem dono, exige o acordo de duas pessoas (Tela 43).
 *
 * <b>A lista e fechada, e o que ficou de FORA dela e a decisao.</b> Marcar que viu o gato,
 * registrar uma ferida, levar ao veterinario e lancar o que foi feito sao de qualquer um — e
 * travar qualquer um deles mataria a tela, porque o valor da colonia esta em o registro ser
 * barato. O acordo de duas protege apenas o que nao se desfaz.
 */
public enum GroupApprovalKind {

    /**
     * Passar o animal para uma pessoa (Tela 44).
     *
     * Irreversivel no unico sentido que importa: depois dela, quem responde pelo animal e outra
     * pessoa, e o grupo nao decide mais nada sobre ele.
     */
    ADOCAO,

    /**
     * Encerrar a linha do tempo.
     *
     * <b>Aqui a protecao vale mais do que no animal com tutor.</b> La quem encerra e quem
     * conviveu com o animal todo dia; na colonia, uma pessoa que nao ve o gato ha duas semanas
     * pode concluir que ele morreu quando ele apenas mudou de quarteirao.
     */
    OBITO,

    /** Tirar alguem do grupo. O unico dos tres que fala de uma pessoa, e nao de um animal. */
    REMOCAO_DE_MEMBRO

}

package br.com.petfy.healthcare.domain.entity;

/**
 * Gravidade da alergia.
 *
 * So se aplica a {@link PetHealthConditionKind#ALERGIA} - condicao cronica nao se mede
 * assim -, e o banco recusa gravidade em condicao cronica por CHECK.
 *
 * Tres niveis, e nao uma escala numerica: quem preenche e o tutor, e "de 1 a 10" produz
 * numero que ninguem sabe interpretar. GRAVE tem significado operacional - e o que uma
 * tela mostra em vermelho antes de um procedimento.
 */
public enum PetHealthConditionSeverity {

    LEVE,

    MODERADA,

    GRAVE

}

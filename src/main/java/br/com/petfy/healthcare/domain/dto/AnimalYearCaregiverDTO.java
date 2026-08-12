package br.com.petfy.healthcare.domain.dto;

import lombok.*;

/**
 * Uma linha de "quem cuidou dele este ano" (Tela 48).
 *
 * <b>A dupla (pessoa, organizacao) e a unidade</b>, porque e assim que o desenho escreve: "Rafaela
 * Lopes, pela Creche Quintal" e "Juliana Dias, co-tutora" sao duas linhas. A mesma pessoa registrando
 * por si e em nome da creche aparece duas vezes — e esta certo: foi ela quem escolheu assinar de cada
 * jeito, e o documento nao deve desfazer essa escolha.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalYearCaregiverDTO {

    /** Nulo quando o registro veio de uma organizacao sem pessoa atras. */
    private String personName;

    /** Nulo quando a pessoa registrou por si. */
    private String organizationName;

    private long records;

}

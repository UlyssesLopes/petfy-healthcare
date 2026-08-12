package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.UUID;

/**
 * Uma linha do resultado da busca (Tela 35).
 *
 * <b>Nada de saude aqui</b>, e nem faria sentido: a busca e uma porta, e o que ela precisa mostrar e
 * o suficiente para a pessoa reconhecer o animal que procura — nome, microchip, e de quem ele e
 * quando nao e dela.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalSearchItemDTO {

    private UUID animalId;

    private String name;

    private String microchipNumber;

    /**
     * "tutor Ricardo Alves". Nulo nos proprios animais — e no animal cuja custodia e de uma
     * organizacao, onde o nome que importa e o dela.
     */
    private String holderName;

}

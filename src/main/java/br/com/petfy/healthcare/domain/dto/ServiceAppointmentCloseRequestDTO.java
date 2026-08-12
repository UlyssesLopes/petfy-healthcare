package br.com.petfy.healthcare.domain.dto;

import lombok.*;

/**
 * "Entregar e avisar Marcelo" (Tela 18).
 *
 * <b>Um campo, e ele e opcional.</b> Um banho que aconteceu sem novidade nao e fato de saude, e
 * obrigar um texto encheria a linha do tempo de "deu banho" — enterrando a vermelhidao na barriga que
 * alguem viu no banho seguinte.
 *
 * <b>E o texto e OBSERVACAO, nao diagnostico.</b> "Descreva o que viu, nao o que acha que e. Isso
 * entra na linha do tempo como observacao sua, e o veterinario decide o resto." E a distincao que o
 * DESIGN chama de "a mais importante do produto", e aqui ela e literal: o petshop nao tem autoridade
 * clinica, e o que ele escreve nunca vira ato clinico sozinho.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAppointmentCloseRequestDTO {

    private String note;

}

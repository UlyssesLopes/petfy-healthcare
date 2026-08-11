package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * O aceite de quem JA tem conta.
 *
 * <b>So o token.</b> Quem aceita e quem esta autenticado, e nao quem o corpo disser — deixar o
 * cliente informar a pessoa seria deixa-lo entrar na equipe em nome de outra. A organizacao
 * tambem nao vem daqui: ela vem do convite, que foi escrito por quem ja e da organizacao.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteAcceptRequestDTO {

    @NotBlank(message = "token e obrigatorio")
    private String token;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicInviteRequestDTO {

    /**
     * Quando informado, so esse email aceita o convite. Vale a pena preencher:
     * um link sem dono encaminhado a terceiros vira porta de entrada.
     */
    @Email(message = "email invalido")
    private String email;

    @Min(value = 1, message = "a validade deve ser de pelo menos 1 dia")
    @Max(value = 30, message = "a validade nao pode passar de 30 dias")
    private Integer expiresInDays;

}

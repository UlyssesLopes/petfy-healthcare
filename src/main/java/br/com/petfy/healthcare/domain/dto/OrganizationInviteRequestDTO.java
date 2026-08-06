package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteRequestDTO {

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

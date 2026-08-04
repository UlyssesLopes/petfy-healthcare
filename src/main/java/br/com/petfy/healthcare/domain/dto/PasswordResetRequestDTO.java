package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Pedido de recuperacao. Publico: quem esqueceu a senha nao tem como se autenticar. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetRequestDTO {

    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    private String email;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * Publico e por e-mail, e nao autenticado: quem perdeu a mensagem de confirmacao
 * pode nem ter conseguido entrar ainda, e pedir de novo nao expoe nada que o
 * cadastro ja nao exponha.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationResendDTO {

    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    private String email;

}

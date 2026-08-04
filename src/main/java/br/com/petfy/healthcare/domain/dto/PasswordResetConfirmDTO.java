package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Conclusao da recuperacao. Nao pede a senha atual, ao contrario da troca
 * comum: quem chega aqui e justamente quem nao a sabe. O que faz o papel de
 * credencial e o token, que so existiu no e-mail.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetConfirmDTO {

    @NotBlank(message = "token e obrigatorio")
    private String token;

    // o BCrypt ignora o que passar de 72 bytes, entao o limite e explicito
    @NotBlank(message = "nova senha e obrigatoria")
    @Size(min = 8, max = 72, message = "nova senha deve ter entre 8 e 72 caracteres")
    private String newPassword;

}

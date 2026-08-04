package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO proprio em vez de reaproveitar o OwnerRequestDTO: aqui os dois campos sao
 * obrigatorios sempre, enquanto o outro e usado tambem no PUT parcial, onde
 * campo ausente significa "preserve o que esta la".
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeRequestDTO {

    @NotBlank(message = "senha atual e obrigatoria")
    private String currentPassword;

    // o BCrypt ignora o que passar de 72 bytes, entao o limite e explicito
    @NotBlank(message = "nova senha e obrigatoria")
    @Size(min = 8, max = 72, message = "nova senha deve ter entre 8 e 72 caracteres")
    private String newPassword;

}

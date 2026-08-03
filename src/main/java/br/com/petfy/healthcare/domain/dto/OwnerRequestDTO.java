package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OwnerRequestDTO {

    @NotBlank(message = "nome e obrigatorio")
    private String name;

    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    private String email;

    // o BCrypt ignora o que passar de 72 bytes, entao o limite e explicito
    @NotBlank(message = "senha e obrigatoria")
    @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
    private String password;

    private String phone;

    private String address;

}

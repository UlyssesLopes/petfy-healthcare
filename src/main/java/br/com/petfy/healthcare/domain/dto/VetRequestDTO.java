package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetRequestDTO {

    @NotBlank(message = "nome e obrigatorio")
    private String name;

    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    private String email;

    @NotBlank(message = "senha e obrigatoria")
    @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
    private String password;

    private String crmv;

    /**
     * Para entrar numa clinica ja cadastrada. Exclusivo com clinic.
     *
     * E convite, e nao clinicId: o id aparece em qualquer listagem publica, entao
     * aceitar o id deixaria qualquer pessoa entrar em qualquer clinica.
     */
    private String inviteToken;

    /** Para cadastrar a clinica junto, sendo o primeiro vet dela. Exclusivo com inviteToken. */
    @Valid
    private ClinicRequestDTO clinic;

}

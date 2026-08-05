package br.com.petfy.healthcare.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    /**
     * Aceite dos termos e da politica de privacidade, obrigatorio no cadastro.
     *
     * A versao nao vem do cliente: quem sabe qual documento esta vigente e o
     * servidor, e aceitar uma versao informada pelo cliente permitiria registrar
     * consentimento com uma politica que nunca foi publicada.
     *
     * {@code @NotNull} junto com {@code @AssertTrue} porque {@code @AssertTrue}
     * sozinho considera nulo valido - campo ausente passaria pela validacao e a conta
     * nasceria sem consentimento, que e a lacuna que isto fecha.
     */
    @NotNull(message = "e obrigatorio aceitar os termos e a politica de privacidade")
    @AssertTrue(message = "e obrigatorio aceitar os termos e a politica de privacidade")
    private Boolean acceptedTerms;

}

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
public class PersonRequestDTO {

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

    /**
     * Numero do registro profissional, opcional.
     *
     * <b>Isto nao e declarar um papel.</b> A pessoa nao diz "sou veterinaria";
     * ela informa uma credencial, que e um fato verificavel sobre ela, e o que
     * ela alcanca decorre disso. E a mesma diferenca entre dizer o que se e e
     * mostrar o que se tem.
     *
     * Informado nao e verificado: nasce com estado INFORMADO, porque nao ha
     * integracao com conselho. O registro carrega essa informacao em vez de
     * fingir garantia que o produto nao tem.
     */
    @Size(max = 32, message = "registro profissional deve ter no maximo 32 caracteres")
    private String crmv;

    /** UF do registro. O CRMV e estadual, e o mesmo numero se repete entre UFs. */
    @Size(min = 2, max = 2, message = "UF do registro deve ter 2 caracteres")
    private String crmvUf;

    /**
     * Convite de uma clinica existente. Continua sendo a unica porta para entrar
     * numa clinica que ja existe: antes bastava saber o organizationId, que aparece em
     * qualquer listagem, e quem entra numa clinica alcanca todos os animais que
     * ela ja foi autorizada a atender.
     */
    private String inviteToken;

    /** Cadastrar uma clinica nova e ser o primeiro membro dela. */
    private OrganizationRequestDTO organization;

}

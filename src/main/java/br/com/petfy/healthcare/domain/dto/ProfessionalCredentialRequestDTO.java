package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * "Registro profissional · Declarar" (Tela 36).
 *
 * <b>So dava para declarar no CADASTRO ate aqui</b>, e a consequencia era absurda na pratica: a
 * veterinaria que criou a conta como tutora — porque descobriu o Petfy pelo proprio cachorro — nao
 * tinha como dizer depois que e veterinaria. Ela criaria uma segunda conta, com outro e-mail, sem
 * nenhum dos animais que ja acompanha. E o mesmo defeito que o aceite de convite de organizacao
 * corrigiu na Tela 16, na outra ponta.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalCredentialRequestDTO {

    @NotBlank(message = "crmv e obrigatorio")
    private String crmv;

    /** A UF do registro: o CRMV e estadual, e o mesmo numero se repete entre estados. */
    @NotBlank(message = "uf e obrigatorio")
    @Size(min = 2, max = 2, message = "uf deve ter 2 letras")
    private String uf;

    /** Opcional, e e o que a busca de encaminhamento (Tela 45) mostra embaixo do nome. */
    private String specialty;

}

package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
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

    /**
     * A funcao que a pessoa tera na organizacao.
     *
     * <b>Ela e escolhida aqui, e nao no cadastro de quem aceita</b> — deixar quem se cadastra
     * escolher a propria funcao seria deixa-lo escolher a propria permissao. Quem convida ja e
     * da organizacao, e e dele a decisao.
     *
     * Opcional: sem ela vale a regra antiga, que deduz a funcao pela credencial informada.
     */
    private MembershipRole role;

}

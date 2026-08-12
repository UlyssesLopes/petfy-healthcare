package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GrantScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * Encaminhar o caso a um especialista (Tela 45).
 *
 * <b>O `reason` é obrigatório aqui, e no {@link GroupApprovalRequestDTO} não é — e a diferença não é
 * inconsistência.</b> Lá quem lê o pedido é outra pessoa do mesmo grupo, que já conhece o gato e a
 * situação; barrar o esquecimento no servidor transformaria um pedido em erro de formulário. Aqui
 * quem lê são duas pessoas que não sabem nada: o tutor decide conceder acesso ao prontuário com base
 * neste texto, e o especialista descobre por ele o que está sendo perguntado. Um encaminhamento sem
 * motivo é a foto de WhatsApp que esta tela existe para acabar.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralRequestDTO {

    @NotNull(message = "toPersonId e obrigatorio")
    private UUID toPersonId;

    @NotBlank(message = "reason e obrigatorio")
    private String reason;

    /**
     * O que vai junto, por escopo.
     *
     * <b>Vazio é recusado, e não tratado como "tudo".</b> É a mesma regra do {@code Grant}: um
     * acesso sem escopo não alcança nada, e aceitar o vazio aqui faria a clínica encaminhar um caso
     * que chega ao especialista sem nada dentro — depois de o tutor ter autorizado.
     */
    @NotEmpty(message = "scopes e obrigatorio")
    private Set<GrantScope> scopes;

    /**
     * Por quantos dias o acesso vale. Nulo usa os 90 que a tela promete.
     *
     * O teto de um ano está no banco também: um encaminhamento não é a concessão permanente do
     * co-tutor, e um prazo de dez anos seria a mesma coisa sem dizer o nome.
     */
    @Min(value = 1, message = "accessDays deve ser maior que zero")
    @Max(value = 365, message = "accessDays nao pode passar de 365")
    private Integer accessDays;

}

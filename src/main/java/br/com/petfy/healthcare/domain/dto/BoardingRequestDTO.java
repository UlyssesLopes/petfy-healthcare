package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * "O Code esta na Creche Quintal, de 08/08 a 15/08" (Tela 47).
 *
 * <b>Hospedagem e custodia temporaria, e nao um vinculo novo.</b> <i>"E a mesma mecanica do lar
 * transitorio do abrigo, aplicada a quem viaja. A creche nao ganha acesso novo: ela ganha
 * responsabilidade, com prazo, e devolve no dia marcado."</i>
 *
 * Por isso este pedido tem exatamente dois campos: para quem, e ate quando.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardingRequestDTO {

    @NotNull(message = "organizationId e obrigatorio")
    private UUID organizationId;

    /**
     * A volta prevista.
     *
     * <b>Obrigatoria, e e o que separa hospedagem de entrega.</b> Uma custodia transitoria sem prazo
     * combinado e o comeco de um animal esquecido numa creche — e o proprio {@code CustodyNature}
     * ja dizia isso antes desta tela existir: "tem prazo combinado, e o fim dele nao dispensa
     * sucessor".
     *
     * <b>Prevista, e nao definitiva:</b> a volta acontece quando alguem registra, e a data serve para
     * a tela dizer "dia 3 de 7" e para a creche saber o que combinou. Ninguem devolve um animal
     * porque o relogio virou.
     */
    @NotNull(message = "expectedReturnOn e obrigatorio")
    private LocalDate expectedReturnOn;

}

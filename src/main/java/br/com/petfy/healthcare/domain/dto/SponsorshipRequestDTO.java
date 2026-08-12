package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * "Apadrinhar o remedio do Teco" (Tela 46).
 *
 * <b>Nao ha campo de pagamento, e nao e omissao.</b> O produto nao move dinheiro em lugar nenhum, e
 * este pedido registra um COMPROMISSO: quem banca, o que banca, quanto por mes. O valor corre fora, e
 * o que o padrinho recebe em troca sao os eventos de custo — <i>"com data, valor e quem comprou"</i>.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipRequestDTO {

    /**
     * O que se banca.
     *
     * Obrigatorio inclusive quando vem de uma linha de custo, e de proposito: o texto e o que o
     * padrinho vai ler na propria lista dele meses depois, e derivar da linha faria a descricao mudar
     * no dia em que o abrigo reescrevesse o lancamento.
     */
    @NotBlank(message = "description e obrigatorio")
    private String description;

    @NotNull(message = "amount e obrigatorio")
    @DecimalMin(value = "0.01", message = "amount deve ser maior que zero")
    private BigDecimal amount;

    /**
     * De qual gasto real este valor saiu. Nulo no "Outro · valor livre".
     *
     * Quando vem preenchido, o servico CONFERE que o gasto e daquele animal — sem isso, um cliente
     * poderia apontar para a linha de custo de outro animal e a lista do abrigo passaria a somar
     * apadrinhamento de um gasto que nao e dele.
     */
    private UUID sourceCostId;

}

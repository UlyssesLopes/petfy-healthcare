package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * "Os proximos 12 meses do Code" (Tela 38).
 *
 * <b>"Isto nao e previsao de gasto: e o que JA ESTA MARCADO no registro dele."</b> Nenhuma linha
 * daqui e estatistica, tendencia ou media de mercado: cada uma sai de um fato escrito — uma data de
 * reforco na carteira, uma mensalidade combinada, uma compra que o tutor disse que dura um mes.
 *
 * <b>O LIMITE DESTA LEITURA E DECLARADO NO DESENHO, e ele e clinico:</b> "ela pode dizer que adiar a
 * vacina custa dias de creche perdidos, porque isso e aritmetica sobre fatos registrados. Nunca vai
 * dizer que tratar a displasia agora sai mais barato que operar depois — isso e prognostico clinico,
 * e o Petfy nao faz prognostico." Nao ha nada neste DTO que dependa de supor o futuro de um corpo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostForecastResponseDTO {

    /** As linhas, do mais urgente ao mais distante. O que ja venceu vem primeiro. */
    private List<CostForecastItemDTO> items;

    /**
     * O total dos proximos 12 meses.
     *
     * <b>Soma so o que tem preco</b>, e {@link #itemsWithoutAmount} diz quantas linhas ficaram de
     * fora. Um total que fingisse cobrir tudo seria um numero autoritario e menor que a verdade — e o
     * tutor planejaria por ele.
     */
    private BigDecimal total;

    /**
     * Quantas linhas entraram sem valor.
     *
     * <b>Viaja para a tela poder dizer</b>, em vez de deixar o total parecer completo. "Se ninguem
     * informou, o evento aparece sem valor, e isso nao e erro" — mas um total silenciosamente
     * parcial e outra coisa.
     */
    private int itemsWithoutAmount;

}

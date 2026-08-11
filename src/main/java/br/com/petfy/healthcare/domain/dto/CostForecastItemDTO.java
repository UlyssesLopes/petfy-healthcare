package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma linha de "o que vem pela frente" (Tela 38).
 *
 * Cada uma sai de um fato ja escrito no registro do animal — nunca de uma media nem de uma
 * tendencia.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostForecastItemDTO {

    /** {@code DOSE_DE_VACINA}, {@code ANTIPARASITARIO}, {@code CRECHE_MENSALIDADE} ou {@code COMPRA_MENSAL}. */
    private String kind;

    private String description;

    /** Em que fatia do "onde foi" isto vai cair quando acontecer. */
    private AnimalCostCategory category;

    /**
     * Quando. Nulo no que se repete todo mes — a mensalidade nao tem uma data, tem todas.
     *
     * O desenho escreve "todo mes" nessas linhas, e nao uma data inventada.
     */
    private LocalDate dueOn;

    /** Verdadeiro quando o prazo ja passou. "Antirrabica · vencida ha 23 dias." */
    private boolean overdue;

    /**
     * Quantas vezes isto acontece nos proximos 12 meses.
     *
     * <b>1 para uma dose com data; 12 para o que se repete todo mes.</b> Sem este campo a
     * mensalidade de R$ 530 entraria no total como R$ 530, e a previsao de um ano diria menos da
     * metade do que o animal vai custar.
     */
    private int timesInTwelveMonths;

    /**
     * Quanto, por vez. <b>Nulo e "ninguem informou ainda"</b>, e nao zero.
     *
     * Numa dose futura o valor vem da ultima dose do MESMO item de catalogo deste animal. Se essa
     * dose nunca teve valor informado, a linha aparece sem preco — e isso nao e erro.
     */
    private BigDecimal amount;

    /**
     * De onde o valor saiu — o "o que gerou esta leitura" do desenho.
     *
     * Nulo quando nao ha valor. Existe porque um numero previsto sem procedencia e um palpite com
     * cara de fato: quem le precisa poder discordar dele.
     */
    private String amountFrom;

}

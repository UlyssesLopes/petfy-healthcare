package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * "Combinado com o tutor" — a caixa da Tela 41.
 *
 * <b>Todo campo e opcional, e isso vem escrito na propria caixa</b>: o titulo dela e "Combinado com
 * o tutor · opcional". A creche que nunca abriu esta caixa tem uma matricula igualmente valida, e
 * um combinado incompleto — mensalidade sem dia de vencimento, por exemplo — e normal, e nao
 * pendencia.
 *
 * <b>Substitui o combinado inteiro, e nao emenda campo a campo.</b> Combinar e um ato unico: quem
 * renegocia diz de novo o que passou a valer, e o que ele nao repetir deixou de valer. Um PATCH
 * campo a campo faria "apagar a mensalidade" ser indistinguivel de "nao falei da mensalidade" — e
 * o valor errado que sobrasse iria para a conta do tutor sem ninguem ter dito nada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentAgreementRequestDTO {

    /** Zero e valido: creche de abrigo com mensalidade zerada existe, e dizer isso e informacao. */
    @DecimalMin(value = "0.0", message = "monthlyFee nao pode ser negativo")
    private BigDecimal monthlyFee;

    /**
     * "Vence todo dia 05".
     *
     * <b>Ate 28, e nao ate 31</b>: dia 30 nao existe em fevereiro, e um vencimento que some uma vez
     * por ano e um vencimento que ninguem consegue explicar ao tutor.
     */
    @Min(value = 1, message = "dueDay tem de estar entre 1 e 28")
    @Max(value = 28, message = "dueDay tem de estar entre 1 e 28")
    private Integer dueDay;

    /** A diaria de quem vem fora dos dias combinados. */
    @DecimalMin(value = "0.0", message = "dailyRate nao pode ser negativo")
    private BigDecimal dailyRate;

    /**
     * Os dias combinados: MONDAY..SUNDAY, o nome do {@code java.time.DayOfWeek}.
     *
     * <b>Nome e nao numero</b>: 1 e segunda na ISO e domingo em metade das bibliotecas de tela, e
     * essa e a classe de erro que aparece uma vez por ano, no dia errado, para um animal so.
     *
     * Vazio ou nulo e "nao se combinou dia" — e a diaria nunca entra sozinha, porque nao ha o que
     * chamar de "fora do combinado".
     */
    private List<String> weekdays;

}

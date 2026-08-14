package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um valor gasto com o animal.
 *
 * <b>Tres campos obrigatorios e nada mais</b>, e isso vem do desenho da Tela 42: "este e o unico
 * formulario de dinheiro em todo o Petfy, e ele cabe em tres toques. Quanto mais campos, menos
 * gente lanca, e menos verdadeiro fica o custo".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalCostRequestDTO {

    @NotBlank(message = "description e obrigatorio")
    @Size(max = 200, message = "description nao pode passar de 200 caracteres")
    private String description;

    /** Zero e valido: consulta de cortesia acontece, e registrar isso e diferente de nao registrar. */
    @NotNull(message = "amount e obrigatorio")
    @DecimalMin(value = "0.0", message = "amount nao pode ser negativo")
    private BigDecimal amount;

    /** Nulo vira COMPRA — o lancamento do tutor, que e o unico manual. */
    private AnimalCostKind kind;

    /**
     * Onde o dinheiro foi (Tela 37).
     *
     * <b>Opcional, e o servidor decide quando o `kind` ja responde:</b> atendimento e SAUDE,
     * mensalidade e diaria sao CRECHE. Quem precisa mandar e a compra do tutor, porque ali racao e
     * remedio saem do mesmo `kind` e vao para fatias diferentes — e so quem tocou no botao sabe
     * qual. Nulo numa compra vira OUTRO, que e o terceiro botao da Tela 42.
     */
    private AnimalCostCategory category;

    /** "Ja foi pago". Nulo e "ninguem disse", e nao "nao foi pago". */
    private Boolean paid;

    /** "Isto se repete" — hoje so a mensalidade da creche, que chega todo mes pelo valor cheio. */
    private CostRecurrence recurrence;

    /**
     * De quantos em quantos meses este gasto volta. Nulo em gasto que nao se repete.
     *
     * <b>"A racao dura dois meses" e o que esta pergunta responde</b>, e o que a caixinha booleana da
     * Tela 42 nao sabia dizer: marcar dizia "todo mes", que e o dobro do que o tutor gasta.
     */
    @jakarta.validation.constraints.Min(value = 1, message = "coversMonths deve ser ao menos 1")
    @jakarta.validation.constraints.Max(value = 12, message = "coversMonths nao pode passar de 12")
    private Integer coversMonths;

    /** Nulo e agora. Quem lanca hoje a nota de ontem informa ontem. */
    private LocalDateTime occurredAt;

    /** De qual atendimento este valor saiu (Tela 40). */
    private UUID sourceHealthRecordId;

    /** De qual matricula (Tela 41). */
    private UUID sourceEnrollmentId;

    /**
     * De qual dose de vacina.
     *
     * <b>E o que da preco a previsao da Tela 38</b>: o reforco do ano que vem sai da dose do mesmo
     * item de catalogo deste animal. Quem informa e a clinica, no momento em que registra a dose.
     */
    private UUID sourceVaccineId;

    /** De qual antiparasitario, pela mesma razao. */
    private UUID sourceAntiparasiticId;

}

package br.com.petfy.healthcare.domain.dto;

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

    /** "Ja foi pago". Nulo e "ninguem disse", e nao "nao foi pago". */
    private Boolean paid;

    /** "Dura cerca de um mes." */
    private CostRecurrence recurrence;

    /** Nulo e agora. Quem lanca hoje a nota de ontem informa ontem. */
    private LocalDateTime occurredAt;

    /** De qual atendimento este valor saiu (Tela 40). */
    private UUID sourceHealthRecordId;

    /** De qual matricula (Tela 41). */
    private UUID sourceEnrollmentId;

}

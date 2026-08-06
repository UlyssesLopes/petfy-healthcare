package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareInstructionFulfillmentRequestDTO {

    /**
     * Quando foi cumprido - e nao quando foi digitado. Ausente significa agora.
     *
     * <b>Nao aceita futuro.</b> "Vou dar o remedio amanha" nao e cumprimento, e
     * intencao: gravar zeraria a pendencia de hoje por uma coisa que nao aconteceu.
     */
    @PastOrPresent
    private LocalDateTime fulfilledAt;

    @Size(max = 500)
    private String note;

}

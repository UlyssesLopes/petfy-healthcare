package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetWeightRequestDTO {

    @NotNull(message = "peso e obrigatorio")
    @Positive(message = "peso deve ser maior que zero")
    private Double weight;

    @NotNull(message = "data da medicao e obrigatoria")
    private LocalDate measuredAt;

    private String note;

}

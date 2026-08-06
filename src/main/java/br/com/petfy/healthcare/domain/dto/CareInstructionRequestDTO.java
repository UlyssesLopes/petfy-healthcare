package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareInstructionRequestDTO {

    @NotBlank
    @Size(max = 500)
    private String description;

    /**
     * De quantos em quantos dias. Positivo: zero significaria pendencia que renasce no
     * mesmo instante em que e cumprida.
     */
    @NotNull
    @Positive
    private Integer intervalDays;

    @NotNull
    private LocalDate startsOn;

    /** Ausente significa tratamento indefinido - cardiopatia, epilepsia. */
    private LocalDate endsOn;

}

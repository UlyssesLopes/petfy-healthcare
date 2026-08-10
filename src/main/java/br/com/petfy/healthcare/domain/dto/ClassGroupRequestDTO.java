package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassGroupRequestDTO {

    @NotBlank(message = "name e obrigatorio")
    @Size(max = 120)
    private String name;

    /** Nulo e "sem limite declarado" — hospedagem costuma nao ter numero fixo. */
    @Positive(message = "capacity precisa ser maior que zero")
    private Integer capacity;
}

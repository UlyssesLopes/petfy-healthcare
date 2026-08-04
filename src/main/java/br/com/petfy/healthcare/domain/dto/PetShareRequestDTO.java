package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetShareRequestDTO {

    /**
     * Validade em dias. O teto existe para que um link nao vire permanente por
     * descuido: o tutor renova quando precisar.
     */
    @Min(value = 1, message = "a validade deve ser de pelo menos 1 dia")
    @Max(value = 365, message = "a validade nao pode passar de 365 dias")
    private Integer expiresInDays;

}

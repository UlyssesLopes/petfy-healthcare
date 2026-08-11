package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import lombok.*;

import java.math.BigDecimal;

/** Uma fatia do "onde foi" (Tela 37). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalCostSliceDTO {

    private AnimalCostCategory category;

    private BigDecimal amount;

}

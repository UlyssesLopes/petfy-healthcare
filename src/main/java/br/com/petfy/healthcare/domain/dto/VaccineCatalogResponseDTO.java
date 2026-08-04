package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.Species;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineCatalogResponseDTO {

    private UUID vaccineCatalogId;

    private String code;

    private String name;

    private Species species;

    private Integer defaultIntervalDays;

    private Integer initialDoseCount;

    private Integer initialDoseIntervalDays;

    private Boolean mandatory;

    private String description;

}

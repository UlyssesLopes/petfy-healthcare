package br.com.petfy.healthcare.domain.dto;

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

    private String species;

    private Integer defaultIntervalDays;

    private String description;

}

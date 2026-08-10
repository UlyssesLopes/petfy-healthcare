package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.UUID;

/** O que a organizacao exige da carteira de quem entra. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRequirementResponseDTO {

    private UUID requirementId;
    private UUID vaccineCatalogId;
    private String vaccineName;
    private String species;
}

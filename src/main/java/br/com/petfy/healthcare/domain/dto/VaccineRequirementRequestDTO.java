package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRequirementRequestDTO {

    @NotNull(message = "vaccineCatalogId e obrigatorio")
    private UUID vaccineCatalogId;
}

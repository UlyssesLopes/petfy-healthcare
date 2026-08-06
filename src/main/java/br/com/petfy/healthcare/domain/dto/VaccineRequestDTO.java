package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRequestDTO {

    @NotNull(message = "animalId e obrigatorio")
    private UUID animalId;

    /**
     * Quando informado, o nome e a data da proxima dose saem do catalogo. O que
     * vier explicito no request continua tendo precedencia.
     */
    private UUID vaccineCatalogId;

    /** Obrigatorio apenas quando nao vem do catalogo - ver validacao no service. */
    private String vaccineName;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    private UUID organizationId;

}

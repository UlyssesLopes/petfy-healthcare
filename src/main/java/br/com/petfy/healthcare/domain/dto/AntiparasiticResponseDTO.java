package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiparasiticResponseDTO {

    private UUID antiparasiticId;

    private UUID animalId;

    private String name;

    private AntiparasiticKind kind;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    private UUID antiparasiticCatalogId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

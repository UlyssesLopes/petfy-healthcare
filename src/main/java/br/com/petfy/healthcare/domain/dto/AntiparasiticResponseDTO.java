package br.com.petfy.healthcare.domain.dto;

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

    private UUID petId;

    private String name;

    private String kind;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    private UUID antiparasiticCatalogId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

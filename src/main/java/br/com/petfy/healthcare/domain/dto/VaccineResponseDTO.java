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
public class VaccineResponseDTO {

    private UUID vaccineId;

    private String vaccineName;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    private UUID animalId;

    private UUID clinicId;

    private UUID vaccineCatalogId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;


}

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
public class AnimalWeightResponseDTO {

    private UUID weightHistoryId;

    private UUID animalId;

    private Double weight;

    private LocalDate measuredAt;

    private String note;

    private LocalDateTime creationDate;

}

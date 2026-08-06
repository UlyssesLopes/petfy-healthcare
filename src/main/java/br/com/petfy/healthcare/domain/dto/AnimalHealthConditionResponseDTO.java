package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalHealthConditionResponseDTO {

    private UUID animalHealthConditionId;

    private UUID animalId;

    private AnimalHealthConditionKind kind;

    private String description;

    private AnimalHealthConditionSeverity severity;

    private String notes;

    private LocalDate since;

    private LocalDate resolvedAt;

    /** Atalho para o cliente destacar sem comparar datas. */
    private boolean ativa;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

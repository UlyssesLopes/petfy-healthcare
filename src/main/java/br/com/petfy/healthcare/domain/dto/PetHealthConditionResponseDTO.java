package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.PetHealthConditionSeverity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetHealthConditionResponseDTO {

    private UUID petHealthConditionId;

    private UUID petId;

    private PetHealthConditionKind kind;

    private String description;

    private PetHealthConditionSeverity severity;

    private String notes;

    private LocalDate since;

    private LocalDate resolvedAt;

    /** Atalho para o cliente destacar sem comparar datas. */
    private boolean ativa;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareInstructionFulfillmentResponseDTO {

    private UUID careInstructionFulfillmentId;

    private UUID careInstructionId;

    /** Sempre presente: cumprimento sem quem o fez nao serve ao historico de aderencia. */
    private String confirmedByName;

    private LocalDateTime fulfilledAt;

    private LocalDateTime recordedAt;

    private String note;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma entrada do rastro de correcoes de um registro de saude.
 *
 * Traz o que o registro ERA antes daquela alteracao. Comparando com o atual,
 * quem le reconstroi o que mudou - e quem mudou.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordCorrectionResponseDTO {

    private UUID healthRecordCorrectionId;

    private LocalDateTime correctedAt;


    private String correctedByName;

    /** Preenchido apenas quando quem corrigiu foi um veterinario. */
    private String correctedByClinicName;

    private String previousEventType;

    private LocalDate previousEventDate;

    private String previousDescription;

}

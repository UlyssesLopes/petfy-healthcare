package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma entrada do rastro de correcoes.
 *
 * Traz o que a vacina ERA antes daquela alteracao. Comparando com o registro
 * atual, quem le reconstroi o que mudou - e quem mudou.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineCorrectionResponseDTO {

    private UUID vaccineCorrectionId;

    private LocalDateTime correctedAt;


    private String correctedByName;

    /** Nome da organizacao em nome de quem a pessoa agiu. Nulo quando ela agiu por si. */
    private String correctedByOrganizationName;

    private String previousVaccineName;

    private LocalDate previousApplicationDate;

    private LocalDate previousNextDoseDate;

    private String previousDescription;

}

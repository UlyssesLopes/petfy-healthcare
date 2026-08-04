package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Carrega o nome do pet junto porque a agenda cruza todos os pets do tutor: sem
 * isso o cliente precisaria de uma chamada por pet so para montar a tela.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineAgendaItemDTO {

    private UUID vaccineId;

    private String vaccineName;

    private UUID petId;

    private String petName;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private VaccineStatus status;

    /** Negativo quando ja venceu. Nulo quando nao ha proxima dose. */
    private Long daysUntilNextDose;

}

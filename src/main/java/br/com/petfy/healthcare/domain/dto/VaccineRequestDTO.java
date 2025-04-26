package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineRequestDTO {

    private UUID petId;

    private String vaccineName;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    private UUID clinicId;

}

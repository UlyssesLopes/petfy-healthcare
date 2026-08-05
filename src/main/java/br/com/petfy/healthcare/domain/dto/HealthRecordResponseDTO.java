package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordResponseDTO {

    private UUID healthRecordId;

    private HealthEventCategory category;

    private String diagnosis;

    private String eventType;

    private LocalDate eventDate;

    private String description;

    private UUID petId;

    private UUID clinicId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

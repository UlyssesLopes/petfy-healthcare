package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicAccessResponseDTO {

    private UUID petClinicAccessId;

    private UUID petId;

    private UUID clinicId;

    private String clinicName;

    private LocalDateTime grantedAt;

    private LocalDateTime revokedAt;

    private boolean active;

}

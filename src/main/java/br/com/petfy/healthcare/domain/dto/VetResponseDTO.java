package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetResponseDTO {

    private UUID vetId;

    private String name;

    private String email;

    private String crmv;

    private UUID clinicId;

    private String clinicName;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

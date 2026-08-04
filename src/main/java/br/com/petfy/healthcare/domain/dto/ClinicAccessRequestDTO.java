package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicAccessRequestDTO {

    @NotNull(message = "clinicId e obrigatorio")
    private UUID clinicId;

}

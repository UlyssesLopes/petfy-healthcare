package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordRequestDTO {

    @NotNull(message = "petId e obrigatorio")
    private UUID petId;

    @NotBlank(message = "tipo do evento e obrigatorio")
    private String eventType;

    private LocalDate eventDate;

    private String description;

    private UUID clinicId;

}

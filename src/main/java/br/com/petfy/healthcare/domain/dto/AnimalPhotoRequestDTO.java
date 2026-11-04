package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/** Qual anexo do animal passa a ser a foto dele. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalPhotoRequestDTO {

    @NotNull(message = "attachmentId e obrigatorio")
    private UUID attachmentId;

}

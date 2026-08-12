package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** "Agendar banho" (Tela 18). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAppointmentRequestDTO {

    @NotNull(message = "animalId e obrigatorio")
    private UUID animalId;

    @NotNull(message = "scheduledAt e obrigatorio")
    private LocalDateTime scheduledAt;

    /** "Banho e tosa higienica". Texto livre: cada petshop escreve como fala com o cliente. */
    @NotBlank(message = "service e obrigatorio")
    private String service;

}

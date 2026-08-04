package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O pet como o veterinario o enxerga.
 *
 * Traz o nome do tutor para o atendimento saber com quem esta falando, mas nao
 * os dados de contato: a clinica foi autorizada a atender o pet, nao a receber a
 * agenda de contatos do tutor.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetPetDTO {

    private UUID petId;

    private String name;

    private String type;

    private String breed;

    private LocalDate bornDate;

    private String gender;

    private Double weight;

    private String ownerName;

    private LocalDateTime accessGrantedAt;

}

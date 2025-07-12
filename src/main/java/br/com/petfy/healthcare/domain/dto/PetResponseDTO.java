package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponseDTO {

    private UUID petId;

    private String generalRegistry;

    private String name;

    private String type;

    private String breed;

    private String color;

    private Boolean microchip;

    private LocalDate bornDate;

    private String bornLocal;

    private Double weight;

    private String gender;

    private UUID ownerId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetRequestDTO {

    private String name;

    private String type;

    private String breed;

    private LocalDate bornDate;

    private Double weight;

    private String gender;

    private UUID ownerId;

}

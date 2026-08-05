package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.Species;
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

    /** O numero, e nao apenas se tem: e o identificador legal do animal. */
    private String microchipNumber;

    private Boolean castrated;

    private LocalDate castratedAt;

    private LocalDate bornDate;

    private String bornLocal;

    private Double weight;

    private String gender;

    private Species species;

    private UUID ownerId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

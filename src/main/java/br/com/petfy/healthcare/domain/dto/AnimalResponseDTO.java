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
public class AnimalResponseDTO {

    private UUID animalId;

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

    private UUID personId;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    /**
     * Quando a linha do tempo fechou. Nulo no animal vivo, que e o caso de quase toda ficha.
     *
     * <b>Esta aqui, e nao num endpoint separado, porque a tela precisa saber disso em toda ficha
     * que abre</b> — inclusive nas listas. O desenho e explicito em que a ficha nao muda de
     * aparencia: "sem tarja preta, sem laco, sem memorial. A ficha fica igual as outras — so
     * parou de pedir coisas". Um campo a mais e exatamente o tamanho dessa diferenca.
     */
    private LocalDate deceasedOn;

}

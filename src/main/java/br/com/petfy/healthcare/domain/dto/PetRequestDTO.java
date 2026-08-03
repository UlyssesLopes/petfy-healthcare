package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 *
 * Nao ha ownerId: o dono do pet e sempre o autenticado na requisicao. Aceitar o
 * campo do cliente deixaria criar pet no nome de outra pessoa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetRequestDTO {

    @NotBlank(message = "nome e obrigatorio")
    private String name;

    private String type;

    private String breed;

    private LocalDate bornDate;

    @Positive(message = "peso deve ser maior que zero")
    private Double weight;

    private String gender;

}

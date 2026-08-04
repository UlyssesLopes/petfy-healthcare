package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.Species;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    /** Sub-classificacao livre (raca, cor, etc). Nao substitui species. */
    private String type;

    private String breed;

    private LocalDate bornDate;

    @Positive(message = "peso deve ser maior que zero")
    private Double weight;

    private String gender;

    /**
     * Especie do pet - obrigatorio no cadastro. Sem isso o catalogo de vacinas
     * nao filtra corretamente e o protocolo de filhote nao roda.
     */
    @NotNull(message = "species e obrigatoria")
    private Species species;

}

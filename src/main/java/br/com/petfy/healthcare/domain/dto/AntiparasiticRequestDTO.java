package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Usado tanto na criacao quanto na atualizacao (PUT parcial).
 * As restricoes so valem onde o controller marca @Valid - apenas no POST.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiparasiticRequestDTO {

    @NotNull(message = "petId e obrigatorio")
    private UUID petId;

    /**
     * Quando informado, o nome, o kind e a data da proxima dose saem do catalogo.
     * O que vier explicito no request tem precedencia.
     */
    private UUID antiparasiticCatalogId;

    /** Obrigatorio apenas quando nao vem do catalogo. */
    private String name;

    /**
     * DEWORMER (vermifugo) ou FLEA_TICK (antipulgas/carrapatos).
     * Obrigatorio apenas quando nao vem do catalogo. Valor fora do enum e
     * recusado pelo proprio desserializador, com 400.
     */
    private AntiparasiticKind kind;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

}

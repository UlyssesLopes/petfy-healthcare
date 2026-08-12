package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/** Um avistamento registrado (Tela 43). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalSightingResponseDTO {

    private UUID animalSightingId;

    private UUID animalId;

    private LocalDate seenOn;

    /** Quem viu — o "por Sandra" da celula. */
    private String recordedByName;

    /**
     * Quantos dias desde este avistamento.
     *
     * <b>Calculado pelo servidor, e nao pelo cliente.</b> "Sumidos ha mais de 15 dias" e um filtro
     * do servidor, e o numero na tela precisa ser o mesmo que o filtro usou — dois relogios
     * fariam o gato aparecer na lista de sumidos dizendo "visto ha 14 dias".
     */
    private long daysSince;

}

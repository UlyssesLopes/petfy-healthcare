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
public class CareInstructionResponseDTO {

    private UUID careInstructionId;

    private UUID animalId;

    private String description;

    private Integer intervalDays;

    private LocalDate startsOn;

    /** Nulo em tratamento indefinido. */
    private LocalDate endsOn;

    private LocalDateTime revokedAt;

    /** Se vale hoje. Atalho para o cliente nao recalcular vigencia. */
    private boolean vigente;

    /**
     * Quem emitiu, e por qual organizacao.
     *
     * <b>Nome, e nao id.</b> Quem le o prontuario quer saber quem mandou dar o remedio,
     * e nao ter de fazer outra chamada para descobrir. Nulos nos registros anteriores ao
     * nucleo de evento, e no que foi registrado pelo proprio tutor sem organizacao.
     */
    private String recordedByName;

    private String organizationName;

    /**
     * O ultimo cumprimento, quando houve.
     *
     * Vem junto porque a pergunta seguinte a "o que devo fazer" e sempre "alguem ja
     * fez?" - e dois tutores dando o mesmo remedio e dano, nao incomodo.
     */
    private LocalDateTime lastFulfilledAt;

    private String lastFulfilledByName;

    private LocalDateTime creationDate;

}

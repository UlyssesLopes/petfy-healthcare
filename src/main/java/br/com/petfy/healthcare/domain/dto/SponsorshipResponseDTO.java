package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.SponsorshipStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um apadrinhamento, do lado de quem le (Tela 46).
 *
 * <b>Dois leitores, e o do abrigo ve um nome a mais.</b> O padrinho le a propria lista e sabe quem ele
 * e; o abrigo precisa saber quem banca cada custo, senao a lista dele e uma coluna de valores sem
 * ninguem atras.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipResponseDTO {

    private UUID sponsorshipId;

    private UUID animalId;

    private String animalName;

    private String organizationName;

    /** Nulo na lista do proprio padrinho: ele nao precisa que o produto lhe diga o nome dele. */
    private String sponsorName;

    private String description;

    private BigDecimal amount;

    private SponsorshipStatus status;

    private LocalDate startedOn;

    private LocalDateTime cancelRequestedAt;

    /**
     * Quando para. Nulo enquanto ninguem pediu.
     *
     * <b>E o campo que a tela do abrigo mais usa</b>: e por ele que ele sabe, com trinta dias de
     * antecedencia, qual custo vai deixar de ser coberto.
     */
    private LocalDate endsOn;

}

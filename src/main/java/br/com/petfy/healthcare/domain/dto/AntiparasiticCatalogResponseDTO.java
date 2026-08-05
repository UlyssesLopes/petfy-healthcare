package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AntiparasiticKind;
import br.com.petfy.healthcare.domain.entity.Species;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiparasiticCatalogResponseDTO {

    private UUID antiparasiticCatalogId;

    private String code;

    private String name;

    private AntiparasiticKind kind;

    private Species species;

    private Integer defaultIntervalDays;

    private String description;

}

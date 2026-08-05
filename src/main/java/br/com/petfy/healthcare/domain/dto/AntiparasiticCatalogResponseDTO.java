package br.com.petfy.healthcare.domain.dto;

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

    private String kind;

    private String species;

    private Integer defaultIntervalDays;

    private String description;

}

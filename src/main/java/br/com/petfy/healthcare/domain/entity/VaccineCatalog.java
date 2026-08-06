package br.com.petfy.healthcare.domain.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Vacina conhecida, com o intervalo de reforco e o protocolo inicial usados
 * para calcular as doses. Populado por migration - ver V2__vaccine_catalog.sql
 * e V13__species_and_puppy_protocol.sql.
 */
@Entity
@Table(name = "vaccine_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineCatalog {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID vaccineCatalogId;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    /** Especie a que a vacina se aplica. Cruza com {@link Animal#getSpecies()}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Species species;

    /** Intervalo de reforco em dias. Nulo significa dose unica. */
    private Integer defaultIntervalDays;

    /**
     * Quantas doses do esquema inicial (adulto padrao = 1). O esquema inicial
     * mora aqui em vez de numa tabela de protocolo porque cabe em dois campos
     * simples - promover para tabela so quando um protocolo pedir mais
     * variacao (doses com intervalos diferentes entre si, por exemplo).
     */
    @Column(nullable = false)
    private Integer initialDoseCount;

    /** Intervalo entre doses do esquema inicial. Nulo quando count = 1. */
    private Integer initialDoseIntervalDays;

    /**
     * Entra automaticamente no schedule do filhote quando um animal dessa especie
     * e cadastrado. Nem toda vacina do catalogo e obrigatoria: a V3 e a V4
     * felinas cobrem o mesmo animal, so uma delas vira schedule automatico; a
     * antirrabica sim, obrigatoria por lei.
     */
    @Column(nullable = false)
    private Boolean mandatory;

    private String description;

}

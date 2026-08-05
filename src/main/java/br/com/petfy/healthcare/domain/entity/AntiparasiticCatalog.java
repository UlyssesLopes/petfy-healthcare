package br.com.petfy.healthcare.domain.entity;

import lombok.*;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Produto antiparasitario conhecido, com o intervalo padrao de reaplicacao.
 * Populado por migration - ver V14__antiparasitario_e_peso_historico.sql.
 * Somente leitura pela API.
 */
@Entity
@Table(name = "antiparasitic_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AntiparasiticCatalog {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID antiparasiticCatalogId;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    /** DEWORMER (vermifugo) ou FLEA_TICK (antipulgas/carrapatos). */
    @Column(nullable = false)
    private String kind;

    /** CANINA ou FELINA. Informativo: Pet.type e texto livre. */
    @Column(nullable = false)
    private String species;

    /** Intervalo de reaplicacao em dias. Nulo significa aplicacao unica. */
    private Integer defaultIntervalDays;

    private String description;

}

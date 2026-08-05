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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AntiparasiticKind kind;

    /**
     * Especie a que o produto se destina. Filtra o catalogo devolvido ao tutor,
     * do mesmo jeito que em VaccineCatalog - vermifugo felino nao aparece para
     * quem tem cachorro.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Species species;

    /** Intervalo de reaplicacao em dias. Nulo significa aplicacao unica. */
    private Integer defaultIntervalDays;

    private String description;

}

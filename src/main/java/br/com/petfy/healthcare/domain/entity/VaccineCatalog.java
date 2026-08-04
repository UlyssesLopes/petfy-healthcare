package br.com.petfy.healthcare.domain.entity;

import lombok.*;

import javax.persistence.*;
import java.util.UUID;

/**
 * Vacina conhecida, com o intervalo de reforco usado para calcular a proxima
 * dose. Populado por migration - ver V2__vaccine_catalog.sql.
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

    /** CANINA ou FELINA. Informativo: Pet.type e texto livre, entao nao ha match automatico. */
    @Column(nullable = false)
    private String species;

    /** Intervalo de reforco em dias. Nulo significa dose unica. */
    private Integer defaultIntervalDays;

    private String description;

}

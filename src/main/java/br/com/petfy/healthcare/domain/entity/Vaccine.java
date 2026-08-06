package br.com.petfy.healthcare.domain.entity;


import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccines")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

public class Vaccine extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID vaccineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    private String vaccineName;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    /** Nulo para vacina digitada em texto livre e para os registros anteriores ao catalogo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_catalog_id")
    private VaccineCatalog catalog;

    /** Quando o ultimo lembrete desta dose foi enviado. Nulo se nunca avisamos. */
    private LocalDateTime lastReminderSentAt;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

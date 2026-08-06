package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

public class HealthRecord extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID healthRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    private String description;

    private LocalDate eventDate;

    /**
     * Classificacao do atendimento. Existe ao lado de {@link #eventType}, que continua
     * sendo o rotulo livre - ver {@link HealthEventCategory} para por que nao substitui.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private HealthEventCategory category;

    /** Diagnostico como campo proprio, e nao enterrado na descricao. */
    @Column(length = 500)
    private String diagnosis;

    private String eventType;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado anterior de um registro de saude, gravado a cada correcao.
 *
 * Guarda o que era, e nao o que passou a ser: o estado atual esta no proprio
 * registro, entao reanimalir seria redundancia que pode divergir.
 */
@Entity
@Table(name = "health_record_corrections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordCorrection {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID healthRecordCorrectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id", nullable = false)
    private HealthRecord healthRecord;

    /** Preenchido quando quem corrigiu foi um veterinario. Exclusivo com o outro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_vet_id")
    private Vet correctedByVet;

    /** Preenchido quando quem corrigiu foi o tutor. Exclusivo com o outro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_person_id")
    private Person correctedByPerson;

    private String previousEventType;

    private LocalDate previousEventDate;

    private String previousDescription;

    @Column(nullable = false)
    private LocalDateTime correctedAt;

}

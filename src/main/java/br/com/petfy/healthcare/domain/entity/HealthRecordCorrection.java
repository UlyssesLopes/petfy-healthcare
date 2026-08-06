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
 * registro, entao repetir seria redundancia que pode divergir.
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

    /**
     * Quem corrigiu. Sempre preenchido: nao ha mais duas colunas exclusivas entre
     * si, uma para tutor e outra para veterinario, porque nao ha mais dois tipos
     * de conta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_person_id", nullable = false)
    private Person correctedBy;

    /**
     * Em nome de qual organizacao a pessoa agiu, quando agiu por uma. Nulo quando
     * ela agiu por si. Substituiu o papel - ver VaccineCorrection para o porque.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_in_clinic_id")
    private Clinic correctedInClinic;

    private String previousEventType;

    private LocalDate previousEventDate;

    private String previousDescription;

    @Column(nullable = false)
    private LocalDateTime correctedAt;

}

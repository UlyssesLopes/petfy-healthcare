package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado anterior de uma vacina, gravado a cada correcao.
 *
 * Guarda o que era, e nao o que passou a ser: o estado atual esta na propria
 * vacina, entao repetir seria redundancia que pode divergir.
 */
@Entity
@Table(name = "vaccine_corrections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineCorrection {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID vaccineCorrectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    /** Preenchido quando quem corrigiu foi um veterinario. Exclusivo com o outro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_vet_id")
    private Vet correctedByVet;

    /** Preenchido quando quem corrigiu foi o tutor. Exclusivo com o outro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_owner_id")
    private Owner correctedByOwner;

    private String previousVaccineName;

    private LocalDate previousApplicationDate;

    private LocalDate previousNextDoseDate;

    private String previousDescription;

    @Column(nullable = false)
    private LocalDateTime correctedAt;

}

package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medicao de peso do pet em uma data especifica.
 *
 * Substitui o Double unico em Pet.weight por uma serie historica, permitindo
 * curva de crescimento do filhote. Pet.weight permanece como espelho da ultima
 * medicao para leitura rapida - ver PetWeightService.
 */
@Entity
@Table(name = "pet_weight_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetWeightHistory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID weightHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private LocalDate measuredAt;

    private String note;

    @Column(nullable = false)
    private LocalDateTime creationDate;

}

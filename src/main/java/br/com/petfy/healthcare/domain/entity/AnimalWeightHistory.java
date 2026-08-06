package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Medicao de peso do animal em uma data especifica.
 *
 * Substitui o Double unico em Animal.weight por uma serie historica, permitindo
 * curva de crescimento do filhote. Animal.weight permanece como espelho da ultima
 * medicao para leitura rapida - ver AnimalWeightService.
 */
@Entity
@Table(name = "animal_weight_history")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor

public class AnimalWeightHistory extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID weightHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private LocalDate measuredAt;

    private String note;

    @Column(nullable = false)
    private LocalDateTime creationDate;

}

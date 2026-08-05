package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alergia ou condicao cronica do pet.
 *
 * <b>Encerrada em vez de apagada:</b> condicao que passou faz parte do historico do
 * animal, e apagar a linha esconderia que ela existiu. {@code resolvedAt} nula significa
 * ativa - e sao as ativas que uma tela mostra em destaque.
 */
@Entity
@Table(name = "pet_health_conditions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetHealthCondition {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID petHealthConditionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PetHealthConditionKind kind;

    @Column(nullable = false)
    private String description;

    /** So para alergia. O banco recusa gravidade em condicao cronica, por CHECK. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PetHealthConditionSeverity severity;

    @Column(length = 500)
    private String notes;

    private LocalDate since;

    /** Nula enquanto a condicao vale. Preenchida encerra sem apagar o registro. */
    @Column(name = "resolved_at")
    private LocalDate resolvedAt;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    public boolean isAtiva() {
        return resolvedAt == null;
    }

}

package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vinculo entre um pet e quem cuida dele.
 *
 * Substitui o {@code pets.owner_id} que existia ate a V15. A coluna nao foi
 * mantida junto com esta tabela de proposito: com as duas, "quem e o dono" teria
 * duas respostas possiveis, e o dia em que elas divergissem seria um vazamento -
 * alguem enxergando pet que nao e seu, ou deixando de enxergar o proprio.
 *
 * Por isso o titular tambem mora aqui, com {@link PetTutorRole#HOLDER}, e nao em
 * campo separado.
 */
@Entity
@Table(name = "pet_tutors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutor {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID petTutorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PetTutorRole role;

    /**
     * Nulo para os vinculos criados pela migration, que nao tem convite atras -
     * eram o dono unico de antes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_owner_id")
    private Owner invitedBy;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    public boolean isHolder() {
        return role != null && role.isHolder();
    }

    public boolean podeEditar() {
        return role != null && role.podeEditar();
    }

}

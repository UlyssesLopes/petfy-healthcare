package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clinic_invites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicInvite {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID clinicInviteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_vet_id", nullable = false)
    private Vet createdBy;

    /** Hash do token. O token em si nao e guardado - ver V7__clinic_invite.sql. */
    @Column(unique = true, nullable = false)
    private String tokenHash;

    /**
     * Quando preenchido, so esse email pode aceitar. Protege o caso do convite
     * ser encaminhado a outra pessoa.
     */
    private String email;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_vet_id")
    private Vet acceptedBy;

    private LocalDateTime revokedAt;

    private LocalDateTime creationDate;

    /** Convite e de uso unico: aceitar consome. */
    public boolean isUsable(LocalDateTime agora) {
        return revokedAt == null && acceptedAt == null && expiresAt.isAfter(agora);
    }

}

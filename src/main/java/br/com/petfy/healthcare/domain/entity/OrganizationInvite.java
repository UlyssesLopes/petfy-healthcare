package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organization_invites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvite {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID organizationInviteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_vet_id", nullable = false)
    private Person createdBy;

    /** Hash do token. O token em si nao e guardado - ver V7__organization_invite.sql. */
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
    private Person acceptedBy;

    private LocalDateTime revokedAt;

    /**
     * A funcao que a pessoa tera ao aceitar, escolhida por quem convida.
     *
     * <b>Nulo e legitimo</b>: convite emitido antes da V34 nao declara funcao, e para ele vale a
     * regra antiga (credencial informada entra como VETERINARIO, o resto como ADMINISTRADOR).
     */
    @Enumerated(EnumType.STRING)
    private MembershipRole role;

    private LocalDateTime creationDate;

    /** Convite e de uso unico: aceitar consome. */
    public boolean isUsable(LocalDateTime agora) {
        return revokedAt == null && acceptedAt == null && expiresAt.isAfter(agora);
    }

}

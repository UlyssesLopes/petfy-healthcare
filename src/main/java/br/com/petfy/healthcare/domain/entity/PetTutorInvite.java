package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite para entrar num animal, emitido pelo titular.
 *
 * Mesmo desenho do {@link OrganizationInvite}: token guardado como hash, uso unico,
 * expiracao curta e e-mail travando o destinatario. Existe convite em vez de
 * vinculo direto porque o caso comum e o conjuge que **ainda nao tem conta** -
 * exigir cadastro previo mataria o fluxo justamente onde ele comeca. E vincular
 * alguem sem que aceite faria a pessoa passar a receber lembrete que nao pediu.
 *
 * O mesmo convite serve para transferir a titularidade: muda o papel oferecido,
 * nao o mecanismo - ver {@link #transfereTitularidade()}.
 */
@Entity
@Table(name = "pet_tutor_invites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorInvite {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID petTutorInviteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_owner_id", nullable = false)
    private Person createdBy;

    /** Hash do token. O token em si nao e guardado - ver V15. */
    @Column(unique = true, nullable = false)
    private String tokenHash;

    /**
     * So este e-mail pode aceitar. **Obrigatorio aqui**, ao contrario do convite
     * de clinica, onde e opcional: um convite de animal da acesso ao historico de
     * saude de um animal e ao nome do tutor, entao um link solto encaminhado por
     * engano entrega dado pessoal a quem passar por ele.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Papel oferecido. {@link PetTutorRole#HOLDER} significa transferencia de
     * titularidade: aceitar promove quem recebeu e rebaixa quem enviou.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PetTutorRole role;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by_owner_id")
    private Person acceptedBy;

    private LocalDateTime revokedAt;

    /**
     * Quando quem recebeu disse nao (Telas 19, 20 e 21).
     *
     * <b>Coluna propria, e nao o {@code revokedAt}.</b> Revogar e o que QUEM CONVIDOU faz ao mudar de
     * ideia; recusar e o que QUEM RECEBEU faz. Com um campo so, a lista de convites do tutor diria
     * "voce revogou" sobre um convite que a outra pessoa recusou — e ele procuraria no proprio
     * historico uma acao que nunca praticou.
     */
    private LocalDateTime rejectedAt;

    private LocalDateTime creationDate;

    /** Uso unico: aceitar consome, recusar consome, revogar consome. */
    public boolean isUsable(LocalDateTime agora) {
        return revokedAt == null && acceptedAt == null && rejectedAt == null
                && expiresAt.isAfter(agora);
    }

    public boolean transfereTitularidade() {
        return role == PetTutorRole.HOLDER;
    }

}

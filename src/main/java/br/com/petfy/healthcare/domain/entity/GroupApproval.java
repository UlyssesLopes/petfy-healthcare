package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O acordo de duas pessoas, num grupo onde ninguem e dono (Telas 43 e 44).
 *
 * <b>"Sem dono, a protecao contra o gesto irreversivel de uma pessoa so e o acordo de duas."</b>
 *
 * Num animal com tutor, o irreversivel e barrado por quem responde: o {@code requireCustodia}
 * pergunta "e voce?" e acabou. Na colonia essa pergunta nao tem resposta util — seis pessoas
 * respondem juntas, e todas passam no {@code requireCustodia} pela organizacao. Sem esta
 * entidade, UMA pessoa daria um gato para adocao ou encerraria uma linha do tempo sozinha.
 *
 * <b>Nao e o {@link AnimalMergeRequest} com outro nome.</b> Os dois sao "pedido que espera
 * decisao", e param ai. No merge, quem decide e determinado — quem RESPONDE pelo animal. Aqui
 * quem decide e indeterminado de proposito: <b>qualquer outra pessoa do grupo serve</b>, e essa
 * indeterminacao e o mecanismo. Apontar para uma pessoa fixa transformaria isto em "peca a
 * Marta", e o gesto pararia no dia em que a Marta viajasse.
 */
@Entity
@Table(name = "group_approvals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupApproval {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "group_approval_id", updatable = false, nullable = false)
    private UUID groupApprovalId;

    /** O grupo em cujo nome o ato aconteceria. E ele que define quem pode concordar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private GroupApprovalKind kind;

    /** Sobre qual animal. Nulo em {@code REMOCAO_DE_MEMBRO}, que fala de uma pessoa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private Animal animal;

    /** Sobre qual pessoa. Nulo nos dois atos que falam de animal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id")
    private Person targetPerson;

    /** Para quem o animal iria, na adocao. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_person_id")
    private Person toPerson;

    /**
     * O porque, escrito por quem pediu.
     *
     * Quem concorda le isto e nada mais. Um pedido que so diz "concorde" nao da a segunda pessoa
     * nada com que decidir — e ela concordaria por confianca, que e o oposto do que a regra quer.
     */
    @Column(columnDefinition = "text")
    private String reason;

    /**
     * A data do óbito, só em {@code OBITO}.
     *
     * Vem no pedido e não na concordância: quem encontrou o gato morto sabe quando foi, e quem
     * concorda três dias depois não sabe.
     */
    @Column(name = "deceased_on")
    private java.time.LocalDate deceasedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupApprovalStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_person_id", nullable = false)
    private Person requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_person_id")
    private Person decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public boolean estaPendente() {
        return status == GroupApprovalStatus.PENDENTE;
    }

}

package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma pessoa dentro de uma organizacao, com funcao.
 *
 * <b>Matou o {@code Vet.clinic} singular</b>, que era o mesmo campo desde antes de
 * existir Person: um {@code @ManyToOne} obrigatorio, que fazia o veterinario em duas
 * clinicas precisar de duas contas - com dois e-mails, o que o namespace unico
 * impedia. Era a divida registrada em <i>Ainda nao levantado com voce</i>.
 *
 * <b>Uma pessoa tem N vinculos.</b> Resolve de uma vez o vet em duas clinicas, o vet
 * voluntario de abrigo e o dono de creche que tambem e tutor - e nenhum dos tres e
 * caso raro: e o caso comum de quem trabalha com animal.
 *
 * <b>E organizacao continua sendo opcional para atuar.</b> Zero vinculos e um estado
 * legitimo, e nao um cadastro incompleto: o veterinario autonomo e uma pessoa com
 * credencial que recebe acesso direto de quem tem custodia. Quem atende e o
 * profissional; a organizacao e onde ele atende.
 *
 * <b>Vinculo encerrado nao sai da tabela.</b> Ter trabalhado numa clinica por tres
 * anos e biografia, e o ato clinico que a pessoa assinou por ela continua apontando
 * para aquele contexto - apagar o vinculo deixaria o registro sem explicar em nome de
 * quem foi assinado.
 */
@Entity
@Table(name = "memberships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "membership_id", updatable = false, nullable = false)
    private UUID membershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MembershipRole role;

    /**
     * O que esta pessoa faz no grupo: "alimenta de manha", "leva ao veterinario" (Tela 43).
     *
     * <b>Texto livre, e nao um valor novo no {@link MembershipRole}.</b> O enum diz o que a
     * pessoa PODE fazer, e por isso governa permissao; isto diz o que ela FAZ. "De manha" contra
     * "a noite" e escala, e nao papel — os dois cairiam no mesmo valor de enum, e a tela perderia
     * exatamente a informacao que mostra.
     *
     * Nulo e o normal: a equipe de uma clinica nao declara turno, e a tela dela nunca pediu isso.
     */
    @Column(length = 80)
    private String contribution;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /** Nulo enquanto a pessoa esta na organizacao. */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public boolean estaAtivo() {
        return leftAt == null;
    }

}

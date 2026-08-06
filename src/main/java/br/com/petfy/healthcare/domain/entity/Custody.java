package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quem responde por um animal, desde quando, ate quando, e por que.
 *
 * Generaliza tutor, lar transitorio e abrigo. <b>E o outro lado do que o PetTutor
 * fazia:</b> ele decidia custodia e acesso com o mesmo campo, e as duas coisas sao
 * opostas - a clinica le e escreve por concessao e nunca responde pelo animal; o
 * tutor responde e nao precisa de concessao de ninguem.
 *
 * <b>O quarto invariante do produto vive aqui:</b> nenhuma custodia termina sem
 * sucessor. Ninguem "solta" um animal - repasse e a criacao do vinculo seguinte,
 * nao o encerramento do anterior. A unica excecao e o motivo terminal (obito,
 * perda), onde nao ha mais o que suceder. A regra e {@code CHECK} no banco alem de
 * validacao no servico, pelo mesmo par de razoes da gravidade de alergia: o banco
 * impede insert direto, o servico impede que o cliente receba erro de integridade
 * como 500.
 */
@Entity
@Table(name = "custodies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Custody {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "custody_id", updatable = false, nullable = false)
    private UUID custodyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /** Quem responde, quando e uma pessoa. Exclusivo com {@link #holderOrganization}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_person_id")
    private Person holderPerson;

    /**
     * Quem responde, quando e uma organizacao - abrigo ou ONG.
     *
     * Aponta para {@code Organization} desde o P3. O
     * slot ja existia no P2b para o guard nao precisar ser reescrito duas vezes: e
     * ele que decide quem alcanca o animal, e e a peca que menos deve mudar por
     * mes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_organization_id")
    private Organization holderOrganization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CustodyNature nature;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Fim combinado. Nulo em custodia sem prazo, que e o caso do tutor comum. */
    @Column(name = "expected_end_at")
    private LocalDateTime expectedEndAt;

    /** Fim real. Nulo enquanto a custodia esta em curso. */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 24)
    private CustodyEndReason endReason;

    /**
     * A custodia que assumiu.
     *
     * Auto-referencia porque o sucessor de uma custodia e outra custodia, e nao uma
     * pessoa: e o que permite ler a linha de quem respondeu pelo animal sem
     * remontar nada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_custody_id")
    private Custody successor;

    public boolean estaEmCurso() {
        return endedAt == null;
    }

    /** Quem responde, seja pessoa ou organizacao. */
    public boolean respondePor(UUID personId) {
        return holderPerson != null && holderPerson.getPersonId().equals(personId);
    }

}

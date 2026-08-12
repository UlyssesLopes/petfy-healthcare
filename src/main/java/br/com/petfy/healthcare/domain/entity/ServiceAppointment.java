package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um banho marcado (Tela 18).
 *
 * <b>"O petshop e AGENDA, nao diaria de cuidado."</b> E por isso isto nao e uma {@link Enrollment}: a
 * creche tem vaga numa turma, dias combinados e mensalidade, porque e um vinculo continuo. O petshop
 * nao tem vaga a ocupar nem turma, e o animal volta quando o pelo cresce. <b>A unidade aqui e o
 * compromisso de um dia e uma hora</b>, e ela nao existia no produto.
 *
 * <b>E nao e um {@link AnimalCost}</b>, porque o agendamento acontece ANTES de qualquer valor — e a
 * maior parte dele nunca vira valor nenhum. Um custo com data futura afirmaria um gasto que ainda nao
 * houve.
 */
@Entity
@Table(name = "service_appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAppointment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "service_appointment_id", updatable = false, nullable = false)
    private UUID serviceAppointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /**
     * Dia E hora.
     *
     * A presenca da creche guarda apenas o dia, porque a pergunta la e "ele veio hoje?". Aqui a agenda
     * do dia e uma coluna de horarios, e guardar so o dia obrigaria o petshop a ordenar oito banhos
     * por ordem de digitacao.
     */
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /**
     * "Banho e tosa higienica", "tosa na tesoura".
     *
     * <b>Texto livre, e nao catalogo.</b> Um enum de servicos seria o comeco de um sistema de gestao de
     * petshop — e este produto e o registro da vida do animal, nao o PDV de quem o atende.
     */
    @Column(nullable = false, length = 120)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ServiceAppointmentStatus status;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Quem marcou o compromisso — e nao quem atendeu: o atendimento e assinado pela observacao. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_person_id", nullable = false)
    private Person createdBy;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

}

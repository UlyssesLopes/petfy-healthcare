package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A matricula do animal numa turma.
 *
 * <b>Ela existe antes de valer.</b> A Tela 10 desenha exatamente esse estado: "matricula pendente
 * — falta a antirrabica em dia. A matricula fica guardada e se completa sozinha assim que a dose
 * for registrada". Guardar em vez de recusar e o que evita a creche refazer o cadastro depois, e e
 * o que faz o produto trabalhar para o tutor em vez de cobrar dele.
 *
 * <b>Nao ha campo de aptidao aqui, e a ausencia e a regra.</b> Estar apto e o resultado de comparar
 * o que a organizacao exige com o que a carteira tem, e isso muda sem ninguem tocar na matricula —
 * uma dose vence sozinha, e outra e registrada por uma clinica do outro lado da cidade. Guardar o
 * resultado criaria uma verdade que envelhece em silencio.
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue
    @Column(name = "enrollment_id", updatable = false, nullable = false)
    private UUID enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** Matricula e ato de alguem, e nao estado que aparece (nucleo de evento do P4). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_person_id")
    private Person createdBy;

    public boolean estaViva() {
        return endedAt == null;
    }
}

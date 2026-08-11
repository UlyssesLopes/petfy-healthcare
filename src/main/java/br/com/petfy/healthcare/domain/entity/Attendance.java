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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O dia de um animal na creche: esperado, chegou, saiu, faltou.
 *
 * <b>E separada da matricula porque o animal matriculado falta.</b> Juntar as duas faria
 * "matriculado" e "esta aqui hoje" virarem a mesma coisa — que e justamente o que a Tela 17 existe
 * para nao deixar acontecer: as 7h34 de segunda ela precisa dizer "14 esperados · 6 ja chegaram".
 *
 * <b>A chave natural e o DIA, e nao o instante.</b> A operacao pergunta "quem vem hoje", e os
 * horarios sao dados dentro do registro do dia. O indice unico por matricula e dia e o que faz o
 * segundo clique em "marcar entrada" — o gesto mais provavel numa manha de creche — nao virar um
 * segundo registro.
 */
@Entity
@Table(name = "attendances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue
    @Column(name = "attendance_id", updatable = false, nullable = false)
    private UUID attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    /**
     * "Sai as 15h, com a avo" — quem busca, quando nao e quem costuma buscar.
     *
     * Texto livre de proposito: a avo nao tem conta no Petfy e nao deveria precisar de uma para
     * buscar o cachorro do neto.
     */
    @Column(name = "pickup_note", length = 200)
    private String pickupNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_person_id")
    private Person recordedBy;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
}

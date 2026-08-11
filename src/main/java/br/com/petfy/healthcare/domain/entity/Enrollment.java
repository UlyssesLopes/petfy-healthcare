package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
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

    /* ------------------------------------------------- o combinado com o tutor (Tela 41) */

    /**
     * A mensalidade.
     *
     * <b>Mora na matricula, e nao numa tabela propria</b>, porque e propriedade do combinado
     * daquele animal naquela turma: mudar de turma e recombinar, e a mensalidade antiga fica com a
     * matricula antiga, onde ela conta a verdade sobre o periodo em que valeu.
     *
     * "O Petfy nao cobra, nao emite boleto e nao processa pagamento. Ele guarda o que foi
     * combinado, para que o tutor veja o custo real do Code e ninguem precise perguntar por
     * telefone."
     */
    @Column(name = "monthly_fee", precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    /** "Vence todo dia 05". Nulo quando nao se combinou dia — e comum, e nao pendencia. */
    @Column(name = "due_day")
    private Integer dueDay;

    /** A diaria de quem vem fora dos dias combinados. E ela que entra sozinha no check-in. */
    @Column(name = "daily_rate", precision = 12, scale = 2)
    private BigDecimal dailyRate;

    /**
     * Os dias em que o animal e esperado — "3 dias por semana", que o tutor le sem perguntar.
     *
     * <b>Existe para a diaria avulsa poder entrar sozinha.</b> Sem os dias, "fora do combinado" nao
     * e pergunta que o servidor saiba responder, e a diaria so entraria se alguem digitasse — que e
     * exatamente o que o desenho recusa: "ninguem digitou nada".
     *
     * <b>Vazio e "nao sei", e nunca "nenhum dia".</b> Ver {@link #foraDoCombinado}.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "enrollment_weekdays",
                     joinColumns = @JoinColumn(name = "enrollment_id"))
    @Column(name = "weekday", nullable = false, length = 12)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DayOfWeek> weekdays = new LinkedHashSet<>();

    public boolean estaViva() {
        return endedAt == null;
    }

    /**
     * O dia esta fora do que se combinou?
     *
     * <b>Conjunto vazio responde `false`, e essa e a linha que impede o produto de cobrar do tutor
     * por um dado que ninguem informou.</b> A creche que nunca abriu a caixa do combinado nao
     * declarou dia nenhum — e ler isso como "todo dia e avulso" faria cada entrada virar diaria.
     * O silencio nao vira cobranca.
     */
    public boolean foraDoCombinado(DayOfWeek dia) {
        return weekdays != null && !weekdays.isEmpty() && !weekdays.contains(dia);
    }
}

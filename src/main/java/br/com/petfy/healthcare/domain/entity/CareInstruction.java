package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Instrucao dada a quem cuida do animal, com prazo e confirmacao de cumprimento.
 *
 * <b>Um conceito, tres usos que estavam separados:</b> prescricao do veterinario,
 * medicacao e tratamento continuo - que estava parado em <i>Fica para depois do
 * frontend</i> - e tema de casa da creche. Os tres tem a mesma forma: alguem manda,
 * alguem cumpre, e o cumprimento precisa ficar registrado.
 *
 * <b>A orientacao segue a CUSTODIA, e nao a pessoa.</b> E por isso que ela aponta para
 * o animal, e nao para quem deve cumpri-la: se o animal volta do lar transitorio para
 * o abrigo no meio de um tratamento de 21 dias, o remedio continua - a instrucao passa
 * para quem assumiu, com o que ja foi cumprido preservado. Amarra-la a uma pessoa
 * faria o tratamento morrer na troca de mao, que e exatamente quando o animal esta
 * mais fragil.
 *
 * <b>Encerra-se, nao se apaga.</b> {@code revokedAt} tira das pendencias sem esconder
 * que existiu - um tratamento suspenso pelo veterinario e informacao clinica, e a
 * proxima pessoa a ler o historico precisa saber que houve.
 */
@Entity
@Table(name = "care_instructions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CareInstruction extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "care_instruction_id", updatable = false, nullable = false)
    private UUID careInstructionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /** O que fazer. Texto, porque prescricao e tema de casa nao cabem em enum. */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * De quantos em quantos dias.
     *
     * Intervalo em dias, e nao expressao de horario: "de 12 em 12 horas" e informacao
     * de <i>execucao</i>, e o formato de quando avisar e o que a tela define melhor que
     * o modelo - esta em <i>Fica para depois do frontend</i> por escolha. O dia basta
     * para saber se a pendencia existe hoje.
     */
    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    /** Nulo em tratamento indefinido - cardiopatia, epilepsia. */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    /** Encerrada antes do prazo. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Quem encerrou.
     *
     * <b>Nao e necessariamente quem emitiu</b>, e isso e deliberado: o veterinario que
     * prescreveu pode nunca mais voltar - o animal trocou de clinica, o profissional saiu
     * -, e exigir a assinatura dele deixaria um tratamento encerrado na vida real cobrando
     * para sempre na lista de pendencias. Pendencia que nao pode ser resolvida treina o
     * usuario a ignorar a lista, que e o caminho mais curto para ela perder valor. Quem
     * tem escrita encerra; quem encerrou fica gravado, e a diferenca entre "o tutor
     * parou" e "o veterinario suspendeu" continua legivel no historico.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_person_id")
    private Person revokedBy;

    private LocalDateTime creationDate;

    /**
     * Se a orientacao esta valendo na data informada.
     *
     * Revogada nunca vale. Sem {@code endsOn}, vale para sempre depois de comecar - e
     * o tratamento continuo, que e justamente a populacao que mais precisa de
     * lembrete.
     */
    public boolean estaVigenteEm(LocalDate dia) {
        return revokedAt == null
                && !dia.isBefore(startsOn)
                && (endsOn == null || !dia.isAfter(endsOn));
    }

}

package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um valor gasto com o animal (Telas 40, 41 e 42).
 *
 * <b>O CAMPO E OPCIONAL, E UM EVENTO SEM VALOR E NORMAL</b> — nunca um erro, nunca um alerta. "Se
 * informar valor virar obrigacao, a clinica para de registrar o atendimento, e ai o produto perde
 * o que importa de verdade."
 *
 * <b>Estende {@link AnimalEvent}</b> porque cada valor tem um evento por tras, com autor e data —
 * "e por isso pode ser CONTESTADO como qualquer outro registro". Um valor sem autor seria um
 * numero que apareceu sozinho na conta do tutor, e ele nao teria a quem perguntar.
 *
 * <b>MAS NAO ENTRA NA LINHA DO TEMPO, e isso e deliberado.</b> "Nenhum escopo de acesso concede
 * preco junto com saude" — o que a clinica cobra do tutor nao e assunto da creche nem do petshop.
 * A linha do tempo e o lugar onde o escopo governa a leitura; o custo tem regra propria, que e
 * CUSTODIA, e por isso ele mora fora dela.
 */
@Entity
@Table(name = "animal_costs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AnimalCost extends AnimalEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "animal_cost_id", updatable = false, nullable = false)
    private UUID animalCostId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /** "O que foi cobrado": consulta dermatologica, mensalidade, racao. */
    @Column(nullable = false, length = 200)
    private String description;

    /** {@code BigDecimal} e nao {@code double}: dinheiro em ponto flutuante erra na terceira soma. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AnimalCostKind kind;

    /**
     * "Ja foi pago."
     *
     * <b>Nulo em quem nao respondeu, e nao {@code false}</b>: o desenho oferece a caixa e nao a
     * obriga, e {@code false} afirmaria "nao foi pago" sobre algo que ninguem disse.
     */
    private Boolean paid;

    /**
     * "Dura cerca de um mes" — a caixinha da Tela 42.
     *
     * <b>E o que transforma uma compra avulsa em custo mensal previsivel</b>, e o que permite ao
     * abrigo dizer ao adotante que a racao custa R$ 190 por mes, todo mes. Sem ela o produto so
     * saberia somar o passado.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CostRecurrence recurrence;

    /** Quando o gasto aconteceu. Quem lanca hoje a nota de ontem lancou ontem. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /**
     * De qual atendimento este valor saiu, quando saiu de um.
     *
     * Nulo na mensalidade da creche e na compra do tutor. Guardado como id e nao como relacao
     * para nao arrastar o prontuario inteiro para uma leitura que e sobre dinheiro.
     */
    @Column(name = "source_health_record_id")
    private UUID sourceHealthRecordId;

    /** De qual matricula, quando e mensalidade ou diaria avulsa. */
    @Column(name = "source_enrollment_id")
    private UUID sourceEnrollmentId;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

}

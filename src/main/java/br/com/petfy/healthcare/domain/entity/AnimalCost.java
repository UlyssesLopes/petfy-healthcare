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
     * Onde o dinheiro foi (Tela 37).
     *
     * <b>Nao e o {@link AnimalCostKind} com outro nome.</b> O `kind` diz de que evento o valor saiu;
     * a categoria diz em que ele foi gasto. As duas divergem no caso que decide o grafico: racao e
     * remedio saem os dois de uma {@code COMPRA}, e o desenho os poe em fatias diferentes.
     *
     * <b>Nunca nula</b>, e o banco tambem recusa: uma linha sem fatia apareceria na Tela 37 como
     * dinheiro que sumiu do grafico, e a soma das fatias nao fecharia com o total. O tutor nao teria
     * como saber qual dos dois numeros acreditar.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnimalCostCategory category;

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

    /**
     * De qual dose de vacina, quando saiu de uma.
     *
     * <b>E o que faz a previsao da Tela 38 ter preco.</b> O reforco do ano que vem e precificado
     * pela dose do MESMO item de catalogo daquele animal — "aritmetica sobre fatos registrados". Sem
     * esta ligacao a unica alternativa seria o ultimo custo de categoria SAUDE, que cobraria a
     * antirrabica com o preco de uma consulta dermatologica.
     */
    @Column(name = "source_vaccine_id")
    private UUID sourceVaccineId;

    /** De qual antiparasitario. Existe pela mesma razao, e a Tela 38 poe os dois lado a lado. */
    @Column(name = "source_antiparasitic_id")
    private UUID sourceAntiparasiticId;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

}

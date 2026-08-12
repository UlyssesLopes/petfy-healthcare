package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quem banca parte do custo de um animal de abrigo (Tela 46).
 *
 * <b>NAO MOVE DINHEIRO, e essa e a decisao central.</b> Nao ha meio de pagamento em lugar nenhum
 * deste produto, e inventar um aqui seria construir a metade menos interessante do problema. O que
 * o desenho promete e outra coisa, e o produto ja sabe fazer: <i>"quando o remedio dele for
 * comprado, voce vai ver o evento — com data, valor e quem comprou."</i>
 *
 * Entao isto e um COMPROMISSO registrado — quem banca, o que banca, desde quando. O valor corre
 * fora, e o que o padrinho recebe em troca e a prestacao de contas que o produto ja dava ao tutor.
 *
 * <b>O padrinho NAO alcanca o animal, e isto nao e um {@link Grant}.</b> O desenho fecha a porta:
 * <i>"Nenhum historico clinico aberto ao padrinho. Ele ve o que banca."</i> Uma linha em
 * {@code grants} apareceria na tela de acessos do abrigo como se ele pudesse ler o animal, e um
 * escopo novo {@code CUSTO} seria uma promessa de recorte que guarda nenhuma cumpre. O servico
 * devolve os eventos de custo a partir DESTA linha, e o {@code AnimalAccessGuard} nao aprendeu nada.
 */
@Entity
@Table(name = "sponsorships")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sponsorship {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "sponsorship_id", updatable = false, nullable = false)
    private UUID sponsorshipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_person_id", nullable = false)
    private Person sponsor;

    /**
     * Quem respondia pelo animal quando isto comecou.
     *
     * <b>Nao e redundante com a custodia:</b> a custodia muda — o Teco pode ser adotado —, e o
     * apadrinhamento precisa lembrar a quem ele foi oferecido. Sem esta coluna, um animal que sai do
     * abrigo deixaria o padrinho bancando algo para quem nao cuida mais dele.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * O que se banca. "O remedio da artrose", "a racao", "a consulta".
     *
     * Texto, e nao referencia obrigatoria a uma linha de custo, porque o desenho oferece quatro
     * botoes e o quarto e <i>"Outro · valor livre"</i> — que e o unico que cobre o que o abrigo ainda
     * nao lancou.
     */
    @Column(nullable = false, length = 200)
    private String description;

    /** Quanto, por mes. {@code BigDecimal} como todo dinheiro deste schema. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * De qual linha de custo o valor saiu, quando saiu de uma.
     *
     * E o que liga "R$ 80 · o remedio da artrose" ao gasto real que o abrigo lancou, e o que permite
     * a tela afirmar que o numero nao foi inventado pelo produto: <i>"O que o abrigo gasta com ele
     * por mes"</i>. Nulo no "Outro".
     */
    @Column(name = "source_cost_id")
    private UUID sourceCostId;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    /** Quando o padrinho pediu para parar. Nulo enquanto ele nao pediu. */
    @Column(name = "cancel_requested_at")
    private LocalDateTime cancelRequestedAt;

    /**
     * Quando de fato para: trinta dias depois do pedido.
     *
     * <b>Sao duas datas porque a tela promete os trinta dias.</b> Uma coluna so faria o encerramento
     * ser imediato, e o abrigo descobriria no dia em que o remedio nao fosse comprado.
     */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SponsorshipStatus status;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    /**
     * Ainda banca?
     *
     * <b>{@code ENCERRAMENTO_PEDIDO} conta como banca</b>, e e o ponto do estado: quem pediu para
     * parar continua cobrindo o custo ate a data. Tratar o pedido como fim faria o abrigo perder
     * trinta dias de cobertura no instante do clique.
     */
    public boolean estaBancando(LocalDate hoje) {
        return status != SponsorshipStatus.ENCERRADO
                && (endsOn == null || !endsOn.isBefore(hoje));
    }

}

package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O pedido de unir dois cadastros do mesmo animal (Tela 32).
 *
 * <b>E um PEDIDO e nao uma acao, e essa e a decisao central desta tela.</b> Quem percebe a
 * duplicata e quase sempre a clinica — ela tem o leitor de microchip na mao —, e quem decide e
 * quem responde pelo animal. "Voce nao pode unir sozinha. Quem responde pelo Code e o Marcelo.
 * Ele recebe o pedido, ve exatamente esta comparacao e decide."
 *
 * <b>Por que nao pode ser da clinica:</b> a uniao e irreversivel. Duas linhas do tempo viram uma,
 * e desfazer exigiria saber de qual cadastro cada evento veio — o que so seria possivel gravando
 * a origem em cada uma das dezoito tabelas que apontam para animal. Ato irreversivel sobre a vida
 * registrada de um animal e de quem responde por ele, e nao de quem tem acesso. E a mesma regra
 * da transferencia de titularidade.
 *
 * <b>O motivo e obrigatorio</b> porque sem ele quem decide recebe um pedido que so diz "una", e
 * nao tem como julgar. Ele tambem viaja para o evento da uniao: "quem ler daqui a cinco anos vai
 * entender por que existem dois nomes no historico".
 */
@Entity
@Table(name = "animal_merge_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalMergeRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "animal_merge_request_id", updatable = false, nullable = false)
    private UUID animalMergeRequestId;

    /**
     * O cadastro que seria absorvido, e o que sobrevive.
     *
     * <b>Os nomes dizem o desfecho, e nao a ordem de chegada.</b> "Origem" e "destino" deixariam
     * ambiguo qual das duas linhas do tempo continua existindo com o proprio id — e e exatamente
     * essa a pergunta que quem le o pedido esta fazendo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "absorbed_animal_id", nullable = false)
    private Animal absorbed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "surviving_animal_id", nullable = false)
    private Animal surviving;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_person_id", nullable = false)
    private Person requestedBy;

    /** Nula quando quem pediu agiu por si — o veterinario autonomo que percebeu a duplicata. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AnimalMergeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_person_id")
    private Person decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /**
     * O que os dois cadastros diziam de diferente, no momento da decisao, em JSON.
     *
     * <b>Guardado no aceite, e nao recalculado na leitura.</b> "Onde ha conflito, o cadastro mais
     * antigo prevalece e o outro valor fica guardado no evento da uniao. Nada e escolhido em
     * silencio" — e um valor descartado que nao foi guardado no instante em que se descartou nao
     * volta nunca. Recalcular depois leria o cadastro ja unido, onde o valor perdido nao existe.
     */
    @Column(name = "discarded_values", columnDefinition = "text")
    private String discardedValues;

    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;

    public boolean estaPendente() {
        return status == AnimalMergeStatus.PENDENTE;
    }

}

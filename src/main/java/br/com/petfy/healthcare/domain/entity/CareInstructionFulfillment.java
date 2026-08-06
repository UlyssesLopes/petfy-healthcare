package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alguem cumpriu uma orientacao.
 *
 * <b>Cumprir e evento, e nao um booleano na orientacao.</b> "Dei o remedio" entra na
 * linha do tempo com quem e quando - e o que transforma orientacao em <i>historico de
 * aderencia</i>, o dado que o veterinario nunca tem quando o tratamento nao funciona.
 * Um campo {@code cumprido = true} na instrucao responderia "sim" e perderia as vinte
 * doses anteriores.
 *
 * <b>E e o que permite nao cobrar duas pessoas pela mesma coisa.</b> Dois tutores
 * dando o mesmo remedio e dano, nao incomodo: a pendencia consulta o ultimo
 * cumprimento e diz quem ja fez, em vez de aparecer identica para os dois.
 *
 * Nao ha DELETE: cumprimento registrado por engano se corrige registrando a correcao,
 * como todo o resto do historico deste produto.
 */
@Entity
@Table(name = "care_instruction_fulfillments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareInstructionFulfillment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "care_instruction_fulfillment_id", updatable = false, nullable = false)
    private UUID careInstructionFulfillmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_instruction_id", nullable = false)
    private CareInstruction careInstruction;

    /**
     * Quem cumpriu. <b>Obrigatorio</b>, ao contrario da autoria dos outros eventos: o
     * cumprimento nasce depois deste passo, entao nao ha linha antiga sem autor para
     * acomodar - e um cumprimento sem quem o fez nao serve ao historico de aderencia,
     * que existe para dizer se alguem esta dando o remedio.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_person_id", nullable = false)
    private Person confirmedBy;

    /**
     * Quando foi cumprido - e nao quando foi digitado.
     *
     * O tutor confirma as 22h o remedio que deu as 8h, e o instante do fato e o das 8h.
     * A mesma distincao que a linha do tempo faz entre acontecer e registrar.
     */
    @Column(name = "fulfilled_at", nullable = false)
    private LocalDateTime fulfilledAt;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(length = 500)
    private String note;

}

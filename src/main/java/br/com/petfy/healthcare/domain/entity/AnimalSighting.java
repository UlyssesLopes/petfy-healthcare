package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Alguem viu o animal hoje (Tela 43).
 *
 * <b>"Visto por ultimo" e o sinal vital da colonia:</b> "um gato de rua nao falta a creche nem
 * deixa de comer em casa: ele some". Marcar que viu e o gesto mais frequente daquela tela, e e o
 * que permite ao grupo perceber o desaparecimento em vez de descobri-lo tarde.
 *
 * <b>Nao e uma {@link Observation}, e a tentacao era grande</b> — observacao ja e "o que alguem
 * viu", ja tem autoria e ja entra na linha do tempo. Duas razoes a descartam. A de volume: isto
 * acontece todo dia, por seis pessoas, em catorze gatos, e a linha do tempo viraria uma coluna de
 * "vi o gato" que enterra a ferida registrada na terca — observacao existe para o que MUDOU, e o
 * avistamento vale justamente quando nada mudou. E a de pergunta: a tela quer "quantos dias desde
 * o ultimo", que aqui e um {@code max(seenOn)} e sobre observacao seria filtro por texto.
 */
@Entity
@Table(name = "animal_sightings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalSighting {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "animal_sighting_id", updatable = false, nullable = false)
    private UUID animalSightingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    /**
     * O dia, e nao o instante.
     *
     * Quem alimenta a colonia as 7h nao registra as 7h — registra quando senta, a noite. Guardar
     * hora daria ao dado uma precisao que ele nao tem.
     */
    @Column(name = "seen_on", nullable = false)
    private LocalDate seenOn;

    /**
     * Quem viu.
     *
     * Sem isto a tela nao diz "hoje, por Sandra" — e esse nome e o que faz o grupo saber que
     * alguem esteve la, e nao apenas que o gato estava.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_person_id", nullable = false)
    private Person recordedBy;

    /** De qual colonia este avistamento fala. Nulo quando quem viu nao agia por grupo nenhum. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

}

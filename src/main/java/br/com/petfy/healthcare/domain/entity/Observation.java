package br.com.petfy.healthcare.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O que alguem viu com o animal: nao comeu, mancou, vomitou, brigou.
 *
 * <b>Observacao nao e ato clinico, e a distincao e a mais importante do produto</b>
 * (3.11, e DESIGN 5.5). Ato clinico e diagnostico, prescricao, procedimento, e carrega
 * responsabilidade profissional; observacao e relato de fato, e qualquer um com acesso de
 * escrita pode registrar - tutor, monitor, lar transitorio, voluntario.
 *
 * <b>Observacao nunca vira ato clinico sozinha.</b> Pode ser <i>referenciada</i> por um:
 * o veterinario le "mancou da direita nos ultimos 3 dias" registrado pela creche e emite
 * um diagnostico que aponta para aquilo como evidencia. E esse caminho que faz o que a
 * creche viu chegar a quem pode diagnosticar, sem que a creche escreva no prontuario.
 */
@Entity
@Table(name = "observations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Observation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "observation_id", updatable = false, nullable = false)
    private UUID observationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(nullable = false, length = 1000)
    private String description;

    /**
     * Quando foi visto - e nao quando foi digitado.
     *
     * A creche registra as 18h o que viu as 9h, e a linha do tempo ordena por este campo
     * (3.9). Guardar so o instante da digitacao faria a cronologia mentir sobre a ordem
     * dos fatos, que e justamente o que permite perceber padrao.
     */
    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    /**
     * O alerta da creche (4.5): observacao com urgencia.
     *
     * <b>Nao e ato clinico e nao e emergencia medica.</b> Ganha peso pela posicao no feed,
     * e nao por cor de alarme (DESIGN 5.5) - "o alerta da creche e observacao com
     * urgencia, nao e vermelho". Fica aqui porque urgencia e atributo do que se viu, e nao
     * um segundo tipo de registro.
     */
    @Column(nullable = false)
    private boolean urgent;

    /** Quem registrou. O nucleo de evento do P4. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_person_id")
    private Person recordedBy;

    /** Em nome de que organizacao, quando houve uma. Nulo quando a pessoa agia por si. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

}

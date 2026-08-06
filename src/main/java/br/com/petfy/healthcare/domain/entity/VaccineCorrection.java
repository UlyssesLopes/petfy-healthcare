package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado anterior de uma vacina, gravado a cada correcao.
 *
 * Guarda o que era, e nao o que passou a ser: o estado atual esta na propria
 * vacina, entao repetir seria redundancia que pode divergir.
 */
@Entity
@Table(name = "vaccine_corrections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineCorrection {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID vaccineCorrectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaccine_id", nullable = false)
    private Vaccine vaccine;

    /**
     * Quem corrigiu. Sempre preenchido: nao ha mais duas colunas exclusivas entre
     * si, uma para tutor e outra para veterinario, porque nao ha mais dois tipos
     * de conta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_by_person_id", nullable = false)
    private Person correctedBy;

    /**
     * Em nome de qual organizacao a pessoa agiu, quando agiu por uma. Nulo quando
     * ela agiu por si.
     *
     * <b>Substituiu o papel.</b> "A Ana corrigiu" e "a Ana, pela Clinica Norte,
     * corrigiu" sao fatos diferentes, e so o segundo carrega responsabilidade
     * institucional - e o contexto da secao 3.2 do PRODUTO. Antes isso era
     * inferido de qual das duas colunas estava preenchida, o que misturava quem a
     * pessoa e com em nome de quem ela agiu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corrected_in_organization_id")
    private Organization correctedInOrganization;

    private String previousVaccineName;

    private LocalDate previousApplicationDate;

    private LocalDate previousNextDoseDate;

    private String previousDescription;

    @Column(nullable = false)
    private LocalDateTime correctedAt;

}

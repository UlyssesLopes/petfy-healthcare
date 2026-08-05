package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aplicacao de antiparasitario (vermifugo ou antipulgas/carrapatos).
 *
 * Entidade paralela a Vaccine: replica o minimo necessario para que o scheduler
 * de lembretes possa varrer as duas sem codigo novo de canal. A logica de
 * notificacao e identica - o Notifier nao conhece a diferenca.
 */
@Entity
@Table(name = "antiparasitics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Antiparasitic {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID antiparasiticId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    /** Nome do produto. Vem do catalogo quando informado; texto livre caso contrario. */
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AntiparasiticKind kind;

    private LocalDate applicationDate;

    private LocalDate nextDoseDate;

    private String description;

    /** Nulo para registro em texto livre ou para registros anteriores ao catalogo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "antiparasitic_catalog_id")
    private AntiparasiticCatalog catalog;

    /** Quando o ultimo lembrete desta dose foi enviado. Nulo se nunca avisamos. */
    private LocalDateTime lastReminderSentAt;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID petId;

    private String name;

    /**
     * Sub-classificacao livre (raca, cor, o que o tutor quiser). Nao serve para
     * casar com o catalogo de vacinas - para isso existe {@link #species}, que
     * e enum e obrigatorio.
     */
    private String type;

    private String breed;

    private LocalDate bornDate;

    private Double weight;

    private String gender;

    /**
     * Espécie. Obrigatorio: sem isso o filtro de catalogo por especie e o
     * protocolo de filhote nao tem como funcionar.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    private String generalRegistry;

    private String color;

    private Boolean microchip;

    private String bornLocal;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

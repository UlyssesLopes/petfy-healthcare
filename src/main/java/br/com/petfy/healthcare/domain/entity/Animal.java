package br.com.petfy.healthcare.domain.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "animals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Animal {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID animalId;

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

    /**
     * Quem cuida deste animal. Substituiu o {@code owner} unico na V15 - ver
     * {@link PetTutor} para por que a coluna antiga nao foi mantida ao lado.
     *
     * Nao ha cascade: vinculo se cria e se apaga pelos fluxos de convite e de
     * remocao, que tem regra propria (o titular nao pode simplesmente sumir).
     */
    @OneToMany(mappedBy = "animal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PetTutor> tutors = new ArrayList<>();

    /**
     * O titular. Sempre existe - a V15 fez o backfill e o indice unico parcial
     * garante que ha exatamente um - mas devolve Optional porque a colecao e
     * lazy e chamar isto fora de transacao e um erro de uso, nao um animal sem dono.
     */
    public Optional<Owner> getHolder() {
        return tutors.stream()
                .filter(PetTutor::isHolder)
                .map(PetTutor::getOwner)
                .findFirst();
    }

    /** Todos os tutores, em qualquer papel. Usado por quem notifica. */
    public List<Owner> getTutorOwners() {
        return tutors.stream().map(PetTutor::getOwner).toList();
    }

    private String generalRegistry;

    private String color;

    private Boolean microchip;

    /**
     * O numero, e nao apenas se tem. E o identificador legal do animal e o que liga o
     * Petfy a registro de animal perdido - o booleano acima ficou por compatibilidade do
     * contrato ja publicado, e o servico mantem os dois coerentes.
     */
    @Column(name = "microchip_number", length = 32)
    private String microchipNumber;

    /** Afeta protocolo vacinal, peso esperado e risco de doenca. */
    private Boolean castrated;

    /** A data importa tanto quanto o fato: muda o que se espera do peso e do humor. */
    @Column(name = "castrated_at")
    private LocalDate castratedAt;

    private String bornLocal;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

}

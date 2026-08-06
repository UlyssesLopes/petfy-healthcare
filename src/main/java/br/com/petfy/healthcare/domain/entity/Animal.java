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
     * A historia de quem respondeu por este animal.
     *
     * Substituiu a colecao de {@code PetTutor} no P2b, e a diferenca nao e de nome:
     * o PetTutor guardava tutor <b>e</b> quem tinha acesso concedido, entao a colecao
     * nao sabia responder "quem responde por este animal" sem filtrar por papel.
     * Aqui toda linha e alguem que respondeu, e no maximo uma esta em curso -
     * garantido por indice unico parcial no banco.
     *
     * Nao ha cascade: custodia se cria e se encerra por fluxo com regra propria, e
     * nenhuma termina sem sucessor.
     */
    @OneToMany(mappedBy = "animal", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Custody> custodies = new ArrayList<>();

    /**
     * Quem responde pelo animal agora, quando e uma pessoa.
     *
     * <b>Devolve vazio em dois casos diferentes, e quem chama nao precisa
     * distingui-los:</b> quando quem responde e uma organizacao - o abrigo nao tem
     * nome de pessoa para mostrar - e quando nao ha custodia em curso, o que
     * acontece depois de obito ou perda. Nenhum dos dois e erro.
     *
     * A colecao e lazy, entao chamar isto fora de transacao e erro de uso.
     */
    public Optional<Person> getHolder() {
        return custodies.stream()
                .filter(Custody::estaEmCurso)
                .map(Custody::getHolderPerson)
                .filter(java.util.Objects::nonNull)
                .findFirst();
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

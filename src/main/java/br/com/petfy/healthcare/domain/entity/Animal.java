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

    /**
     * A castracao MARCADA, e ainda nao feita — o mutirao do dia 22 (Tela 43).
     *
     * <b>Nao e o {@link #castratedAt} com data futura</b>, e a diferenca custa caro: aquele campo
     * afirma que o animal FOI castrado. O gato que nao aparece no dia do mutirao ficaria
     * registrado como castrado para sempre, e a proxima lista de "falta castrar" o deixaria de
     * fora — o defeito mais caro possivel numa tela cujo propósito é não perder gato nenhum.
     *
     * O servico limpa este campo quando a castracao acontece: um animal castrado nao tem
     * castracao marcada. Data no passado e sinal, e nao erro — quer dizer que o mutirao passou e
     * ninguem registrou o que houve.
     */
    @Column(name = "neutering_scheduled_for")
    private LocalDate neuteringScheduledFor;

    /**
     * Se o abrigo abriu este animal a padrinhos (Tela 46).
     *
     * <b>Coluna, e nao inferencia.</b> A alternativa era "todo animal sob custodia de organizacao
     * aceita padrinho", e ela poria o cao que chegou ontem, ainda sem diagnostico, na mesma vitrine do
     * Teco — e poria tambem o animal que o abrigo nao quer expor. A decisao de oferecer um animal a
     * padrinhos e do abrigo, e uma inferencia a tomaria por ele.
     *
     * Falso por omissao: nenhum animal existente passou a aceitar padrinho por causa da V43.
     */
    @Column(name = "accepts_sponsorship", nullable = false)
    @Builder.Default
    private Boolean acceptsSponsorship = false;

    private String bornLocal;

    private LocalDateTime creationDate;

    private LocalDateTime updateDate;

    /**
     * O cadastro que absorveu este, quando houve uniao (Tela 32).
     *
     * <b>Este registro NAO e apagado na uniao</b>, e e a mesma escolha que o vinculo desligado e
     * a orientacao encerrada ja fizeram: encerra-se, nao se apaga. Alguem tem o link do cadastro
     * antigo — a clinica que pediu a uniao guardou o id —, e um 404 diria que o animal nunca
     * existiu quando o que aconteceu foi o oposto: ele virou parte de outro.
     *
     * Nulo e o normal. Nao-nulo significa "eu sou um apontador: a vida deste animal esta la".
     */
    @Column(name = "merged_into_animal_id")
    private UUID mergedIntoAnimalId;

    /**
     * Este microchip aparece em outro cadastro que <b>alguem afirmou ser outro animal</b>.
     *
     * <b>E uma marca, e nao um erro.</b> "Se forem diferentes, o microchip repetido fica marcado
     * nos dois cadastros — provavelmente ha um erro de digitacao em algum lugar, e alguem vai
     * precisar saber disso." O produto nao sabe qual dos dois esta errado, e adivinhar apagaria
     * o numero certo metade das vezes: quem sabe e quem tem o animal na frente e o leitor de
     * microchip na mao.
     */
    @Column(name = "microchip_conflict", nullable = false)
    @Builder.Default
    private boolean microchipConflict = false;

    /** Um cadastro absorvido nao recebe registro novo: ele so aponta para quem o absorveu. */
    public boolean foiAbsorvido() {
        return mergedIntoAnimalId != null;
    }

}

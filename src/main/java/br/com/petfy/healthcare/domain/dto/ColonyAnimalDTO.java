package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da lista da colônia (Tela 43).
 *
 * <b>Não reusa o {@code VetPetDTO} da Tela 12</b>, e a diferença é o que a colônia pergunta: lá a
 * lista do abrigo responde "quais animais estão sob nossa responsabilidade"; aqui ela responde
 * "quem falta castrar, quem está em tratamento e **quem ninguém vê há dias**". A última é a
 * pergunta que só existe na rua.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColonyAnimalDTO {

    private UUID animalId;

    private String name;

    /** Castrado, e quando. */
    private Boolean neutered;

    private LocalDate neuteredAt;

    /** A castração marcada e ainda não feita — "Castração marcada, 22/08". */
    private LocalDate neuteringScheduledFor;

    /** "Tratamento renal, dia 12 de 30" — a orientação em curso, se houver. */
    private String ongoingCare;

    /**
     * O último avistamento: o dia, quem viu, e há quantos dias.
     *
     * <b>Os três nulos juntos significam "ninguém marcou nunca"</b>, e a tela diz isso — não
     * "sumido". Um gato registrado ontem por alguém que não marcou avistamento não está
     * desaparecido; ele está sem informação, que é outra coisa e aparece diferente no desenho.
     */
    private LocalDate lastSeenOn;

    private String lastSeenBy;

    private Long daysSinceLastSeen;

}

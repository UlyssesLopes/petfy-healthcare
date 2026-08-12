package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O ano do animal (Tela 48).
 *
 * <b>"Não é retrospectiva."</b> Um resumo anual de produto costuma ser sentimental e inútil —
 * colagem de fotos, número de passos, "que ano incrível". Este é um documento clínico e social do
 * animal, e serve para levar ao veterinário.
 *
 * <b>Por isso ele inclui o que deu errado</b>, e essa é a decisão que governa este DTO inteiro: os
 * campos {@code lapses} e {@code notReassessed} existem para dizer os 23 dias com a vacina vencida e
 * a displasia sem reavaliação desde 2023. <i>"Um resumo que só mostra o bonito não serve para
 * cuidar."</i>
 *
 * <b>E não há nada aqui para compartilhar</b>: sem imagem pronta, sem texto de story, sem marca
 * d'água. Se o tutor quiser mostrar, ele imprime — e o produto não ganha nada com isso, o que é
 * exatamente o ponto.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalYearDTO {

    private UUID animalId;

    private String animalName;

    /** O primeiro e o último dia da janela. "Agosto de 2025 a agosto de 2026." */
    private LocalDate from;

    private LocalDate to;

    /**
     * "Sétimo ano dele." Nulo quando não se sabe a data de nascimento.
     *
     * <b>Estimado a partir de {@code bornDate}</b>, e não contado desde o cadastro: o animal que
     * chegou ao Petfy aos cinco anos está no sétimo ano de vida, e não no segundo.
     */
    private Integer yearOfLife;

    // ------------------------------------------------------------------ os três números

    private long records;

    private long people;

    private long organizations;

    /** "142 dias na creche." */
    private long daycareDays;

    /** "E 7 de hospedagem." */
    private long boardingDays;

    /** "8,6 kg · era 7,5 kg em agosto passado." Nulos quando não houve pesagem na janela. */
    private Double weightNow;

    private Double weightBefore;

    // ---------------------------------------------------------- o que aconteceu de saúde

    private long appointments;

    private long vaccines;

    private long antiparasitics;

    /**
     * Os períodos em que uma vacina esteve vencida dentro da janela.
     *
     * <b>É o campo mais difícil deste DTO e o que faz o documento valer</b>: "a antirrábica ficou 23
     * dias vencida em julho. Foi o único período do ano em que ele esteve irregular."
     */
    private List<AnimalYearLapseDTO> lapses;

    /**
     * Condições crônicas abertas que ninguém tocou dentro da janela.
     *
     * "A displasia dele não foi reavaliada desde 2023."
     */
    private List<AnimalYearPendingDTO> notReassessed;

    /** "Quem cuidou dele este ano", do que mais registrou para o que menos. */
    private List<AnimalYearCaregiverDTO> caregivers;

}

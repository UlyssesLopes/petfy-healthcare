package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.CostRecurrence;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um valor, como quem responde pelo animal o le.
 *
 * <b>Traz quem registrou e por qual organizacao</b> porque "cada valor tem um evento por tras, com
 * autor e data — e por isso pode ser contestado como qualquer outro registro". Um numero sem nome
 * ao lado apareceria sozinho na conta do tutor, e ele nao teria a quem perguntar.
 *
 * <b>So chega a quem tem custodia.</b> Nao ha versao reduzida deste DTO para quem alcanca o animal
 * por concessao: a resposta certa para a creche que pergunta o preco da clinica e 403, e nao um
 * corpo com campos nulos que a faria achar que nao houve gasto.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalCostResponseDTO {

    private UUID animalCostId;

    private String description;

    private BigDecimal amount;

    private AnimalCostKind kind;

    /** Onde o dinheiro foi: SAUDE, ALIMENTACAO, CRECHE, HIGIENE ou OUTRO (Tela 37). */
    private AnimalCostCategory category;

    private Boolean paid;

    private CostRecurrence recurrence;

    /** De quantos em quantos meses o gasto volta. Nulo em gasto que nao se repete. */
    private Integer coversMonths;

    /**
     * De qual tratamento, quando o gasto e um remedio que o animal esta tomando.
     *
     * <b>E o unico `source` que esta resposta expoe</b>, e a assimetria e proposital: os outros
     * (atendimento, matricula, dose) ligam o custo a um evento que ja aconteceu, e quem le a conta
     * nao tem o que fazer com eles. Este liga a um tratamento EM CURSO — "isto e do remedio que o
     * Code toma ate dia 20" —, e ai a conta deixa de ser so uma soma.
     */
    private UUID sourceCareInstructionId;

    private LocalDateTime occurredAt;

    private String recordedByName;

    private String organizationName;

    private UUID sourceHealthRecordId;

    private UUID sourceEnrollmentId;

}

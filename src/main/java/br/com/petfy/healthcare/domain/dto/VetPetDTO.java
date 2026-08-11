package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O animal como o veterinario o enxerga.
 *
 * Traz o nome do tutor para o atendimento saber com quem esta falando, mas nao
 * os dados de contato: a clinica foi autorizada a atender o animal, nao a receber a
 * agenda de contatos do tutor.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VetPetDTO {

    private UUID animalId;

    private String name;

    private String type;

    private String breed;

    private LocalDate bornDate;

    private String gender;

    private Double weight;

    private String personName;

    private LocalDateTime accessGrantedAt;

    /**
     * A situacao da carteira: a coluna que a Tela 03 pedia e nao tinha.
     *
     * <b>E a dose mais urgente do animal</b>, e nao um resumo de todas: vencida vence tudo,
     * depois vencendo, depois em dia. Quem le a tabela decide a quem ligar hoje, e para isso o
     * pior caso e a unica informacao que serve.
     *
     * {@code NO_NEXT_DOSE} quando o animal nao tem dose com prazo — e isso NAO e "em dia": pode
     * ser animal sem carteira registrada, e afirmar saude a partir de ausencia de dado e a
     * mentira que o produto inteiro existe para nao contar.
     */
    private VaccineStatus healthStatus;

    /**
     * A ultima visita: a data do registro de saude mais recente. Nula quando nunca houve — e
     * nulo aqui e "nunca veio", que e diferente de "veio ha muito tempo".
     */
    private LocalDate lastVisitAt;

    /** Tem condicao de saude aberta — o "em tratamento" do cabecalho. */
    private boolean underTreatment;

}

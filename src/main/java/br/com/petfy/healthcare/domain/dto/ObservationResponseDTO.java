package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma observacao, como o cliente a le.
 *
 * <b>Nao carrega credencial de proposito</b>, ao contrario da entrada da linha do tempo:
 * observacao nao e ato clinico, e mostrar credencial aqui sugeriria responsabilidade
 * profissional sobre um relato de fato - o oposto da distincao que o 3.11 protege.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationResponseDTO {

    private UUID observationId;

    private UUID animalId;

    private String description;

    /** Quando foi visto. E por este campo que a lista ordena. */
    private LocalDateTime observedAt;

    /** Quando foi digitado. Diferente do acima sempre que houve atraso. */
    private LocalDateTime recordedAt;

    private boolean urgent;

    /** Quem viu. Nulo apenas se a pessoa foi removida depois. */
    private String recordedByName;

    /**
     * Em nome de que organizacao, quando houve uma.
     *
     * "A creche Pata Legal observou" e "a Maria observou" nao sao o mesmo fato: o primeiro
     * carrega responsabilidade institucional (3.2). Nulo quando quem registrou agia por
     * si - o tutor em casa, por exemplo.
     */
    private String organizationName;

}

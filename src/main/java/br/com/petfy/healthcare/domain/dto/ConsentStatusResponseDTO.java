package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.ConsentDocument;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O que o titular aceitou, e o que falta.
 *
 * {@code pendentes} e o campo que o cliente usa: enquanto nao estiver vazio, ha
 * documento vigente sem aceite - politica que mudou de versao, ou conta anterior a
 * V16, que nunca foi perguntada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentStatusResponseDTO {

    private List<AceiteDTO> aceites;

    private List<PendenteDTO> pendentes;

    /** Atalho para o cliente nao precisar inspecionar a lista. */
    private boolean tudoAceito;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AceiteDTO {

        private ConsentDocument document;

        private String documentVersion;

        private LocalDateTime acceptedAt;

        /**
         * A evidencia nao volta na resposta: IP e user agent existem para auditoria
         * interna, e devolve-los ao cliente exporia de onde a pessoa acessou sem
         * proposito nenhum para ela.
         */
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendenteDTO {

        private ConsentDocument document;

        /** Versao vigente, que e a que falta aceitar. */
        private String documentVersion;

        /** Versao que a pessoa aceitou antes, ou nulo se nunca aceitou nenhuma. */
        private String versaoAceitaAnteriormente;
    }

}

package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import br.com.petfy.healthcare.domain.entity.GroupApprovalStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Um pedido de concordância, do lado de quem lê (Telas 43 e 44). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupApprovalResponseDTO {

    private UUID groupApprovalId;

    private GroupApprovalKind kind;

    private GroupApprovalStatus status;

    private UUID animalId;

    private String animalName;

    private UUID targetPersonId;

    private String targetPersonName;

    private UUID toPersonId;

    private String toPersonName;

    private String reason;

    private String requestedByName;

    private LocalDateTime requestedAt;

    private String decidedByName;

    private LocalDateTime decidedAt;

    /**
     * Se quem está lendo pode decidir este pedido.
     *
     * <b>Vem do servidor, e não é calculado pela tela.</b> A regra é "qualquer pessoa do grupo,
     * menos quem pediu" — e o cliente recalculá-la criaria uma segunda verdade sobre quem manda,
     * do mesmo jeito que esconder o botão da união recalcularia custódia. A tela mostra o que o
     * servidor disser, e o servidor recusa de novo se alguém tentar.
     */
    private boolean canDecide;

}

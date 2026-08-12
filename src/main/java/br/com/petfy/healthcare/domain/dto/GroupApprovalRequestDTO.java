package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Pedir que outra pessoa do grupo concorde (Telas 43 e 44).
 *
 * <b>O `reason` não é obrigatório pelo validador, e é cobrado pela tela.</b> A distinção importa:
 * um pedido sem motivo é gravável, mas quem concorda recebe só "concorde" — e concordaria por
 * confiança, que é o oposto do que a regra quer. Barrar no servidor transformaria o esquecimento
 * num erro de formulário; o lugar de insistir é a tela, antes do gesto.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupApprovalRequestDTO {

    @NotNull(message = "kind e obrigatorio")
    private GroupApprovalKind kind;

    /** Sobre qual animal. Obrigatório em ADOCAO e OBITO. */
    private UUID animalId;

    /** Sobre qual pessoa. Obrigatório em REMOCAO_DE_MEMBRO. */
    private UUID targetPersonId;

    /** Para quem o animal iria. Obrigatório em ADOCAO. */
    private UUID toPersonId;

    private String reason;

    /**
     * A data do óbito, em `OBITO`.
     *
     * <b>Vem no PEDIDO, e não na concordância</b>, e a razão é que o dado é de quem viu: quem
     * encontrou o gato morto sabe quando foi, e quem concorda três dias depois não sabe. Usar a
     * data da concordância seria inventar um fato — e este é o campo que o tutor de um animal com
     * dono não consegue reconstruir depois.
     */
    private java.time.LocalDate deceasedOn;

}

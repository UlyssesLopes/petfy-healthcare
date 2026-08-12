package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.ReferralStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Um encaminhamento, do lado de quem lê (Tela 45).
 *
 * <b>Três leitores diferentes recebem este mesmo DTO</b>, e é por isso que dois campos vêm nulos com
 * frequência:
 * <ul>
 *   <li><b>quem encaminhou</b> — acompanha se foi autorizado;</li>
 *   <li><b>o tutor</b> — decide, e é o único que vê {@code canDecide} verdadeiro;</li>
 *   <li><b>o especialista</b> — vê o que chegou, e <b>não vê o nome do animal enquanto o
 *       encaminhamento está pendente</b>. Ver {@code ReferralServiceImpl}.</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralResponseDTO {

    private UUID referralId;

    private UUID animalId;

    /**
     * O nome do animal. <b>Nulo para o especialista enquanto o pedido está pendente</b> — o tutor
     * ainda não autorizou nada, e o nome já é informação sobre um animal que não é dele.
     */
    private String animalName;

    private ReferralStatus status;

    private String reason;

    private Set<GrantScope> scopes;

    private Integer accessDays;

    private UUID referredByPersonId;

    private String referredByName;

    /** A clínica de quem encaminhou. Nulo no veterinário autônomo. */
    private String fromOrganizationName;

    private UUID toPersonId;

    private String toPersonName;

    /** A especialidade declarada de quem recebe. Nula em quem não declarou. */
    private String toPersonSpecialty;

    private LocalDateTime requestedAt;

    private String decidedByName;

    private LocalDateTime decidedAt;

    /**
     * Até quando o acesso vale, se foi autorizado. Nulo enquanto não foi.
     *
     * <b>Vem da concessão, e não é `decidedAt + accessDays` calculado pela tela.</b> A data que vale
     * é a que está gravada no {@code Grant} — quem revoga antes muda aquela linha, e um cálculo no
     * cliente continuaria anunciando 08/11/2026 para um acesso que fechou em setembro.
     */
    private LocalDateTime accessExpiresAt;

    /**
     * Se quem está lendo pode decidir este encaminhamento.
     *
     * <b>Vem do servidor</b>, pela mesma razão do {@link GroupApprovalResponseDTO#isCanDecide()}: a
     * regra é "só quem responde pelo animal", e o cliente recalculá-la criaria uma segunda verdade
     * sobre quem manda. A tela mostra o que o servidor disser, e o servidor recusa de novo se alguém
     * tentar.
     */
    private boolean canDecide;

}

package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.GrantScope;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Quem alcanca este animal de fato.
 *
 * <b>Nao carrega e-mail nem telefone, e a ausencia e a regra.</b> A secao 5.4 do DESIGN e
 * explicita: "o que aparece aqui e quem tem alcance de fato - custodia e acesso -, nunca
 * uma lista de contatos". A rota antiga de tutores devolvia {@code personEmail}; esta nao,
 * e e a mesma postura do {@code VetPetDTO}, que ja recusava entregar a agenda do tutor a
 * quem foi autorizado a atender o animal.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareNetworkMemberDTO {

    /** Pessoa ou organizacao. Reaproveita a mesma enum do contexto ativo. */
    private ContextKind kind;

    /** Preenchido quando {@code kind} e PESSOA. */
    private UUID personId;

    /** Preenchido quando {@code kind} e ORGANIZACAO. */
    private UUID organizationId;

    private String name;

    /** Por que esta pessoa ou organizacao alcanca o animal. */
    private CareNetworkReach reach;

    /**
     * Se e quem <b>responde</b> pelo animal.
     *
     * Titular da custodia em curso. Quem responde vem primeiro na lista, sempre - era o que
     * a ordenacao por papel fazia antes de o papel deixar de existir.
     */
    private boolean holder;

    /**
     * O quanto alcanca.
     *
     * <b>Vazio na custodia, e isso nao significa "nada".</b> Escopo delimita concessao;
     * quem detem a custodia responde pelo animal e alcanca tudo - inventar um conjunto de
     * escopos para ele sugeriria um limite que nao existe.
     */
    private Set<GrantScope> scopes;

    /** Desde quando alcanca: inicio da custodia, ou momento da concessao. */
    private LocalDateTime since;

    /** Quando o alcance termina, quando tem prazo. Nulo na custodia e na concessao sem prazo. */
    private LocalDateTime expiresAt;

    /**
     * Quando contribuiu por ultimo neste animal.
     *
     * <b>E o que mostra que a rede esta viva</b> (DESIGN 5.4): "registrou hoje, registrou
     * ontem". Nulo em quem alcanca e nunca registrou nada - o que e um fato sobre a rede, e
     * nao um dado faltando.
     */
    private LocalDateTime lastContributionAt;

    /** Quem concedeu o acesso. Nulo na custodia, que ninguem concede. */
    private String grantedByName;

}

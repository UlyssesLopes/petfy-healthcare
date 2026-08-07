package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * Um contexto em que a pessoa pode atuar.
 *
 * <b>As capacidades vem junto de proposito.</b> Sem elas o cliente descobriria que
 * nao pode registrar um ato clinico so ao receber o erro, depois de ter desenhado o
 * formulario - e a alternativa seria ele reimplementar a regra de capacidade por
 * conta propria, que e o mesmo problema que o papel causava antes da Fase 6.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextOptionDTO {

    private ContextKind kind;

    /** Nulo quando o contexto e a propria pessoa. */
    private UUID organizationId;

    /** Nulo quando o contexto e a propria pessoa. */
    private String organizationName;

    /** O papel da pessoa <i>nesta</i> organizacao. Nulo no contexto de pessoa. */
    private MembershipRole role;

    /**
     * O que a organizacao pode. Vazio no contexto de pessoa, e nao por falta de
     * poder: quem limita o autonomo e a credencial dele, nao a capacidade de um
     * coletivo que ele nao tem.
     */
    private Set<OrganizationCapability> capabilities;

}

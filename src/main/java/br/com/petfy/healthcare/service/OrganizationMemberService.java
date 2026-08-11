package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.entity.MembershipRole;

import java.util.List;
import java.util.UUID;

/**
 * A equipe da organizacao: quem esta nela, com que funcao, e desde quando.
 *
 * Existe porque a Tela 16 nao tinha de onde tirar nada disso — o contrato devolvia CONVITES, e
 * nada devolvia quem ja entrou. Uma tabela de equipe vazia teria sido mentira: os membros
 * existem, e o produto e que nao sabia mostra-los.
 */
public interface OrganizationMemberService {

    /*
     * Os nomes sao `listMembers`/`changeMemberRole`/`removeMember` e nao `list`/`changeRole`/
     * `remove` porque o springdoc deriva o `operationId` do nome do metodo: colisao com outro
     * controller o faz renomear a operacao ALHEIA com sufixo numerico, e o sufixo depende da
     * ordem de varredura.
     */

    /** Quem esta na equipe agora. */
    List<MembershipResponseDTO> listMembers();

    /** O "ajustar" do desenho: muda a funcao de quem ja e da equipe. */
    MembershipResponseDTO changeMemberRole(UUID membershipId, MembershipRole role);

    /** O desligamento. Nao apaga o vinculo: marca a saida, porque o que a pessoa registrou fica. */
    void removeMember(UUID membershipId);

}

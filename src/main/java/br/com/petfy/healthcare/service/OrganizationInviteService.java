package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInvitePreviewResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteResponseDTO;
import br.com.petfy.healthcare.domain.entity.OrganizationInvite;

import java.util.List;
import java.util.UUID;

public interface OrganizationInviteService {

    OrganizationInviteResponseDTO create(OrganizationInviteRequestDTO request);

    List<OrganizationInviteResponseDTO> listFromMyOrganization();

    void revoke(UUID organizationInviteId);

    /**
     * Valida o convite para o email que esta se cadastrando. Nao consome ainda -
     * o vet precisa existir antes de poder ser registrado como quem aceitou.
     */
    OrganizationInvite validate(String token, String email);

    void markAccepted(OrganizationInvite invite, UUID acceptedByVetId);

    /**
     * O convite que esta na mao de quem ja tem conta, antes do aceite.
     *
     * Le sem consumir: abrir o link nao pode gastar o convite, senao quem clicou para ler
     * perderia o direito de entrar.
     */
    OrganizationInvitePreviewResponseDTO preview(String token);

    /**
     * O aceite de quem JA tem conta — o caminho que faltava.
     *
     * Ate aqui o unico aceite possivel era o {@code inviteToken} na CRIACAO da conta, e a
     * consequencia era absurda na pratica: a veterinaria que ja usa o Petfy, convidada pela
     * clinica, so entraria criando uma segunda conta com outro e-mail — e levando consigo
     * nenhum dos animais que ja acompanha.
     */
    MembershipResponseDTO accept(String token);

}

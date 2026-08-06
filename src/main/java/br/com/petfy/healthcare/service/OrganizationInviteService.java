package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
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

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OrganizationAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationAccessResponseDTO;

import java.util.List;
import java.util.UUID;

/** Lado do tutor: quem decide quais clinicas alcancam o animal. */
public interface OrganizationAccessService {

    OrganizationAccessResponseDTO grant(UUID animalId, OrganizationAccessRequestDTO request);

    List<OrganizationAccessResponseDTO> list(UUID animalId);

    void revoke(UUID animalId, UUID organizationId);

}

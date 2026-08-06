package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizationService {

    OrganizationResponseDTO createOrganization(OrganizationRequestDTO request);

    OrganizationResponseDTO getOrganizationById(UUID organizationId);

    /** Listagem paginada para o controller. */
    Page<OrganizationResponseDTO> listAllOrganizations(Pageable pageable);

    OrganizationResponseDTO updateOrganization(UUID organizationId, OrganizationRequestDTO request);

    void deleteOrganization(UUID organizationId);

}

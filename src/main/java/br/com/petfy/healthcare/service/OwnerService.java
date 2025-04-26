package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;

import java.util.List;
import java.util.UUID;

public interface OwnerService {

    OwnerResponseDTO createOwner(OwnerRequestDTO request);

    List<OwnerResponseDTO> listAllOwners();

    OwnerResponseDTO getOwnerById(UUID ownerId);

    OwnerResponseDTO updateOwner(UUID id, OwnerRequestDTO request);

    void deleteOwner(UUID id);

}

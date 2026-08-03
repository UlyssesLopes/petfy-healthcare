package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;

/**
 * Nao ha busca por id nem listagem: um owner so enxerga a si mesmo, entao o id
 * viria sempre do token e nunca da URL.
 */
public interface OwnerService {

    OwnerResponseDTO createOwner(OwnerRequestDTO request);

    OwnerResponseDTO getCurrentOwner();

    OwnerResponseDTO updateCurrentOwner(OwnerRequestDTO request);

    void deleteCurrentOwner();

}

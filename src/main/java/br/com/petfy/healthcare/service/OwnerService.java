package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;

/**
 * Nao ha busca por id nem listagem: um owner so enxerga a si mesmo, entao o id
 * viria sempre do token e nunca da URL.
 */
public interface OwnerService {

    OwnerResponseDTO createOwner(OwnerRequestDTO request);

    OwnerResponseDTO getCurrentOwner();

    OwnerResponseDTO updateCurrentOwner(OwnerRequestDTO request);

    /**
     * Endpoint proprio porque o updateCurrentOwner ignora o campo password: trocar
     * senha exige confirmar a atual, e um PUT parcial nao tem como exigir isso.
     */
    void changePassword(PasswordChangeRequestDTO request);

    void deleteCurrentOwner();

}

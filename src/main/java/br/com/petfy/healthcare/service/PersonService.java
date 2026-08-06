package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PersonRequestDTO;
import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;

/**
 * Nao ha busca por id nem listagem: um person so enxerga a si mesmo, entao o id
 * viria sempre do token e nunca da URL.
 */
public interface PersonService {

    PersonResponseDTO createPerson(PersonRequestDTO request);

    PersonResponseDTO getCurrentPerson();

    PersonResponseDTO updateCurrentPerson(PersonRequestDTO request);

    /**
     * Endpoint proprio porque o updateCurrentPerson ignora o campo password: trocar
     * senha exige confirmar a atual, e um PUT parcial nao tem como exigir isso.
     */
    void changePassword(PasswordChangeRequestDTO request);

    void deleteCurrentPerson();

}

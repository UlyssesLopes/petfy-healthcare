package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetHealthConditionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Alergias e condicoes cronicas do pet.
 *
 * Existem para aparecer em destaque, e nao enterradas na descricao de um atendimento:
 * alergia a anestesico perdida num texto corrido e o tipo de informacao que so se
 * descobre que faltava depois de um procedimento.
 */
public interface PetHealthConditionService {

    PetHealthConditionResponseDTO create(UUID petId, PetHealthConditionRequestDTO request);

    /** Ativas primeiro: a pergunta e "o que vale para este animal hoje". */
    List<PetHealthConditionResponseDTO> listByPet(UUID petId);

    PetHealthConditionResponseDTO update(UUID petId, UUID conditionId, PetHealthConditionRequestDTO request);

    void delete(UUID petId, UUID conditionId);

}

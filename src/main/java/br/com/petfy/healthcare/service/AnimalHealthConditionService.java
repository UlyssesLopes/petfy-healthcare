package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalHealthConditionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Alergias e condicoes cronicas do animal.
 *
 * Existem para aparecer em destaque, e nao enterradas na descricao de um atendimento:
 * alergia a anestesico perdida num texto corrido e o tipo de informacao que so se
 * descobre que faltava depois de um procedimento.
 */
public interface AnimalHealthConditionService {

    AnimalHealthConditionResponseDTO create(UUID animalId, AnimalHealthConditionRequestDTO request);

    /** Ativas primeiro: a pergunta e "o que vale para este animal hoje". */
    List<AnimalHealthConditionResponseDTO> listByAnimal(UUID animalId);

    AnimalHealthConditionResponseDTO update(UUID animalId, UUID conditionId, AnimalHealthConditionRequestDTO request);

    void delete(UUID animalId, UUID conditionId);

}

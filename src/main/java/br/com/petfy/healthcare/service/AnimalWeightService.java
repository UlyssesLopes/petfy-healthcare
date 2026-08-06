package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalWeightResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AnimalWeightService {

    AnimalWeightResponseDTO addWeight(UUID animalId, AnimalWeightRequestDTO request);

    List<AnimalWeightResponseDTO> listWeights(UUID animalId);

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetWeightResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PetWeightService {

    PetWeightResponseDTO addWeight(UUID petId, PetWeightRequestDTO request);

    List<PetWeightResponseDTO> listWeights(UUID petId);

}

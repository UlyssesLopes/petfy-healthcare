package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PetService {

    PetResponseDTO createPet(PetRequestDTO dto);

    PetResponseDTO getPetById(UUID petId);

    List<PetResponseDTO> listAllPets();

    PetResponseDTO updatePet(UUID petId, PetRequestDTO dto);

    void deletePet(UUID petId);

}

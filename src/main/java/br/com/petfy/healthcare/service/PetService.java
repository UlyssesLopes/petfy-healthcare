package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PetService {

    PetResponseDTO createPet(PetRequestDTO dto);

    PetResponseDTO getPetById(UUID petId);

    /** Listagem paginada para o controller. */
    Page<PetResponseDTO> listAllPets(Pageable pageable);

    PetResponseDTO updatePet(UUID petId, PetRequestDTO dto);

    void deletePet(UUID petId);

}

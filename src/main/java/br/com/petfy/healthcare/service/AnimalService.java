package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AnimalService {

    AnimalResponseDTO createAnimal(AnimalRequestDTO dto);

    AnimalResponseDTO getAnimalById(UUID animalId);

    /** Listagem paginada para o controller. */
    Page<AnimalResponseDTO> listAllAnimals(Pageable pageable);

    AnimalResponseDTO updateAnimal(UUID animalId, AnimalRequestDTO dto);

    void deleteAnimal(UUID animalId);

}

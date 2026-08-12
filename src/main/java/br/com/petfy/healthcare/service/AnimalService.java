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

    /**
     * "Quem ja esteve com voce" (Tela 33).
     *
     * A lista que recebe o animal que saiu da outra. <b>Nao e uma lista de animais mortos:</b> o
     * animal transferido sem acesso residual tambem esta aqui, porque a pergunta que ela responde
     * e "de quem eu ja cuidei", e nao "quem morreu".
     */
    Page<AnimalResponseDTO> queJaEstiveramComigo(Pageable pageable);

    AnimalResponseDTO updateAnimal(UUID animalId, AnimalRequestDTO dto);

    void deleteAnimal(UUID animalId);

}

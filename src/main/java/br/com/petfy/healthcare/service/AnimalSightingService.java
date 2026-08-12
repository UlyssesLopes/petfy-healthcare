package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalSightingRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalSightingResponseDTO;

import java.util.UUID;

/** "Vi o gato" — o sinal vital da colônia (Tela 43). */
public interface AnimalSightingService {

    AnimalSightingResponseDTO registrar(UUID animalId, AnimalSightingRequestDTO dto);

}

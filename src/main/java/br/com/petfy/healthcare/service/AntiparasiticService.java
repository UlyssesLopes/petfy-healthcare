package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AntiparasiticCatalogResponseDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticRequestDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AntiparasiticService {

    AntiparasiticResponseDTO create(AntiparasiticRequestDTO request);

    AntiparasiticResponseDTO update(UUID id, AntiparasiticRequestDTO request);

    void delete(UUID id);

    AntiparasiticResponseDTO getById(UUID id);

    List<AntiparasiticResponseDTO> listByPet(UUID petId);

    List<AntiparasiticCatalogResponseDTO> listCatalog();

}

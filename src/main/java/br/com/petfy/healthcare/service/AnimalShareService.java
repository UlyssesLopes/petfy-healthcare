package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalShareResponseDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;

import java.util.List;
import java.util.UUID;

public interface AnimalShareService {

    AnimalShareResponseDTO createShare(UUID animalId, AnimalShareRequestDTO request);

    List<AnimalShareResponseDTO> listShares(UUID animalId);

    void revokeShare(UUID animalShareId);

    /** Acesso publico: quem tem o token nao precisa estar autenticado. */
    SharedVaccineCardDTO viewSharedCard(String token);

}

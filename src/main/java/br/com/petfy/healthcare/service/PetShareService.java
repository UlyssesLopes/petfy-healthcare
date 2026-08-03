package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PetShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetShareResponseDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;

import java.util.List;
import java.util.UUID;

public interface PetShareService {

    PetShareResponseDTO createShare(UUID petId, PetShareRequestDTO request);

    List<PetShareResponseDTO> listShares(UUID petId);

    void revokeShare(UUID petShareId);

    /** Acesso publico: quem tem o token nao precisa estar autenticado. */
    SharedVaccineCardDTO viewSharedCard(String token);

}

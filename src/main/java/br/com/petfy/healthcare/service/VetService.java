package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;

public interface VetService {

    VetResponseDTO register(VetRequestDTO request);

    VetResponseDTO getCurrentVet();

}

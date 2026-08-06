package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicAccessResponseDTO;

import java.util.List;
import java.util.UUID;

/** Lado do tutor: quem decide quais clinicas alcancam o animal. */
public interface PetClinicAccessService {

    ClinicAccessResponseDTO grant(UUID animalId, ClinicAccessRequestDTO request);

    List<ClinicAccessResponseDTO> list(UUID animalId);

    void revoke(UUID animalId, UUID clinicId);

}

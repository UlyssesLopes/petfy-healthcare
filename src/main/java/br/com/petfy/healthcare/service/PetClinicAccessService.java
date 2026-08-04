package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicAccessResponseDTO;

import java.util.List;
import java.util.UUID;

/** Lado do tutor: quem decide quais clinicas alcancam o pet. */
public interface PetClinicAccessService {

    ClinicAccessResponseDTO grant(UUID petId, ClinicAccessRequestDTO request);

    List<ClinicAccessResponseDTO> list(UUID petId);

    void revoke(UUID petId, UUID clinicId);

}

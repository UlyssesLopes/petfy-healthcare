package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClinicService {

    ClinicResponseDTO createClinic(ClinicRequestDTO request);

    ClinicResponseDTO getClinicById(UUID clinicId);

    /** Listagem paginada para o controller. */
    Page<ClinicResponseDTO> listAllClinics(Pageable pageable);

    ClinicResponseDTO updateClinic(UUID clinicId, ClinicRequestDTO request);

    void deleteClinic(UUID clinicId);

}

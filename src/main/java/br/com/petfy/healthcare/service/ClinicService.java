package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ClinicService {

    ClinicResponseDTO createClinic(ClinicRequestDTO request);

    ClinicResponseDTO getClinicById(UUID clinicId);

    List<ClinicResponseDTO> listAllClinics();

    ClinicResponseDTO updateClinic(UUID clinicId, ClinicRequestDTO request);

    void deleteClinic(UUID clinicId);

}

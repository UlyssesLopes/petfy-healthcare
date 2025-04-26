package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;

import java.util.List;
import java.util.UUID;

public interface VaccineService {

    VaccineResponseDTO createVaccine(VaccineRequestDTO request);

    VaccineResponseDTO updateVaccine(UUID id, VaccineRequestDTO request);

    void deleteVaccine(UUID id);

    VaccineResponseDTO getVaccineById(UUID id);

    List<VaccineResponseDTO> listAllVaccines();

}

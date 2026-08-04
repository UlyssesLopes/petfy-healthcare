package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;

import java.util.List;
import java.util.UUID;

/** Lado do veterinario: o que ele alcanca dos pets que a clinica dele atende. */
public interface VetPetService {

    List<VetPetDTO> listAccessiblePets();

    List<VaccineResponseDTO> listVaccines(UUID petId);

    VaccineResponseDTO registerVaccine(UUID petId, VaccineRequestDTO request);

    /** Corrige um registro da propria clinica, dentro da janela de correcao. */
    VaccineResponseDTO correctVaccine(UUID petId, UUID vaccineId, VaccineRequestDTO request);

    List<VaccineCorrectionResponseDTO> listCorrections(UUID petId, UUID vaccineId);

    List<HealthRecordResponseDTO> listHealthRecords(UUID petId);

    HealthRecordResponseDTO registerHealthRecord(UUID petId, HealthRecordRequestDTO request);

    /** Corrige um atendimento da propria clinica, dentro da janela de correcao. */
    HealthRecordResponseDTO correctHealthRecord(UUID petId, UUID healthRecordId, HealthRecordRequestDTO request);

    List<HealthRecordCorrectionResponseDTO> listHealthRecordCorrections(UUID petId, UUID healthRecordId);

}

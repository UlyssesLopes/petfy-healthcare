package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;

import java.util.List;
import java.util.UUID;

public interface HealthRecordService {

    HealthRecordResponseDTO createHealthRecord(HealthRecordRequestDTO request);

    HealthRecordResponseDTO getHealthRecordById(UUID healthRecordId);

    List<HealthRecordResponseDTO> listAllHealthRecords();

    List<HealthRecordResponseDTO> listHealthRecordsByPet(UUID petId);

    HealthRecordResponseDTO updateHealthRecord(UUID healthRecordId, HealthRecordRequestDTO request);

    void deleteHealthRecord(UUID healthRecordId);

    /** Rastro de alteracoes de um registro do proprio tutor. */
    List<HealthRecordCorrectionResponseDTO> listCorrections(UUID healthRecordId);

}

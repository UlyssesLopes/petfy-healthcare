package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface HealthRecordService {

    HealthRecordResponseDTO createHealthRecord(HealthRecordRequestDTO request);

    HealthRecordResponseDTO getHealthRecordById(UUID healthRecordId);

    /** Listagem paginada para o controller. */
    Page<HealthRecordResponseDTO> listAllHealthRecords(Pageable pageable);

    List<HealthRecordResponseDTO> listHealthRecordsByPet(UUID petId);

    HealthRecordResponseDTO updateHealthRecord(UUID healthRecordId, HealthRecordRequestDTO request);

    void deleteHealthRecord(UUID healthRecordId);

    /** Rastro de alteracoes de um registro do proprio tutor. */
    List<HealthRecordCorrectionResponseDTO> listCorrections(UUID healthRecordId);

}

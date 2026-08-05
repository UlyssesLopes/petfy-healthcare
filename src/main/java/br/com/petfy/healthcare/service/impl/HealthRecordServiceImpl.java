package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.HealthRecordCorrectionLog;
import br.com.petfy.healthcare.service.HealthRecordService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;

    private final ClinicRepository clinicRepository;

    private final CurrentOwnerProvider currentOwnerProvider;
    private final PetAccessGuard petAccessGuard;

    private final HealthRecordCorrectionLog healthRecordCorrectionLog;

    @Override
    public HealthRecordResponseDTO createHealthRecord(HealthRecordRequestDTO request) {
        Pet pet = findPet(request.getPetId());

        Clinic clinic = request.getClinicId() != null ? findClinic(request.getClinicId()) : null;

        HealthRecord healthRecord = HealthRecord.builder()
                .pet(pet)
                .clinic(clinic)
                .eventType(request.getEventType())
                .eventDate(request.getEventDate())
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .build();

        return toResponse(healthRecordRepository.save(healthRecord));
    }

    @Override
    public HealthRecordResponseDTO getHealthRecordById(UUID healthRecordId) {
        return toResponse(buscarAlcancavel(healthRecordId));
    }

    @Override
    public Page<HealthRecordResponseDTO> listAllHealthRecords(Pageable pageable) {
        return healthRecordRepository
                .findByPetTutorsOwnerOwnerIdOrderByEventDateDesc(currentOwnerProvider.require().getOwnerId(), pageable)
                .map(this::toResponse);
    }

    /**
     * Ler o historico e leitura: o tutor VIEWER acompanha o que aconteceu com o
     * pet sem poder lancar atendimento. Exigir EDITOR aqui deixaria de fora
     * justamente quem so olha.
     */
    @Override
    public List<HealthRecordResponseDTO> listHealthRecordsByPet(UUID petId) {
        // valida o pet primeiro para diferenciar "pet nao existe" de
        // "pet existe e ainda nao tem historico"
        petAccessGuard.requireLeitura(petId);

        return healthRecordRepository.findByPetPetIdOrderByEventDateDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponseDTO updateHealthRecord(UUID healthRecordId, HealthRecordRequestDTO request) {
        HealthRecord existing = buscarAlcancavel(healthRecordId);

        // o snapshot sai antes dos setters. O tutor nao tem janela - o historico e
        // do pet dele - mas deixa rastro igual, senao a auditoria contaria meia
        // verdade e daria impressao de completude
        healthRecordCorrectionLog.recordByOwner(existing, currentOwnerProvider.require());

        if (request.getEventType() != null) existing.setEventType(request.getEventType());
        if (request.getEventDate() != null) existing.setEventDate(request.getEventDate());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());

        if (request.getPetId() != null) {
            existing.setPet(findPet(request.getPetId()));
        }

        if (request.getClinicId() != null) {
            existing.setClinic(findClinic(request.getClinicId()));
        }

        existing.setUpdateDate(LocalDateTime.now());

        return toResponse(healthRecordRepository.save(existing));
    }

    @Override
    public List<HealthRecordCorrectionResponseDTO> listCorrections(UUID healthRecordId) {
        // mesma checagem de propriedade da leitura do registro: o rastro e tao do
        // tutor quanto o registro
        buscarAlcancavel(healthRecordId);

        return healthRecordCorrectionLog.list(healthRecordId);
    }

    @Override
    public void deleteHealthRecord(UUID healthRecordId) {
        healthRecordRepository.delete(buscarAlcancavel(healthRecordId));
    }

    /**
     * Registro de pet fora do alcance responde HEALTH_RECORD_NOT_FOUND, e nao
     * 403: um 403 confirmaria que aquele id existe.
     *
     * Basta alcancar o pet em qualquer papel - ler o proprio registro e leitura.
     */
    private HealthRecord buscarAlcancavel(UUID healthRecordId) {
        return healthRecordRepository.findById(healthRecordId)
                .filter(registro -> petAccessGuard.alcanca(registro.getPet().getPetId()))
                .orElseThrow(this::notFound);
    }

    /**
     * Registrar atendimento no historico de saude e escrita, entao exige EDITOR.
     * Pet inalcancavel e indistinguivel de pet inexistente - ver PetAccessGuard.
     */
    private Pet findPet(UUID petId) {
        return petAccessGuard.requireEscrita(petId);
    }

    private Clinic findClinic(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
    }

    private PetfyHealthcareException notFound() {
        return new PetfyHealthcareException(ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getMessage(), ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    private HealthRecordResponseDTO toResponse(HealthRecord healthRecord) {
        return HealthRecordResponseDTO.builder()
                .healthRecordId(healthRecord.getHealthRecordId())
                .eventType(healthRecord.getEventType())
                .eventDate(healthRecord.getEventDate())
                .description(healthRecord.getDescription())
                .petId(healthRecord.getPet().getPetId())
                .clinicId(healthRecord.getClinic() != null ? healthRecord.getClinic().getClinicId() : null)
                .creationDate(healthRecord.getCreationDate())
                .updateDate(healthRecord.getUpdateDate())
                .build();
    }

}

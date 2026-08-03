package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.HealthRecordService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;

    private final PetRepository petRepository;

    private final ClinicRepository clinicRepository;

    private final CurrentOwnerProvider currentOwnerProvider;

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
        return toResponse(buscarDoOwnerAutenticado(healthRecordId));
    }

    @Override
    public List<HealthRecordResponseDTO> listAllHealthRecords() {
        return healthRecordRepository
                .findByPetOwnerOwnerIdOrderByEventDateDesc(currentOwnerProvider.require().getOwnerId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HealthRecordResponseDTO> listHealthRecordsByPet(UUID petId) {
        // valida o pet primeiro para diferenciar "pet nao existe" de
        // "pet existe e ainda nao tem historico"
        findPet(petId);

        return healthRecordRepository.findByPetPetIdOrderByEventDateDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponseDTO updateHealthRecord(UUID healthRecordId, HealthRecordRequestDTO request) {
        HealthRecord existing = buscarDoOwnerAutenticado(healthRecordId);

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
    public void deleteHealthRecord(UUID healthRecordId) {
        healthRecordRepository.delete(buscarDoOwnerAutenticado(healthRecordId));
    }

    /**
     * Registro de pet de outro dono responde HEALTH_RECORD_NOT_FOUND, e nao 403:
     * um 403 confirmaria que aquele id existe.
     */
    private HealthRecord buscarDoOwnerAutenticado(UUID healthRecordId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return healthRecordRepository.findById(healthRecordId)
                .filter(registro -> registro.getPet().getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(this::notFound);
    }

    /** Pet de outro dono e indistinguivel de pet inexistente, pelo mesmo motivo. */
    private Pet findPet(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.PET_NOT_FOUND.getMessage(), ErrorMessageEnum.PET_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
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

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
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

    private final OrganizationRepository organizationRepository;

    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalAccessGuard animalAccessGuard;

    private final HealthRecordCorrectionLog healthRecordCorrectionLog;

    @Override
    public HealthRecordResponseDTO createHealthRecord(HealthRecordRequestDTO request) {
        Animal animal = findAnimal(request.getAnimalId());

        Organization organization = request.getOrganizationId() != null ? findOrganization(request.getOrganizationId()) : null;

        HealthRecord healthRecord = HealthRecord.builder()
                .animal(animal)
                .organization(organization)
                .eventType(request.getEventType())
                .category(request.getCategory())
                .diagnosis(request.getDiagnosis())
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
                .findAlcancadosPor(currentPersonProvider.require().getPersonId(), LocalDateTime.now(), pageable)
                .map(this::toResponse);
    }

    /**
     * Ler o historico e leitura: o tutor VIEWER acompanha o que aconteceu com o
     * animal sem poder lancar atendimento. Exigir EDITOR aqui deixaria de fora
     * justamente quem so olha.
     */
    @Override
    public List<HealthRecordResponseDTO> listHealthRecordsByAnimal(UUID animalId) {
        // valida o animal primeiro para diferenciar "animal nao existe" de
        // "animal existe e ainda nao tem historico"
        animalAccessGuard.requireLeitura(animalId);

        return healthRecordRepository.findByAnimalAnimalIdOrderByEventDateDesc(animalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public HealthRecordResponseDTO updateHealthRecord(UUID healthRecordId, HealthRecordRequestDTO request) {
        HealthRecord existing = buscarAlcancavel(healthRecordId);

        // o snapshot sai antes dos setters. O tutor nao tem janela - o historico e
        // do animal dele - mas deixa rastro igual, senao a auditoria contaria meia
        // verdade e daria impressao de completude
        healthRecordCorrectionLog.recordByPerson(existing, currentPersonProvider.require());

        if (request.getEventType() != null) existing.setEventType(request.getEventType());
        if (request.getCategory() != null) existing.setCategory(request.getCategory());
        if (request.getDiagnosis() != null) existing.setDiagnosis(request.getDiagnosis());
        if (request.getEventDate() != null) existing.setEventDate(request.getEventDate());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());

        if (request.getAnimalId() != null) {
            existing.setAnimal(findAnimal(request.getAnimalId()));
        }

        if (request.getOrganizationId() != null) {
            existing.setOrganization(findOrganization(request.getOrganizationId()));
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
     * Registro de animal fora do alcance responde HEALTH_RECORD_NOT_FOUND, e nao
     * 403: um 403 confirmaria que aquele id existe.
     *
     * Basta alcancar o animal em qualquer papel - ler o proprio registro e leitura.
     */
    private HealthRecord buscarAlcancavel(UUID healthRecordId) {
        return healthRecordRepository.findById(healthRecordId)
                .filter(registro -> animalAccessGuard.alcanca(registro.getAnimal().getAnimalId()))
                .orElseThrow(this::notFound);
    }

    /**
     * Registrar atendimento no historico de saude e escrita, entao exige EDITOR.
     * Animal inalcancavel e indistinguivel de animal inexistente - ver AnimalAccessGuard.
     */
    private Animal findAnimal(UUID animalId) {
        return animalAccessGuard.requireEscrita(animalId);
    }

    private Organization findOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
    }

    private PetfyHealthcareException notFound() {
        return new PetfyHealthcareException(ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getMessage(), ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    private HealthRecordResponseDTO toResponse(HealthRecord healthRecord) {
        return HealthRecordResponseDTO.builder()
                .healthRecordId(healthRecord.getHealthRecordId())
                .eventType(healthRecord.getEventType())
                .category(healthRecord.getCategory())
                .diagnosis(healthRecord.getDiagnosis())
                .eventDate(healthRecord.getEventDate())
                .description(healthRecord.getDescription())
                .animalId(healthRecord.getAnimal().getAnimalId())
                .organizationId(healthRecord.getOrganization() != null ? healthRecord.getOrganization().getOrganizationId() : null)
                .creationDate(healthRecord.getCreationDate())
                .updateDate(healthRecord.getUpdateDate())
                .build();
    }

}

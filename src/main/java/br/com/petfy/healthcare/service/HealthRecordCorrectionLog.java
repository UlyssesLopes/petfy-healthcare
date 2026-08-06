package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.HealthRecordCorrection;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.HealthRecordCorrectionRepository;
import br.com.petfy.healthcare.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rastro de correcoes de um registro de saude: grava e le.
 *
 * Espelha o VaccineCorrectionLog. Sao dois componentes parecidos, e nao um
 * generico, porque cada um guarda os campos do seu dominio - um rastro
 * polimorfico teria que serializar os campos e perderia tanto a tipagem quanto a
 * chave estrangeira para o registro corrigido.
 */
@Component
@RequiredArgsConstructor
public class HealthRecordCorrectionLog {

    private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;

    public void recordByVet(HealthRecord antes, Vet vet) {
        healthRecordCorrectionRepository.save(snapshot(antes).correctedByVet(vet).build());
    }

    public void recordByPerson(HealthRecord antes, Person person) {
        healthRecordCorrectionRepository.save(snapshot(antes).correctedByPerson(person).build());
    }

    /** Da correcao mais recente para a mais antiga. */
    public List<HealthRecordCorrectionResponseDTO> list(UUID healthRecordId) {
        return healthRecordCorrectionRepository
                .findByHealthRecordHealthRecordIdOrderByCorrectedAtDesc(healthRecordId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Precisa ser chamado ANTES dos setters, senao grava o estado novo. */
    private HealthRecordCorrection.HealthRecordCorrectionBuilder snapshot(HealthRecord antes) {
        return HealthRecordCorrection.builder()
                .healthRecord(antes)
                .previousEventType(antes.getEventType())
                .previousEventDate(antes.getEventDate())
                .previousDescription(antes.getDescription())
                .correctedAt(LocalDateTime.now());
    }

    private HealthRecordCorrectionResponseDTO toResponse(HealthRecordCorrection correction) {
        boolean porVet = correction.getCorrectedByVet() != null;

        return HealthRecordCorrectionResponseDTO.builder()
                .healthRecordCorrectionId(correction.getHealthRecordCorrectionId())
                .correctedAt(correction.getCorrectedAt())
                .correctedByRole(porVet ? UserRole.VET.name() : UserRole.OWNER.name())
                .correctedByName(porVet
                        ? correction.getCorrectedByVet().getName()
                        : nomeDoTutor(correction))
                .correctedByClinicName(porVet ? correction.getCorrectedByVet().getClinic().getName() : null)
                .previousEventType(correction.getPreviousEventType())
                .previousEventDate(correction.getPreviousEventDate())
                .previousDescription(correction.getPreviousDescription())
                .build();
    }

    private String nomeDoTutor(HealthRecordCorrection correction) {
        return correction.getCorrectedByPerson() != null ? correction.getCorrectedByPerson().getName() : null;
    }

}

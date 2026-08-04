package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import br.com.petfy.healthcare.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rastro de correcoes de um registro de vacina: grava e le.
 *
 * Escrita e leitura ficam juntas de proposito. Quem grava define o formato do
 * que e guardado, e separar em dois componentes so cria a chance de a leitura
 * interpretar diferente do que a escrita quis dizer.
 *
 * Usado pelo tutor e pelo veterinario: se so um dos dois deixasse rastro, o
 * historico contaria meia verdade e seria pior que nao ter historico nenhum -
 * daria a impressao de completude.
 */
@Component
@RequiredArgsConstructor
public class VaccineCorrectionLog {

    private final VaccineCorrectionRepository vaccineCorrectionRepository;

    public void recordByVet(Vaccine antes, Vet vet) {
        vaccineCorrectionRepository.save(snapshot(antes).correctedByVet(vet).build());
    }

    public void recordByOwner(Vaccine antes, Owner owner) {
        vaccineCorrectionRepository.save(snapshot(antes).correctedByOwner(owner).build());
    }

    /** Da correcao mais recente para a mais antiga. */
    public List<VaccineCorrectionResponseDTO> list(UUID vaccineId) {
        return vaccineCorrectionRepository.findByVaccineVaccineIdOrderByCorrectedAtDesc(vaccineId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Precisa ser chamado ANTES dos setters, senao grava o estado novo. */
    private VaccineCorrection.VaccineCorrectionBuilder snapshot(Vaccine antes) {
        return VaccineCorrection.builder()
                .vaccine(antes)
                .previousVaccineName(antes.getVaccineName())
                .previousApplicationDate(antes.getApplicationDate())
                .previousNextDoseDate(antes.getNextDoseDate())
                .previousDescription(antes.getDescription())
                .correctedAt(LocalDateTime.now());
    }

    private VaccineCorrectionResponseDTO toResponse(VaccineCorrection correction) {
        boolean porVet = correction.getCorrectedByVet() != null;

        return VaccineCorrectionResponseDTO.builder()
                .vaccineCorrectionId(correction.getVaccineCorrectionId())
                .correctedAt(correction.getCorrectedAt())
                .correctedByRole(porVet ? UserRole.VET.name() : UserRole.OWNER.name())
                .correctedByName(porVet
                        ? correction.getCorrectedByVet().getName()
                        : nomeDoTutor(correction))
                .correctedByClinicName(porVet ? correction.getCorrectedByVet().getClinic().getName() : null)
                .previousVaccineName(correction.getPreviousVaccineName())
                .previousApplicationDate(correction.getPreviousApplicationDate())
                .previousNextDoseDate(correction.getPreviousNextDoseDate())
                .previousDescription(correction.getPreviousDescription())
                .build();
    }

    private String nomeDoTutor(VaccineCorrection correction) {
        return correction.getCorrectedByOwner() != null ? correction.getCorrectedByOwner().getName() : null;
    }

}

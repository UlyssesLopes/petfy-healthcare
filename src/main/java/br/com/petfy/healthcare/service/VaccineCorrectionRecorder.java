package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Grava o estado anterior antes de a vacina ser alterada.
 *
 * Chamado tanto pelo tutor quanto pelo veterinario: se so um dos dois deixasse
 * rastro, o historico contaria meia verdade e seria pior que nao ter historico
 * nenhum - daria a impressao de completude.
 */
@Component
@RequiredArgsConstructor
public class VaccineCorrectionRecorder {

    private final VaccineCorrectionRepository vaccineCorrectionRepository;

    public void recordByVet(Vaccine antes, Vet vet) {
        vaccineCorrectionRepository.save(snapshot(antes).correctedByVet(vet).build());
    }

    public void recordByOwner(Vaccine antes, Owner owner) {
        vaccineCorrectionRepository.save(snapshot(antes).correctedByOwner(owner).build());
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

}

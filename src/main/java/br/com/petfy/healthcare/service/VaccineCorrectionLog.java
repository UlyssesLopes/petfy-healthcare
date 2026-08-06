package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCorrection;
import br.com.petfy.healthcare.domain.repository.VaccineCorrectionRepository;
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

    /**
     * Correcao feita por alguem agindo em nome de uma organizacao.
     *
     * A clinica nao e um detalhe da resposta: e ela que o tutor autorizou, e e ela
     * que responde institucionalmente pelo que foi escrito.
     */
    public void recordByProfessional(Vaccine antes, Person profissional, Clinic clinic) {
        vaccineCorrectionRepository.save(snapshot(antes)
                .correctedBy(profissional)
                .correctedInClinic(clinic)
                .build());
    }

    /** Correcao feita pela pessoa agindo por si, sem organizacao atras. */
    public void recordByPerson(Vaccine antes, Person person) {
        vaccineCorrectionRepository.save(snapshot(antes).correctedBy(person).build());
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

    /**
     * O papel saiu da resposta, e o contexto entrou no lugar.
     *
     * Antes havia {@code correctedByRole} com OWNER ou VET, inferido de qual das
     * duas colunas estava preenchida. Papel deixou de existir, e o que separa os
     * dois casos e outra coisa: em nome de quem a pessoa agiu. Quem le distingue
     * "a Ana corrigiu" de "a Ana, pela Clinica Norte, corrigiu" por
     * {@code correctedByClinicName} vir ou nao vazio.
     */
    private VaccineCorrectionResponseDTO toResponse(VaccineCorrection correction) {
        return VaccineCorrectionResponseDTO.builder()
                .vaccineCorrectionId(correction.getVaccineCorrectionId())
                .correctedAt(correction.getCorrectedAt())
                .correctedByName(correction.getCorrectedBy() != null
                        ? correction.getCorrectedBy().getName()
                        : null)
                .correctedByClinicName(correction.getCorrectedInClinic() != null
                        ? correction.getCorrectedInClinic().getName()
                        : null)
                .previousVaccineName(correction.getPreviousVaccineName())
                .previousApplicationDate(correction.getPreviousApplicationDate())
                .previousNextDoseDate(correction.getPreviousNextDoseDate())
                .previousDescription(correction.getPreviousDescription())
                .build();
    }

}

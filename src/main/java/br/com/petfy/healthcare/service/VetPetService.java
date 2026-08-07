package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Lado do veterinario: o que ele alcanca dos animals que a clinica dele atende. */
public interface VetPetService {

    /**
     * Os animais que o contexto ativo alcanca, com recorte.
     *
     * <b>Paginada, e nao lista.</b> Esta e a leitura da area de organizacao, que le
     * centenas - a unica cobranca que a secao 9.5 do PRODUTO.md ja dava como certa.
     * As vizinhas ({@code /animals}, {@code /organizations}, {@code /vaccines},
     * {@code /timeline}) ja eram {@code Page}; esta era a unica que continuava array.
     *
     * @param busca casa com nome, registro geral e microchip. Nulo ou em branco nao
     *              filtra nada
     */
    Page<VetPetDTO> listAccessibleAnimals(String busca, Pageable pageable);

    List<VaccineResponseDTO> listVaccines(UUID animalId);

    VaccineResponseDTO registerVaccine(UUID animalId, VaccineRequestDTO request);

    /** Corrige um registro da propria clinica, dentro da janela de correcao. */
    VaccineResponseDTO correctVaccine(UUID animalId, UUID vaccineId, VaccineRequestDTO request);

    List<VaccineCorrectionResponseDTO> listCorrections(UUID animalId, UUID vaccineId);

    List<HealthRecordResponseDTO> listHealthRecords(UUID animalId);

    HealthRecordResponseDTO registerHealthRecord(UUID animalId, HealthRecordRequestDTO request);

    /** Corrige um atendimento da propria clinica, dentro da janela de correcao. */
    HealthRecordResponseDTO correctHealthRecord(UUID animalId, UUID healthRecordId, HealthRecordRequestDTO request);

    List<HealthRecordCorrectionResponseDTO> listHealthRecordCorrections(UUID animalId, UUID healthRecordId);

}

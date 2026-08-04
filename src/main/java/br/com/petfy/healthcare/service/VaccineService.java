package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineAgendaResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;

import java.util.List;
import java.util.UUID;

public interface VaccineService {

    /** Vacinas do tutor que pedem acao: vencidas, ou vencendo dentro da janela. */
    VaccineAgendaResponseDTO getAgenda(int windowDays);

    VaccineResponseDTO createVaccine(VaccineRequestDTO request);

    VaccineResponseDTO updateVaccine(UUID id, VaccineRequestDTO request);

    void deleteVaccine(UUID id);

    VaccineResponseDTO getVaccineById(UUID id);

    List<VaccineResponseDTO> listAllVaccines();

    /** Rastro de alteracoes de um registro do proprio tutor. */
    List<VaccineCorrectionResponseDTO> listCorrections(UUID vaccineId);

}

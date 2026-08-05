package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineAgendaItemDTO;
import br.com.petfy.healthcare.domain.dto.VaccineAgendaResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.VaccineCorrectionLog;
import br.com.petfy.healthcare.service.VaccineFactory;
import br.com.petfy.healthcare.service.VaccineService;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class VaccineServiceImpl implements VaccineService {

    private final VaccineRepository vaccineRepository;

    private final PetRepository petRepository;

    private final ClinicRepository clinicRepository;

    private final CurrentOwnerProvider currentOwnerProvider;
    private final PetAccessGuard petAccessGuard;

    private final VaccineStatusCalculator vaccineStatusCalculator;

    private final VaccineFactory vaccineFactory;

    private final VaccineCorrectionLog vaccineCorrectionLog;

    @Override
    public VaccineResponseDTO createVaccine(VaccineRequestDTO request) {
        Pet pet = petAccessGuard.requireEscrita(request.getPetId());

        Clinic clinic = request.getClinicId() != null
                ? clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND))
                : null;

        return toResponse(vaccineRepository.save(vaccineFactory.build(pet, clinic, request)));
    }

    @Override
    public VaccineResponseDTO updateVaccine(UUID id, VaccineRequestDTO request) {
        Vaccine existing = buscarDoOwnerAutenticado(id);

        // o snapshot sai antes dos setters. O tutor nao tem janela de correcao -
        // a carteira e dele - mas deixa rastro igual: se so o veterinario
        // registrasse, o historico contaria meia verdade
        vaccineCorrectionLog.recordByOwner(existing, currentOwnerProvider.require());

        if (request.getVaccineName() != null) existing.setVaccineName(request.getVaccineName());
        if (request.getApplicationDate() != null) existing.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) existing.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());

        if (request.getPetId() != null) {
            existing.setPet(petAccessGuard.requireEscrita(request.getPetId()));
        }

        if (request.getClinicId() != null) {
            Clinic clinic = clinicRepository.findById(request.getClinicId())
                    .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
            existing.setClinic(clinic);
        }

        existing.setUpdateDate(LocalDateTime.now());

        return toResponse(vaccineRepository.save(existing));
    }

    @Override
    public List<VaccineCorrectionResponseDTO> listCorrections(UUID vaccineId) {
        // passa pela mesma checagem de propriedade da leitura da vacina: o rastro
        // e tao do tutor quanto o registro
        buscarDoOwnerAutenticado(vaccineId);

        return vaccineCorrectionLog.list(vaccineId);
    }

    @Override
    public void deleteVaccine(UUID id) {
        vaccineRepository.delete(buscarDoOwnerAutenticado(id));
    }

    @Override
    public VaccineResponseDTO getVaccineById(UUID id) {
        return toResponse(buscarDoOwnerAutenticado(id));
    }

    @Override
    public Page<VaccineResponseDTO> listAllVaccines(Pageable pageable) {
        return vaccineRepository.findByPetTutorsOwnerOwnerId(currentOwnerProvider.require().getOwnerId(), pageable)
                .map(this::toResponse);
    }

    @Override
    public VaccineAgendaResponseDTO getAgenda(int windowDays) {
        LocalDate hoje = LocalDate.now();

        // filtra em memoria de proposito: a agenda cobre as vacinas de um tutor,
        // que sao poucas, e assim a classificacao inteira fica testavel sem banco
        List<Vaccine> doTutor = vaccineRepository.findByPetTutorsOwnerOwnerId(currentOwnerProvider.require().getOwnerId());

        Map<VaccineStatus, List<VaccineAgendaItemDTO>> porStatus = doTutor.stream()
                .map(vaccine -> toAgendaItem(vaccine, hoje, windowDays))
                .collect(Collectors.groupingBy(VaccineAgendaItemDTO::getStatus));

        List<VaccineAgendaItemDTO> acionaveis = Stream.concat(
                        porStatus.getOrDefault(VaccineStatus.OVERDUE, List.of()).stream(),
                        porStatus.getOrDefault(VaccineStatus.DUE_SOON, List.of()).stream())
                .sorted(Comparator.comparing(VaccineAgendaItemDTO::getNextDoseDate))
                .collect(Collectors.toList());

        return VaccineAgendaResponseDTO.builder()
                .referenceDate(hoje)
                .windowDays(windowDays)
                .overdueCount(porStatus.getOrDefault(VaccineStatus.OVERDUE, List.of()).size())
                .dueSoonCount(porStatus.getOrDefault(VaccineStatus.DUE_SOON, List.of()).size())
                .upToDateCount(porStatus.getOrDefault(VaccineStatus.UP_TO_DATE, List.of()).size())
                .withoutNextDoseCount(porStatus.getOrDefault(VaccineStatus.NO_NEXT_DOSE, List.of()).size())
                .items(acionaveis)
                .build();
    }

    private VaccineAgendaItemDTO toAgendaItem(Vaccine vaccine, LocalDate hoje, int windowDays) {
        LocalDate proximaDose = vaccine.getNextDoseDate();

        VaccineStatus status = vaccineStatusCalculator.classify(proximaDose, hoje, windowDays);
        Long diasAte = proximaDose != null ? ChronoUnit.DAYS.between(hoje, proximaDose) : null;

        return VaccineAgendaItemDTO.builder()
                .vaccineId(vaccine.getVaccineId())
                .vaccineName(vaccine.getVaccineName())
                .petId(vaccine.getPet().getPetId())
                .petName(vaccine.getPet().getName())
                .applicationDate(vaccine.getApplicationDate())
                .nextDoseDate(proximaDose)
                .status(status)
                .daysUntilNextDose(diasAte)
                .build();
    }

    /**
     * Vacina de pet de outro dono responde VACCINE_NOT_FOUND, e nao 403: um 403
     * confirmaria que aquele id existe.
     */
    private Vaccine buscarDoOwnerAutenticado(UUID vaccineId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return vaccineRepository.findById(vaccineId)
                .filter(vaccine -> petAccessGuard.alcanca(vaccine.getPet().getPetId()))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VaccineResponseDTO toResponse(Vaccine vaccine) {
        return VaccineResponseDTO.builder()
                .vaccineId(vaccine.getVaccineId())
                .vaccineName(vaccine.getVaccineName())
                .applicationDate(vaccine.getApplicationDate())
                .nextDoseDate(vaccine.getNextDoseDate())
                .description(vaccine.getDescription())
                .petId(vaccine.getPet().getPetId())
                .clinicId(vaccine.getClinic() != null ? vaccine.getClinic().getClinicId() : null)
                .vaccineCatalogId(vaccine.getCatalog() != null ? vaccine.getCatalog().getVaccineCatalogId() : null)
                .creationDate(vaccine.getCreationDate())
                .updateDate(vaccine.getUpdateDate())
                .build();
    }


}

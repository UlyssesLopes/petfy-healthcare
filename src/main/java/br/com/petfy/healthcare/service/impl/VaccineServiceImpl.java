package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineAgendaItemDTO;
import br.com.petfy.healthcare.domain.dto.VaccineAgendaResponseDTO;
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
import br.com.petfy.healthcare.service.VaccineService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    private final VaccineCatalogRepository vaccineCatalogRepository;

    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    public VaccineResponseDTO createVaccine(VaccineRequestDTO request) {
        Pet pet = buscarPetDoOwnerAutenticado(request.getPetId());

        Clinic clinic = request.getClinicId() != null
                ? clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND))
                : null;

        VaccineCatalog catalog = request.getVaccineCatalogId() != null
                ? buscarNoCatalogo(request.getVaccineCatalogId())
                : null;

        String nome = resolverNome(request, catalog);

        Vaccine vaccine = Vaccine.builder()
                .pet(pet)
                .catalog(catalog)
                .vaccineName(nome)
                .applicationDate(request.getApplicationDate())
                .nextDoseDate(resolverProximaDose(request, catalog))
                .description(request.getDescription())
                .clinic(clinic)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        return toResponse(vaccineRepository.save(vaccine));
    }

    /**
     * O nome vem do catalogo quando a vacina e escolhida da lista. Continua
     * obrigatorio quando o tutor digita em texto livre - por isso a validacao
     * esta aqui e nao como @NotBlank no DTO, que nao enxerga essa condicao.
     */
    private String resolverNome(VaccineRequestDTO request, VaccineCatalog catalog) {
        if (request.getVaccineName() != null && !request.getVaccineName().isBlank()) {
            return request.getVaccineName();
        }

        if (catalog != null) {
            return catalog.getName();
        }

        throw new PetfyHealthcareException(
                "vaccineName e obrigatorio quando vaccineCatalogId nao e informado",
                ErrorMessageEnum.INVALID_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Calcula a proxima dose somando o intervalo de reforco do catalogo a data de
     * aplicacao - o ponto do catalogo e o tutor nao ter que estimar isso. Data
     * enviada explicitamente sempre vence, para o caso de orientacao diferente
     * do veterinario.
     */
    private LocalDate resolverProximaDose(VaccineRequestDTO request, VaccineCatalog catalog) {
        if (request.getNextDoseDate() != null) {
            return request.getNextDoseDate();
        }

        if (catalog == null || catalog.getDefaultIntervalDays() == null || request.getApplicationDate() == null) {
            return null;
        }

        return request.getApplicationDate().plusDays(catalog.getDefaultIntervalDays());
    }

    private VaccineCatalog buscarNoCatalogo(UUID vaccineCatalogId) {
        return vaccineCatalogRepository.findById(vaccineCatalogId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    @Override
    public VaccineResponseDTO updateVaccine(UUID id, VaccineRequestDTO request) {
        Vaccine existing = buscarDoOwnerAutenticado(id);

        if (request.getVaccineName() != null) existing.setVaccineName(request.getVaccineName());
        if (request.getApplicationDate() != null) existing.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) existing.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());

        if (request.getPetId() != null) {
            existing.setPet(buscarPetDoOwnerAutenticado(request.getPetId()));
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
    public void deleteVaccine(UUID id) {
        vaccineRepository.delete(buscarDoOwnerAutenticado(id));
    }

    @Override
    public VaccineResponseDTO getVaccineById(UUID id) {
        return toResponse(buscarDoOwnerAutenticado(id));
    }

    @Override
    public List<VaccineResponseDTO> listAllVaccines() {
        return vaccineRepository.findByPetOwnerOwnerId(currentOwnerProvider.require().getOwnerId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VaccineAgendaResponseDTO getAgenda(int windowDays) {
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(windowDays);

        // filtra em memoria de proposito: a agenda cobre as vacinas de um tutor,
        // que sao poucas, e assim a classificacao inteira fica testavel sem banco
        List<Vaccine> doTutor = vaccineRepository.findByPetOwnerOwnerId(currentOwnerProvider.require().getOwnerId());

        Map<VaccineStatus, List<VaccineAgendaItemDTO>> porStatus = doTutor.stream()
                .map(vaccine -> toAgendaItem(vaccine, hoje, limite))
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

    private VaccineAgendaItemDTO toAgendaItem(Vaccine vaccine, LocalDate hoje, LocalDate limite) {
        LocalDate proximaDose = vaccine.getNextDoseDate();

        VaccineStatus status;
        Long diasAte = null;

        if (proximaDose == null) {
            status = VaccineStatus.NO_NEXT_DOSE;
        } else {
            diasAte = ChronoUnit.DAYS.between(hoje, proximaDose);
            // vencer hoje conta como vencendo, nao como vencido
            if (proximaDose.isBefore(hoje)) {
                status = VaccineStatus.OVERDUE;
            } else if (!proximaDose.isAfter(limite)) {
                status = VaccineStatus.DUE_SOON;
            } else {
                status = VaccineStatus.UP_TO_DATE;
            }
        }

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
                .filter(vaccine -> vaccine.getPet().getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Pet buscarPetDoOwnerAutenticado(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
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

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.VaccineService;
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
public class VaccineServiceImpl implements VaccineService {

    private final VaccineRepository vaccineRepository;

    private final PetRepository petRepository;

    private final ClinicRepository clinicRepository;

    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    public VaccineResponseDTO createVaccine(VaccineRequestDTO request) {
        Pet pet = buscarPetDoOwnerAutenticado(request.getPetId());

        Clinic clinic = request.getClinicId() != null
                ? clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND))
                : null;

        Vaccine vaccine = Vaccine.builder()
                .pet(pet)
                .vaccineName(request.getVaccineName())
                .applicationDate(request.getApplicationDate())
                .nextDoseDate(request.getNextDoseDate())
                .description(request.getDescription())
                .clinic(clinic)
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        return toResponse(vaccineRepository.save(vaccine));
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
                .creationDate(vaccine.getCreationDate())
                .updateDate(vaccine.getUpdateDate())
                .build();
    }


}

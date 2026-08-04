package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.VaccineRecordedNotifier;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.VaccineFactory;
import br.com.petfy.healthcare.service.VetPetService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VetPetServiceImpl implements VetPetService {

    private final PetClinicAccessRepository petClinicAccessRepository;
    private final VaccineRepository vaccineRepository;
    private final CurrentVetProvider currentVetProvider;
    private final VaccineFactory vaccineFactory;
    private final VaccineRecordedNotifier vaccineRecordedNotifier;

    @Override
    public List<VetPetDTO> listAccessiblePets() {
        return acessosAtivosDaClinica()
                .stream()
                .map(this::toVetPet)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaccineResponseDTO> listVaccines(UUID petId) {
        exigirAcessoAoPet(petId);

        return vaccineRepository.findByPetPetIdOrderByApplicationDateDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VaccineResponseDTO registerVaccine(UUID petId, VaccineRequestDTO request) {
        Vet vet = currentVetProvider.require();
        Pet pet = exigirAcessoAoPet(petId).getPet();

        // a clinica vem do vet autenticado, nunca do payload: aceitar clinicId do
        // cliente deixaria um vet registrar vacina em nome de outra clinica
        Vaccine vaccine = vaccineRepository.save(vaccineFactory.build(pet, vet.getClinic(), request));

        // o tutor precisa saber o que a clinica escreveu no pet dele
        vaccineRecordedNotifier.notifyOwner(vaccine);

        return toResponse(vaccine);
    }

    private List<PetClinicAccess> acessosAtivosDaClinica() {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();
        return petClinicAccessRepository.findByClinicClinicIdAndRevokedAtIsNull(clinicId);
    }

    /**
     * Pet sem concessao ativa para a clinica do vet responde PET_NOT_FOUND, e nao
     * 403: para o veterinario, um pet que a clinica dele nao atende e
     * indistinguivel de um pet que nao existe.
     */
    private PetClinicAccess exigirAcessoAoPet(UUID petId) {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();

        return petClinicAccessRepository.findByPetPetIdAndClinicClinicId(petId, clinicId)
                .filter(PetClinicAccess::isActive)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VetPetDTO toVetPet(PetClinicAccess access) {
        Pet pet = access.getPet();

        return VetPetDTO.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .bornDate(pet.getBornDate())
                .gender(pet.getGender())
                .weight(pet.getWeight())
                .ownerName(pet.getOwner().getName())
                .accessGrantedAt(access.getGrantedAt())
                .build();
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

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.PetService;
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
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    public PetResponseDTO createPet(PetRequestDTO dto) {
        Owner owner = currentOwnerProvider.require();

        Pet pet = Pet.builder()
                .name(dto.getName())
                .type(dto.getType())
                .breed(dto.getBreed())
                .bornDate(dto.getBornDate())
                .weight(dto.getWeight())
                .gender(dto.getGender())
                .owner(owner)
                .creationDate(LocalDateTime.now())
                .build();

        return toResponse(petRepository.save(pet));
    }

    @Override
    public PetResponseDTO getPetById(UUID petId) {
        return toResponse(buscarDoOwnerAutenticado(petId));
    }

    @Override
    public List<PetResponseDTO> listAllPets() {
        return petRepository.findByOwnerOwnerId(currentOwnerProvider.require().getOwnerId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PetResponseDTO updatePet(UUID petId, PetRequestDTO dto) {
        Pet pet = buscarDoOwnerAutenticado(petId);

        pet.setName(dto.getName() != null ? dto.getName() : pet.getName());
        pet.setType(dto.getType() != null ? dto.getType() : pet.getType());
        pet.setBreed(dto.getBreed() != null ? dto.getBreed() : pet.getBreed());
        pet.setBornDate(dto.getBornDate() != null ? dto.getBornDate() : pet.getBornDate());
        pet.setWeight(dto.getWeight() != null ? dto.getWeight() : pet.getWeight());
        pet.setGender(dto.getGender() != null ? dto.getGender() : pet.getGender());
        pet.setUpdateDate(LocalDateTime.now());

        return toResponse(petRepository.save(pet));
    }

    @Override
    public void deletePet(UUID petId) {
        petRepository.delete(buscarDoOwnerAutenticado(petId));
    }

    /**
     * Pet de outro dono responde PET_NOT_FOUND, e nao 403: um 403 confirmaria
     * que aquele id existe, o que permitiria varrer ids para descobrir o que ha
     * na base.
     */
    private Pet buscarDoOwnerAutenticado(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private PetResponseDTO toResponse(Pet pet) {
        return PetResponseDTO.builder()
                .petId(pet.getPetId())
                .name(pet.getName())
                .type(pet.getType())
                .breed(pet.getBreed())
                .bornDate(pet.getBornDate())
                .weight(pet.getWeight())
                .gender(pet.getGender())
                .ownerId(pet.getOwner().getOwnerId())
                .creationDate(pet.getCreationDate())
                .updateDate(pet.getUpdateDate())
                .build();
    }

}

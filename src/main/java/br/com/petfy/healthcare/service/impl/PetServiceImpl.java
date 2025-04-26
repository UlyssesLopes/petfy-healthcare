package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
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
    private final OwnerRepository ownerRepository;

    @Override
    public PetResponseDTO createPet(PetRequestDTO dto) {
        Owner owner = ownerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

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

        Pet saved = petRepository.save(pet);
        return toResponse(saved);
    }

    @Override
    public PetResponseDTO getPetById(UUID petId) {
        return toResponse(petRepository.findById(petId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.PET_NOT_FOUND.getMessage(), ErrorMessageEnum.PET_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND)));
    }

    @Override
    public List<PetResponseDTO> listAllPets() {
        return petRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PetResponseDTO updatePet(UUID petId, PetRequestDTO dto) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.PET_NOT_FOUND.getMessage(), ErrorMessageEnum.PET_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        pet.setName(dto.getName() != null ? dto.getName() : pet.getName());
        pet.setType(dto.getType() != null ? dto.getType() : pet.getType());
        pet.setBreed(dto.getBreed() != null ? dto.getBreed() : pet.getBreed());
        pet.setBornDate(dto.getBornDate() != null ? dto.getBornDate() : pet.getBornDate());
        pet.setWeight(dto.getWeight() != null ? dto.getWeight() : pet.getWeight());
        pet.setGender(dto.getGender() != null ? dto.getGender() : pet.getGender());

        if (dto.getOwnerId() != null) {
            Owner newOwner = ownerRepository.findById(dto.getOwnerId())
                    .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
            pet.setOwner(newOwner);
        }

        pet.setUpdateDate(LocalDateTime.now());

        return toResponse(petRepository.save(pet));
    }

    @Override
    public void deletePet(UUID petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.PET_NOT_FOUND.getMessage(), ErrorMessageEnum.PET_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
        petRepository.delete(pet);
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

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.OwnerService;
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
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;

    @Override
    public OwnerResponseDTO createOwner(OwnerRequestDTO request) {

        Owner owner = Owner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .address(request.getAddress())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        Owner savedOwner = ownerRepository.save(owner);

        return toResponseDTO(savedOwner);
    }

    @Override
    public List<OwnerResponseDTO> listAllOwners() {
        return ownerRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OwnerResponseDTO getOwnerById(UUID ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
        return toResponseDTO(owner);
    }

    @Override
    public OwnerResponseDTO updateOwner(UUID ownerId, OwnerRequestDTO request) {
        Owner existingOwner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        if (request.getName() != null) {
            existingOwner.setName(request.getName());
        }

        if (request.getEmail() != null) {
            existingOwner.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            existingOwner.setPhone(request.getPhone());
        }

        if (request.getAddress() != null) {
            existingOwner.setAddress(request.getAddress());
        }

        existingOwner.setUpdateDate(LocalDateTime.now());

        Owner updatedOwner = ownerRepository.save(existingOwner);
        return toResponseDTO(updatedOwner);
    }

    @Override
    public void deleteOwner(UUID ownerId) {
        if (!ownerRepository.existsById(ownerId)) {
            throw new PetfyHealthcareException(ErrorMessageEnum.OWNER_NOT_FOUND.getMessage(), ErrorMessageEnum.OWNER_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }
        ownerRepository.deleteById(ownerId);
    }

    private OwnerResponseDTO toResponseDTO(Owner owner) {
        return OwnerResponseDTO.builder()
                .ownerId(owner.getOwnerId())
                .name(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .address(owner.getAddress())
                .creationDate(owner.getCreationDate())
                .updateDate(owner.getUpdateDate())
                .build();
    }

}

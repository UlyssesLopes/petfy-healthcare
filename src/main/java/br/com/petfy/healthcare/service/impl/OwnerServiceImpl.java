package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.repository.OwnerRepository;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private final OwnerRepository ownerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    public OwnerResponseDTO createOwner(OwnerRequestDTO request) {

        Owner owner = Owner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .address(request.getAddress())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        return toResponseDTO(ownerRepository.save(owner));
    }

    @Override
    public OwnerResponseDTO getCurrentOwner() {
        return toResponseDTO(currentOwnerProvider.require());
    }

    @Override
    public OwnerResponseDTO updateCurrentOwner(OwnerRequestDTO request) {
        Owner existingOwner = currentOwnerProvider.require();

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

        // password fica de fora de proposito: troca de senha pede endpoint
        // proprio, com confirmacao da senha atual
        existingOwner.setUpdateDate(LocalDateTime.now());

        return toResponseDTO(ownerRepository.save(existingOwner));
    }

    @Override
    public void deleteCurrentOwner() {
        ownerRepository.delete(currentOwnerProvider.require());
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

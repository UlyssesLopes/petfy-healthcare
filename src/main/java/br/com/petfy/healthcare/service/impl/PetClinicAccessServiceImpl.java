package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicAccessResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.PetClinicAccess;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.domain.repository.PetClinicAccessRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.PetAccessGuard;
import br.com.petfy.healthcare.service.PetClinicAccessService;
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
public class PetClinicAccessServiceImpl implements PetClinicAccessService {

    private final PetClinicAccessRepository petClinicAccessRepository;
    private final ClinicRepository clinicRepository;
    private final PetAccessGuard petAccessGuard;

    @Override
    public ClinicAccessResponseDTO grant(UUID petId, ClinicAccessRequestDTO request) {
        Pet pet = petAccessGuard.requireEscrita(petId);
        Clinic clinic = buscarClinica(request.getClinicId());

        // reconceder reativa a linha existente: a chave unica impede duplicar o
        // par, e o tutor que revogou e mudou de ideia espera simplesmente voltar
        // a ter acesso liberado
        PetClinicAccess access = petClinicAccessRepository
                .findByPetPetIdAndClinicClinicId(petId, request.getClinicId())
                .orElseGet(() -> PetClinicAccess.builder().pet(pet).clinic(clinic).build());

        access.setRevokedAt(null);
        access.setGrantedAt(LocalDateTime.now());

        return toResponse(petClinicAccessRepository.save(access));
    }

    @Override
    public List<ClinicAccessResponseDTO> list(UUID petId) {
        petAccessGuard.requireEscrita(petId);

        return petClinicAccessRepository.findByPetPetIdOrderByGrantedAtDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void revoke(UUID petId, UUID clinicId) {
        petAccessGuard.requireEscrita(petId);

        PetClinicAccess access = petClinicAccessRepository
                .findByPetPetIdAndClinicClinicId(petId, clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_ACCESS_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_ACCESS_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (access.isActive()) {
            access.setRevokedAt(LocalDateTime.now());
            petClinicAccessRepository.save(access);
        }
    }

    private Clinic buscarClinica(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private ClinicAccessResponseDTO toResponse(PetClinicAccess access) {
        return ClinicAccessResponseDTO.builder()
                .petClinicAccessId(access.getPetClinicAccessId())
                .petId(access.getPet().getPetId())
                .clinicId(access.getClinic().getClinicId())
                .clinicName(access.getClinic().getName())
                .grantedAt(access.getGrantedAt())
                .revokedAt(access.getRevokedAt())
                .active(access.isActive())
                .build();
    }

}

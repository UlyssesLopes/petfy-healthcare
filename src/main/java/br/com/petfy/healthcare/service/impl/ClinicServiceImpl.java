package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.ClinicService;
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
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;

    @Override
    public ClinicResponseDTO createClinic(ClinicRequestDTO request) {
        Clinic clinic = Clinic.builder()
                .name(request.getName())
                .ownerVetName(request.getOwnerVetName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .cnpj(request.getCnpj())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .cep(request.getCep())
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .build();

        return toResponse(clinicRepository.save(clinic));
    }

    @Override
    public ClinicResponseDTO getClinicById(UUID clinicId) {
        return clinicRepository.findById(clinicId)
                .map(this::toResponse)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
    }

    @Override
    public List<ClinicResponseDTO> listAllClinics() {
        return clinicRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClinicResponseDTO updateClinic(UUID clinicId, ClinicRequestDTO request) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        clinic.setName(request.getName() != null ? request.getName() : clinic.getName());
        clinic.setOwnerVetName(request.getOwnerVetName() != null ? request.getOwnerVetName() : clinic.getOwnerVetName());
        clinic.setPhone(request.getPhone() != null ? request.getPhone() : clinic.getPhone());
        clinic.setEmail(request.getEmail() != null ? request.getEmail() : clinic.getEmail());
        clinic.setCnpj(request.getCnpj() != null ? request.getCnpj() : clinic.getCnpj());
        clinic.setAddress(request.getAddress() != null ? request.getAddress() : clinic.getAddress());
        clinic.setCity(request.getCity() != null ? request.getCity() : clinic.getCity());
        clinic.setState(request.getState() != null ? request.getState() : clinic.getState());
        clinic.setCep(request.getCep() != null ? request.getCep() : clinic.getCep());
        clinic.setDescription(request.getDescription() != null ? request.getDescription() : clinic.getDescription());
        clinic.setUpdateDate(LocalDateTime.now());

        return toResponse(clinicRepository.save(clinic));
    }

    @Override
    public void deleteClinic(UUID clinicId) {
        if (!clinicRepository.existsById(clinicId)) {
            throw new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }
        clinicRepository.deleteById(clinicId);
    }

    private ClinicResponseDTO toResponse(Clinic clinic) {
        return ClinicResponseDTO.builder()
                .clinicId(clinic.getClinicId())
                .name(clinic.getName())
                .ownerVetName(clinic.getOwnerVetName())
                .phone(clinic.getPhone())
                .email(clinic.getEmail())
                .cnpj(clinic.getCnpj())
                .address(clinic.getAddress())
                .city(clinic.getCity())
                .state(clinic.getState())
                .cep(clinic.getCep())
                .description(clinic.getDescription())
                .creationDate(clinic.getCreationDate())
                .updateDate(clinic.getUpdateDate())
                .build();
    }

}

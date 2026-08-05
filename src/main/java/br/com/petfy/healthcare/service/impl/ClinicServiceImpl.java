package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.repository.ClinicRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.service.ClinicService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;

    private final CurrentVetProvider currentVetProvider;

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
    public Page<ClinicResponseDTO> listAllClinics(Pageable pageable) {
        return clinicRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public ClinicResponseDTO updateClinic(UUID clinicId, ClinicRequestDTO request) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        exigirVetDaClinica(clinicId);

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

        exigirVetDaClinica(clinicId);

        clinicRepository.deleteById(clinicId);
    }

    /**
     * Leitura e criacao seguem abertas a qualquer autenticado - o tutor precisa
     * consultar clinicas e registrar onde vacinou. Alterar e remover, nao: quem
     * mantem o cadastro de uma clinica e quem trabalha nela.
     *
     * Responde 403 e nao 404 de proposito. Aqui, diferente dos recursos do
     * tutor, a existencia da clinica nao e segredo: ela ja aparece na listagem
     * publica, entao esconder o motivo so confundiria.
     */
    private void exigirVetDaClinica(UUID clinicId) {
        UUID clinicaDoVet = currentVetProvider.require().getClinic().getClinicId();

        if (!clinicaDoVet.equals(clinicId)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getMessage(),
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getCode(),
                    HttpStatus.FORBIDDEN);
        }
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

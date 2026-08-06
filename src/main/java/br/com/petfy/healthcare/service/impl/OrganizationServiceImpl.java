package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationResponseDTO;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.OrganizationService;
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
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    private final CurrentProfessionalProvider currentProfessionalProvider;

    @Override
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO request) {
        Organization organization = Organization.builder()
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

        return toResponse(organizationRepository.save(organization));
    }

    @Override
    public OrganizationResponseDTO getOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<OrganizationResponseDTO> listAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public OrganizationResponseDTO updateOrganization(UUID organizationId, OrganizationRequestDTO request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        exigirVetDaClinica(organizationId);

        organization.setName(request.getName() != null ? request.getName() : organization.getName());
        organization.setOwnerVetName(request.getOwnerVetName() != null ? request.getOwnerVetName() : organization.getOwnerVetName());
        organization.setPhone(request.getPhone() != null ? request.getPhone() : organization.getPhone());
        organization.setEmail(request.getEmail() != null ? request.getEmail() : organization.getEmail());
        organization.setCnpj(request.getCnpj() != null ? request.getCnpj() : organization.getCnpj());
        organization.setAddress(request.getAddress() != null ? request.getAddress() : organization.getAddress());
        organization.setCity(request.getCity() != null ? request.getCity() : organization.getCity());
        organization.setState(request.getState() != null ? request.getState() : organization.getState());
        organization.setCep(request.getCep() != null ? request.getCep() : organization.getCep());
        organization.setDescription(request.getDescription() != null ? request.getDescription() : organization.getDescription());
        organization.setUpdateDate(LocalDateTime.now());

        return toResponse(organizationRepository.save(organization));
    }

    @Override
    public void deleteOrganization(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }

        exigirVetDaClinica(organizationId);

        organizationRepository.deleteById(organizationId);
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
    private void exigirVetDaClinica(UUID organizationId) {
        UUID clinicaDoVet = currentProfessionalProvider.require().getOrganization().getOrganizationId();

        if (!clinicaDoVet.equals(organizationId)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getMessage(),
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    private OrganizationResponseDTO toResponse(Organization organization) {
        return OrganizationResponseDTO.builder()
                .organizationId(organization.getOrganizationId())
                .name(organization.getName())
                .ownerVetName(organization.getOwnerVetName())
                .phone(organization.getPhone())
                .email(organization.getEmail())
                .cnpj(organization.getCnpj())
                .address(organization.getAddress())
                .city(organization.getCity())
                .state(organization.getState())
                .cep(organization.getCep())
                .description(organization.getDescription())
                .creationDate(organization.getCreationDate())
                .updateDate(organization.getUpdateDate())
                .build();
    }

}

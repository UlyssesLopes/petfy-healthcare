package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteResponseDTO;
import br.com.petfy.healthcare.domain.entity.OrganizationInvite;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationInviteRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.OrganizationInviteService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationInviteServiceImpl implements OrganizationInviteService {

    private final OrganizationInviteRepository organizationInviteRepository;
    private final PersonRepository personRepository;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final OpaqueTokenService opaqueTokenService;

    @Value("${petfy.organization-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    @Override
    public OrganizationInviteResponseDTO create(OrganizationInviteRequestDTO request) {
        Person emissor = currentProfessionalProvider.require();

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        OrganizationInvite invite = organizationInviteRepository.save(OrganizationInvite.builder()
                .organization(organizacaoDoContextoOuFalha())
                .createdBy(emissor)
                .tokenHash(opaqueTokenService.hash(token))
                .email(request != null ? request.getEmail() : null)
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(invite, token);
    }

    @Override
    public List<OrganizationInviteResponseDTO> listFromMyOrganization() {
        UUID organizationId = organizacaoDoContexto();

        return organizationInviteRepository.findByOrganizationOrganizationIdOrderByCreationDateDesc(organizationId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revoke(UUID organizationInviteId) {
        UUID organizationId = organizacaoDoContexto();

        OrganizationInvite invite = organizationInviteRepository.findById(organizationInviteId)
                .filter(i -> i.getOrganization().getOrganizationId().equals(organizationId))
                .orElseThrow(this::inviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            organizationInviteRepository.save(invite);
        }
    }

    /**
     * Token inexistente, expirado, revogado, ja usado e destinado a outro email
     * respondem igual. Distinguir diria a quem tenta adivinhar qual parte errou -
     * e o convite e o que separa um estranho dos animals de uma clinica inteira.
     */
    @Override
    public OrganizationInvite validate(String token, String email) {
        return organizationInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(invite -> invite.isUsable(LocalDateTime.now()))
                .filter(invite -> aceitaEmail(invite, email))
                .orElseThrow(this::inviteInvalido);
    }

    @Override
    public void markAccepted(OrganizationInvite invite, UUID acceptedByVetId) {
        Person aceitante = personRepository.findById(acceptedByVetId)
                .orElseThrow(this::inviteInvalido);

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);

        organizationInviteRepository.save(invite);
    }

    /** Convite sem email e aberto a quem tiver o link; com email, so aquele. */
    private boolean aceitaEmail(OrganizationInvite invite, String email) {
        return invite.getEmail() == null || invite.getEmail().equalsIgnoreCase(email);
    }

    private PetfyHealthcareException inviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private OrganizationInviteResponseDTO toResponse(OrganizationInvite invite, String token) {
        return OrganizationInviteResponseDTO.builder()
                .organizationInviteId(invite.getOrganizationInviteId())
                .organizationId(invite.getOrganization().getOrganizationId())
                .organizationName(invite.getOrganization().getName())
                .token(token)
                .email(invite.getEmail())
                .createdByVetName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .build();
    }

    /**
     * A organizacao em nome de quem a pessoa esta agindo.
     *
     * Convidar membro e manter o cadastro sao atos <b>da organizacao</b>, nao da
     * pessoa: quem atua por si nao tem organizacao para convidar ninguem, e a resposta
     * certa e dizer isso, nao um NullPointerException onde antes havia um campo sempre
     * preenchido.
     */
    private Organization organizacaoDoContextoOuFalha() {
        return currentProfessionalProvider.requireContext().organizacao()
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));
    }

    private java.util.UUID organizacaoDoContexto() {
        return organizacaoDoContextoOuFalha().getOrganizationId();
    }

}

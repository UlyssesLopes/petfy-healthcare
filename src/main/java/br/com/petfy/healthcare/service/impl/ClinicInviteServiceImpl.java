package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ClinicInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicInviteResponseDTO;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;
import br.com.petfy.healthcare.domain.entity.Vet;
import br.com.petfy.healthcare.domain.repository.ClinicInviteRepository;
import br.com.petfy.healthcare.domain.repository.VetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentVetProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.ClinicInviteService;
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
public class ClinicInviteServiceImpl implements ClinicInviteService {

    private final ClinicInviteRepository clinicInviteRepository;
    private final VetRepository vetRepository;
    private final CurrentVetProvider currentVetProvider;
    private final OpaqueTokenService opaqueTokenService;

    @Value("${petfy.clinic-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    @Override
    public ClinicInviteResponseDTO create(ClinicInviteRequestDTO request) {
        Vet emissor = currentVetProvider.require();

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        ClinicInvite invite = clinicInviteRepository.save(ClinicInvite.builder()
                .clinic(emissor.getClinic())
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
    public List<ClinicInviteResponseDTO> listFromMyClinic() {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();

        return clinicInviteRepository.findByClinicClinicIdOrderByCreationDateDesc(clinicId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revoke(UUID clinicInviteId) {
        UUID clinicId = currentVetProvider.require().getClinic().getClinicId();

        ClinicInvite invite = clinicInviteRepository.findById(clinicInviteId)
                .filter(i -> i.getClinic().getClinicId().equals(clinicId))
                .orElseThrow(this::inviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            clinicInviteRepository.save(invite);
        }
    }

    /**
     * Token inexistente, expirado, revogado, ja usado e destinado a outro email
     * respondem igual. Distinguir diria a quem tenta adivinhar qual parte errou -
     * e o convite e o que separa um estranho dos pets de uma clinica inteira.
     */
    @Override
    public ClinicInvite validate(String token, String email) {
        return clinicInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(invite -> invite.isUsable(LocalDateTime.now()))
                .filter(invite -> aceitaEmail(invite, email))
                .orElseThrow(this::inviteInvalido);
    }

    @Override
    public void markAccepted(ClinicInvite invite, UUID acceptedByVetId) {
        Vet aceitante = vetRepository.findById(acceptedByVetId)
                .orElseThrow(this::inviteInvalido);

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);

        clinicInviteRepository.save(invite);
    }

    /** Convite sem email e aberto a quem tiver o link; com email, so aquele. */
    private boolean aceitaEmail(ClinicInvite invite, String email) {
        return invite.getEmail() == null || invite.getEmail().equalsIgnoreCase(email);
    }

    private PetfyHealthcareException inviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private ClinicInviteResponseDTO toResponse(ClinicInvite invite, String token) {
        return ClinicInviteResponseDTO.builder()
                .clinicInviteId(invite.getClinicInviteId())
                .clinicId(invite.getClinic().getClinicId())
                .clinicName(invite.getClinic().getName())
                .token(token)
                .email(invite.getEmail())
                .createdByVetName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .build();
    }

}

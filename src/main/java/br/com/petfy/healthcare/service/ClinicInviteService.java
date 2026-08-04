package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ClinicInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicInviteResponseDTO;
import br.com.petfy.healthcare.domain.entity.ClinicInvite;

import java.util.List;
import java.util.UUID;

public interface ClinicInviteService {

    ClinicInviteResponseDTO create(ClinicInviteRequestDTO request);

    List<ClinicInviteResponseDTO> listFromMyClinic();

    void revoke(UUID clinicInviteId);

    /**
     * Valida o convite para o email que esta se cadastrando. Nao consome ainda -
     * o vet precisa existir antes de poder ser registrado como quem aceitou.
     */
    ClinicInvite validate(String token, String email);

    void markAccepted(ClinicInvite invite, UUID acceptedByVetId);

}

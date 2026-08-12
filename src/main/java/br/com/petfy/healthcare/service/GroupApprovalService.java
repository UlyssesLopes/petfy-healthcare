package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.GroupApprovalRequestDTO;
import br.com.petfy.healthcare.domain.dto.GroupApprovalResponseDTO;

import java.util.List;
import java.util.UUID;

/** O acordo de duas pessoas, num grupo onde ninguém é dono (Telas 43 e 44). */
public interface GroupApprovalService {

    GroupApprovalResponseDTO pedir(GroupApprovalRequestDTO dto);

    List<GroupApprovalResponseDTO> pendentes();

    GroupApprovalResponseDTO concordar(UUID groupApprovalId);

    GroupApprovalResponseDTO recusar(UUID groupApprovalId);

}

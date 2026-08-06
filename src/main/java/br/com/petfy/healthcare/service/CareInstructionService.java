package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Orientacoes de cuidado do animal e a confirmacao de que foram cumpridas.
 *
 * <b>Nao tem update.</b> Mudar a dose de um remedio nao e corrigir um campo - e uma
 * orientacao nova, e quem cumpriu a antiga cumpriu a antiga. Editar o texto reescreveria
 * o que os cumprimentos anteriores atestam. Encerra-se a orientacao e emite-se outra.
 */
public interface CareInstructionService {

    CareInstructionResponseDTO create(UUID animalId, CareInstructionRequestDTO request);

    /** Todas as orientacoes do animal, incluindo as encerradas - elas sao historico. */
    List<CareInstructionResponseDTO> listByAnimal(UUID animalId);

    /** Encerra antes do prazo. Idempotente: a segunda chamada nao mexe na data da primeira. */
    CareInstructionResponseDTO revoke(UUID animalId, UUID careInstructionId);

    CareInstructionFulfillmentResponseDTO confirmFulfillment(
            UUID animalId, UUID careInstructionId, CareInstructionFulfillmentRequestDTO request);

    /** O historico de aderencia de uma orientacao, do cumprimento mais recente ao mais antigo. */
    List<CareInstructionFulfillmentResponseDTO> listFulfillments(UUID animalId, UUID careInstructionId);

}

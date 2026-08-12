package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.BoardingRequestDTO;
import br.com.petfy.healthcare.domain.dto.BoardingResponseDTO;

import java.util.UUID;

/** O animal fora de casa por uma semana (Tela 47). */
public interface BoardingService {

    /** Entrega o animal para hospedagem: a custodia passa, com prazo. */
    BoardingResponseDTO hospedar(UUID animalId, BoardingRequestDTO dto);

    /** A estadia em curso, se houver. */
    BoardingResponseDTO emCurso(UUID animalId);

    /** Registra a volta: a custodia retorna a quem entregou. */
    BoardingResponseDTO devolver(UUID animalId);

}

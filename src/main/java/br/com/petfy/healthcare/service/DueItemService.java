package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;

import java.util.List;

public interface DueItemService {

    /** Tudo que cobra acao da pessoa autenticada, do mais atrasado ao menos urgente. */
    List<DueItemResponseDTO> doAutenticado(int windowDays);

}

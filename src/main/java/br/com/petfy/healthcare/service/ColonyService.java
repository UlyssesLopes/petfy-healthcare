package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ColonyAnimalDTO;

import java.util.List;

/** A lista da colônia (Tela 43). */
public interface ColonyService {

    /**
     * @param filtro TODOS, FALTA_CASTRAR, EM_TRATAMENTO ou SUMIDOS. Nulo é TODOS.
     */
    List<ColonyAnimalDTO> listar(String filtro);

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalSearchResultDTO;

/** A busca autenticada: nome, microchip ou RGA (Tela 35). */
public interface AnimalSearchService {

    AnimalSearchResultDTO buscar(String termo);

}

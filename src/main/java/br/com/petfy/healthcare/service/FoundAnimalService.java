package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.FoundAnimalCardDTO;

/** A busca de animal encontrado, sem conta (Tela 34). */
public interface FoundAnimalService {

    FoundAnimalCardDTO procurar(String microchipNumber);

}

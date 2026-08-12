package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClosedLifeResponseDTO;

import java.util.UUID;

/**
 * O fim da linha do tempo (Tela 33).
 *
 * A lista de "quem ja esteve com voce" NAO mora aqui: ela e uma listagem de animais como as
 * outras, e vive no {@link AnimalService} junto com a que ela complementa. Quem transferiu o
 * animal tambem aparece la, e obito e so um dos jeitos de a custodia acabar.
 */
public interface AnimalDeathService {

    ClosedLifeResponseDTO registrar(UUID animalId, AnimalDeathRequestDTO dto);

    ClosedLifeResponseDTO daFichaFechada(UUID animalId);

}

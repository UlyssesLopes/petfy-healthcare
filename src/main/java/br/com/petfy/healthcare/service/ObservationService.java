package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ObservationRequestDTO;
import br.com.petfy.healthcare.domain.dto.ObservationResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * O que alguem viu com o animal (3.11).
 *
 * <b>Nao ha update nem delete, e isso e a promessa 5.2 valendo.</b> Observacao e relato de
 * fato num instante; corrigir o que se viu depois nao e conserto de digitacao, e mudar o
 * relato. Quem viu outra coisa registra outra observacao, e as duas ficam - a segunda com
 * a data em que foi vista.
 */
public interface ObservationService {

    ObservationResponseDTO create(UUID animalId, ObservationRequestDTO request);

    List<ObservationResponseDTO> listByAnimal(UUID animalId);

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalCostResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * O custo do animal (Telas 40, 41 e 42).
 *
 * <b>Escreve quem registra, le so quem responde.</b> A assimetria e a regra inteira — e ela existe
 * porque "o custo do animal so existe se o dado entrar sem esforco", mas "o que a clinica cobra do
 * tutor nao e assunto da creche, do petshop nem de outra clinica".
 */
public interface AnimalCostService {

    /** Exige CUSTODIA. Nenhum escopo de concessao substitui. */
    List<AnimalCostResponseDTO> doAnimal(UUID animalId);

    /** Exige o mesmo alcance de escrita que registrar qualquer evento. */
    AnimalCostResponseDTO lancar(UUID animalId, AnimalCostRequestDTO request);

}

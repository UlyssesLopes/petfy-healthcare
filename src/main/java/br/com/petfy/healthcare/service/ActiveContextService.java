package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ActiveContextResponseDTO;

/** Em nome de quem a pessoa autenticada esta agindo, e em nome de quem poderia. */
public interface ActiveContextService {

    /**
     * @param organizacaoDeclarada o valor do header {@code X-Petfy-Organization},
     *                             quando o cliente declarou um. Nulo ou em branco
     *                             faz a resolucao cair na regra default
     */
    ActiveContextResponseDTO contextoAtivo(String organizacaoDeclarada);

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * A uniao de dois cadastros do mesmo animal (Tela 32).
 *
 * <b>Quem percebe nao e quem decide, e essa separacao e a tela inteira.</b> A clinica tem o leitor
 * de microchip na mao e ve a duplicata; quem responde pelo animal e quem aceita. "Ninguem mexe na
 * vida registrada de um animal sem quem responde por ele."
 */
public interface AnimalMergeService {

    /**
     * Outros cadastros com o mesmo microchip deste animal.
     *
     * <b>E uma leitura, e nao um efeito colateral da criacao.</b> Assim ela serve tanto ao
     * instante seguinte ao cadastro — que e quando a Tela 32 aparece — quanto a semana seguinte,
     * quando alguem passa o leitor e desconfia. Amarra-la a resposta do POST faria a deteccao
     * existir uma vez so, no unico momento em que a pessoa esta com pressa.
     */
    List<AnimalMergeRequestResponseDTO.LadoDaUniao> duplicatasDe(UUID animalId);

    /**
     * Pede a uniao a quem responde pelo animal que sobrevive.
     *
     * O {@code animalId} do caminho e o SOBREVIVENTE — e dele o dono que decide, e e na tela dele
     * que o pedido aparece.
     */
    AnimalMergeRequestResponseDTO pedir(UUID animalId, AnimalMergeRequestDTO request);

    /** O que espera decisao de quem responde por este animal. */
    List<AnimalMergeRequestResponseDTO> pendentesDe(UUID animalId);

    /** Tudo que ja envolveu este cadastro, dos dois lados — inclusive o que foi recusado. */
    List<AnimalMergeRequestResponseDTO> historicoDe(UUID animalId);

    /**
     * Aceita: os eventos do absorvido passam para o sobrevivente, e o absorvido vira apontador.
     *
     * <b>Irreversivel.</b> So quem responde pelo animal sobrevivente pode.
     */
    AnimalMergeRequestResponseDTO aceitar(UUID animalMergeRequestId);

    /**
     * "Sao animais diferentes."
     *
     * Recusar nao e so arquivar: e AFIRMAR que os dois sao bichos distintos, e por isso marca o
     * microchip em conflito nos dois cadastros — "provavelmente ha um erro de digitacao em algum
     * lugar, e alguem vai precisar saber disso".
     */
    AnimalMergeRequestResponseDTO recusar(UUID animalMergeRequestId);

    /**
     * "Sao animais diferentes" — dito por quem PERCEBEU, e sem pedido nenhum.
     *
     * <b>Este caminho existe porque o desenho poe os dois botoes lado a lado na tela da clinica</b>,
     * e nao ha por que passar pelo tutor para afirmar o que a clinica ja sabe: ela tem o animal na
     * frente e o leitor na mao. Pedir a uniao mexe na vida registrada de um animal e por isso
     * precisa de quem responde; dizer "sao outros bichos" nao mexe em nada — so acende uma marca.
     *
     * <b>E a marca e o unico desfecho possivel.</b> O produto nao sabe qual dos dois microchips
     * esta errado, e adivinhar apagaria o numero certo metade das vezes. Quem sabe e quem tem o
     * animal na frente, e o que este metodo faz e garantir que a proxima pessoa a olhar veja que
     * alguem ja reparou.
     */
    void marcarComoDiferentes(UUID animalId, UUID outroAnimalId);

}

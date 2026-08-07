package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.domain.entity.DueItemKind;

import java.util.List;
import java.util.UUID;

public interface DueItemService {

    /**
     * Tudo que cobra acao da pessoa autenticada, do mais atrasado ao menos urgente.
     *
     * @param includeSilenced quando falso - o default do feed - o que ela silenciou nao
     *                        aparece. Quando verdadeiro, aparece marcado com
     *                        {@code silenced}, para ela poder voltar a ser cobrada. Sem
     *                        essa segunda leitura, silenciar seria irreversivel pela tela
     */
    List<DueItemResponseDTO> doAutenticado(int windowDays, boolean includeSilenced);

    /**
     * Para de cobrar esta pendencia desta pessoa.
     *
     * <b>Nao para o registro.</b> A proxima dose continua sendo calculada, a orientacao
     * continua vigente e a linha do tempo continua recebendo tudo (PRODUTO 4.2).
     *
     * Idempotente: silenciar duas vezes e o mesmo que silenciar uma.
     */
    void silenciar(DueItemKind kind, UUID sourceId);

    /** Volta a cobrar. Idempotente pelo mesmo motivo. */
    void voltarACobrar(DueItemKind kind, UUID sourceId);

}

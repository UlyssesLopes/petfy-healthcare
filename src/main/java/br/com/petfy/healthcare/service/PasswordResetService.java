package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PasswordResetConfirmDTO;
import br.com.petfy.healthcare.domain.dto.PasswordResetRequestDTO;

public interface PasswordResetService {

    /**
     * Emite um token de recuperacao e envia pelo canal de notificacao.
     *
     * Nao devolve nada e nao falha por e-mail desconhecido: a resposta e a mesma
     * exista ou nao a conta. Ver o motivo no PasswordResetServiceImpl.
     */
    void requestReset(PasswordResetRequestDTO request);

    void confirmReset(PasswordResetConfirmDTO request);

}

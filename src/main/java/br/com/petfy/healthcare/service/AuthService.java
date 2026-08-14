package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;

public interface AuthService {

    /**
     * Entra na conta, e a entrada fica registrada (Tela 36).
     *
     * O {@code userAgent} e como a pessoa reconhece o proprio aparelho na lista de conectados.
     * Aceita nulo: cliente que nao manda o cabecalho continua entrando, e a linha aparece sem nome
     * de aparelho — recusar o login por falta de um rotulo seria trocar o essencial pelo acessorio.
     */
    LoginResponseDTO login(LoginRequestDTO request, String userAgent);

    /** Sem aparelho declarado. */
    default LoginResponseDTO login(LoginRequestDTO request) {
        return login(request, null);
    }

}

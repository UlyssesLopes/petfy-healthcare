package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.LoginRequestDTO;
import br.com.petfy.healthcare.domain.dto.LoginResponseDTO;

public interface AuthService {

    /**
     * O resultado do login: o corpo que vai no JSON, e o refresh que NAO vai.
     *
     * <b>Os dois viajam por caminhos diferentes de proposito.</b> O JWT vai no corpo porque o
     * cliente precisa le-lo para mandar no header; o refresh vai num cookie httpOnly, que
     * JavaScript nenhum le. Poe-lo no corpo devolveria ao XSS exatamente o que o cookie existe para
     * tirar do alcance dele.
     */
    record Autenticada(LoginResponseDTO corpo, String refreshToken, boolean manterConectado) { }

    /**
     * Entra na conta, e a entrada fica registrada (Tela 36).
     *
     * O {@code userAgent} e como a pessoa reconhece o proprio aparelho na lista de conectados.
     * Aceita nulo: cliente que nao manda o cabecalho continua entrando, e a linha aparece sem nome
     * de aparelho — recusar o login por falta de um rotulo seria trocar o essencial pelo acessorio.
     */
    Autenticada login(LoginRequestDTO request, String userAgent);

    /** Sem aparelho declarado. */
    default Autenticada login(LoginRequestDTO request) {
        return login(request, null);
    }

    /**
     * Se a pessoa tem credencial profissional ativa AGORA.
     *
     * <b>Lido a cada resposta, e nunca carimbado no token.</b> Se a credencial for suspensa hoje, a
     * resposta de hoje muda — o que nao aconteceria com um papel gravado dentro do JWT. A renovacao
     * e justamente onde uma sessao longa reencontra a verdade: sem isto, quem renova por trinta dias
     * carregaria por trinta dias a resposta do dia em que entrou.
     */
    boolean temCredencialAtiva(String email);

}

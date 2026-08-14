package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.PersonSessionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Os aparelhos conectados (Tela 36).
 *
 * <b>A tela dizia que nao sabia, e agora sabe.</b> A razao registrada para nao saber — "JWT sem
 * estado, o servidor nao sabe quantos tokens validos existem" — descrevia um custo ja pago: o
 * filtro consulta o banco em toda rota autenticada desde o P1, para derrubar token anterior a uma
 * troca de senha.
 */
public interface PersonSessionService {

    /** As minhas, das mais novas para as mais velhas, incluindo as ja encerradas. */
    List<PersonSessionResponseDTO> listMine();

    /**
     * Encerra uma entrada.
     *
     * <b>Encerrar a sessao atual e sair</b>, e e permitido: quem esta lendo a lista num aparelho
     * emprestado quer exatamente isso. A tela avisa antes; o servidor nao impede.
     */
    void revoke(UUID personSessionId);

}

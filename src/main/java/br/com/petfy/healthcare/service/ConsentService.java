package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.ConsentStatusResponseDTO;
import br.com.petfy.healthcare.domain.entity.Owner;

/**
 * Registro de consentimento do titular.
 *
 * Existe porque a LGPD nao pede apenas que o titular possa sair - pede que a base
 * legal do tratamento seja registrada e <b>demonstravel</b>. Sem isto a aplicacao
 * tratava dado de saude sem poder provar que alguem consentiu.
 */
public interface ConsentService {

    /**
     * Registra o aceite dos documentos vigentes para um titular que acabou de se
     * cadastrar. Chamado de dentro da criacao de conta, na mesma transacao: conta
     * criada sem consentimento registrado seria exatamente a lacuna que isto fecha.
     */
    void registrarAceiteNoCadastro(Owner owner);

    /** O que o titular autenticado ja aceitou, e o que falta aceitar. */
    ConsentStatusResponseDTO statusDoAutenticado();

    /**
     * Aceite dos documentos vigentes por quem ja tem conta.
     *
     * Serve a dois casos: a politica mudou de versao, e a conta e anterior a esta
     * tabela - nenhuma migration pode inventar consentimento, entao essas contas
     * comecam pendentes.
     */
    ConsentStatusResponseDTO aceitarVigentes();

}

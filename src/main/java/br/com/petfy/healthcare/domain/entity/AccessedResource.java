package br.com.petfy.healthcare.domain.entity;

/**
 * O que foi lido.
 *
 * Granularidade por recurso, e nao por linha: o tutor quer saber que a clinica abriu
 * o historico de saude, nao que leu o atendimento numero sete. Registrar por linha
 * multiplicaria o volume sem responder melhor a pergunta que o log existe para
 * responder.
 */
public enum AccessedResource {

    VACCINES,

    HEALTH_RECORDS,

    VACCINE_CORRECTIONS,

    HEALTH_RECORD_CORRECTIONS,

    /** Arquivo anexado - laudo, exame, foto da carteirinha de papel. */
    ATTACHMENTS,

    /** Carteira aberta pelo link publico. */
    SHARED_CARD,

    /** O cartao aberto pela busca de microchip, sem conta e sem link (Tela 34). */
    FOUND_CARD

}

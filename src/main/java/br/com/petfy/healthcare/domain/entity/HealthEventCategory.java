package br.com.petfy.healthcare.domain.entity;

/**
 * O que aconteceu no atendimento.
 *
 * Existe ao lado de {@code eventType}, que continua sendo o rotulo livre, e nao em lugar
 * dele: trocar String por enum exigiria mapear todo valor ja gravado, e o que nao casasse
 * seria descartado - dado de saude que alguem digitou, perdido para caber num enum.
 *
 * A lista responde a pergunta "o que este animal ja passou", que e a que um prontuario
 * precisa responder. Vacina nao entra: tem entidade propria, com catalogo e calculo de
 * proxima dose.
 */
public enum HealthEventCategory {

    CONSULTA,

    /** Consulta de acompanhamento da mesma queixa. */
    RETORNO,

    /** Exame de imagem ou laboratorial. */
    EXAME,

    CIRURGIA,

    INTERNACAO,

    EMERGENCIA,

    /** Curativo, limpeza dentaria, aplicacao - intervencao que nao e cirurgia. */
    PROCEDIMENTO,

    /**
     * O que nao se encaixou.
     *
     * Serve a dois casos. O primeiro e o que o produto ainda nao previu, e para isso o
     * rotulo livre em {@code eventType} continua ao lado. O segundo e o registro que a
     * V19 encontrou ja gravado: ela nao tenta adivinhar categoria a partir do texto
     * antigo, entao todos entram como OUTRO - honesto, e reclassificavel.
     */
    OUTRO

}

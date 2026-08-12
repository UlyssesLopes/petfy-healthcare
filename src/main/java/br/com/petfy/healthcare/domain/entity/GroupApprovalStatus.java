package br.com.petfy.healthcare.domain.entity;

/** Em que pe esta o pedido de concordancia (Tela 43). */
public enum GroupApprovalStatus {

    /** Esperando que outra pessoa do grupo decida. */
    PENDENTE,

    /**
     * Alguem concordou, e o ato aconteceu.
     *
     * <b>Os dois na mesma transacao</b>: concordar E executar. Guardar "concordado, falta
     * executar" criaria um estado em que o grupo acha que deu, e o animal nao mudou de mao.
     */
    CONCORDADO,

    /**
     * Alguem recusou.
     *
     * <b>Nao apaga o pedido</b>, pela mesma escolha que o resto do schema ja fez: encerra-se, nao
     * se apaga. Quem pediu precisa poder ver que foi recusado — e nao que o pedido sumiu.
     */
    RECUSADO

}

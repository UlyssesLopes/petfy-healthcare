package br.com.petfy.healthcare.domain.entity;

/** Em que pe esta um banho agendado (Tela 18). */
public enum ServiceAppointmentStatus {

    /** Marcado, e o animal ainda nao chegou. */
    AGENDADO,

    /** "Marcar entrada": o animal esta la, e o petshop responde por ele nas proximas horas. */
    EM_ATENDIMENTO,

    /**
     * "Entregue as 9h20."
     *
     * <b>Concluir e o unico estado que produz registro na vida do animal</b> — uma observacao
     * assinada pelo petshop, quando houver algo a dizer. Agendar e marcar entrada nao entram na linha
     * do tempo: um banho que aconteceu sem novidade nao e fato de saude, e enche-la de "deu banho"
     * enterraria a vermelhidao na barriga que alguem viu no banho seguinte.
     */
    CONCLUIDO,

    /**
     * O animal nao veio.
     *
     * Existe pela mesma razao do {@code FALTA} da creche: a ausencia e informacao. Um compromisso que
     * some da agenda sem desfecho faz o petshop perder a conta de quem desmarcou e de quem simplesmente
     * nao apareceu.
     */
    FALTOU

}

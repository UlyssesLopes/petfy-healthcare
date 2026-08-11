package br.com.petfy.healthcare.domain.entity;

/**
 * O desfecho de um pedido de uniao de cadastros.
 *
 * <b>Sao tres e nao quatro:</b> nao ha "cancelado". Quem pediu nao desfaz o pedido — ele ja esta
 * na frente de quem responde pelo animal, e retirar da mesa dele uma pergunta que ja foi feita
 * seria decidir por ele que a pergunta nao importava. Se a clinica se enganou, quem responde
 * recusa, e a recusa fica registrada com o motivo original ao lado — que e informacao util para
 * a proxima vez que os dois cadastros aparecerem parecidos.
 */
public enum AnimalMergeStatus {

    PENDENTE,

    ACEITO,

    /**
     * "Sao animais diferentes."
     *
     * Recusar nao e so arquivar o pedido: e AFIRMAR que os dois cadastros sao bichos distintos. E
     * por isso que a recusa marca o microchip em conflito nos dois — "provavelmente ha um erro de
     * digitacao em algum lugar, e alguem vai precisar saber disso".
     */
    RECUSADO

}

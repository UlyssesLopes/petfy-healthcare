package br.com.petfy.healthcare.domain.entity;

/** Em que pe esta o encaminhamento (Tela 45). */
public enum ReferralStatus {

    /**
     * Esperando que quem responde pelo animal autorize.
     *
     * <b>O especialista nao e AVISADO, e ve na propria caixa de entrada se abrir.</b> Mandar-lhe
     * e-mail agora seria oferecer um caso que ele ainda nao pode abrir — e a promessa da tela e o
     * oposto: "ele abre e ja sabe tudo". Esconder por completo seria pior: quem sabe que ha um caso
     * esperando autorizacao pode ligar para a clinica que encaminhou. O que ele nao ve do pendente e
     * o animal — nome, prontuario, nada.
     */
    PENDENTE,

    /**
     * O tutor autorizou, e a concessao existe.
     *
     * <b>Os dois na mesma transacao</b>: autorizar E conceder, como o {@link GroupApprovalStatus}
     * ja fazia com concordar e executar. Um estado "autorizado, falta conceder" faria a tela dizer
     * ao tutor que o especialista alcanca o animal enquanto o especialista recebe 404.
     */
    AUTORIZADO,

    /**
     * O tutor recusou.
     *
     * <b>Nao apaga o pedido.</b> Quem encaminhou precisa saber que foi recusado — sumir diria que o
     * encaminhamento nunca chegou, e a clinica encaminharia de novo.
     *
     * <b>E o motivo da recusa nao e pedido.</b> Autorizar acesso ao proprio prontuario e decisao de
     * quem responde pelo animal, e nao ha a quem justificar: um campo de justificativa aqui faria
     * o tutor devolver explicacao a uma clinica sobre o que ele nao quis entregar.
     */
    RECUSADO

}

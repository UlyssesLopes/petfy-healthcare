package br.com.petfy.healthcare.domain.entity;

/**
 * O que gera uma pendencia.
 *
 * A lista existe para o cliente saber que tela abrir ao tocar no item - e ela vai
 * crescer: matricula com saude irregular e tema de casa entram com a creche, no
 * Horizonte 3. O ponto de a pendencia ser um conceito e que crescer aqui nao obriga
 * nenhuma tela nova.
 */
public enum DueItemKind {

    /** Dose de vacina vencida ou vencendo. Era a agenda, e continua sendo o caso comum. */
    DOSE_DE_VACINA,

    /** Antiparasitario no intervalo de reforco. */
    ANTIPARASITARIO,

    /** Orientacao valendo hoje e ainda nao cumprida no intervalo. */
    ORIENTACAO,

    /**
     * Convite de co-tutor aguardando resposta.
     *
     * Cobra quem <b>convidou</b>, e nao quem foi convidado: quem recebeu o convite
     * talvez nem tenha conta, e o produto nao tem como cobrar quem nao alcanca.
     */
    CONVITE_PENDENTE,

    /**
     * Consentimento de uma versao nova da politica.
     *
     * Nao tem animal nem data: cobra agora, e bloqueia o resto - e o unico item da
     * lista que e estado, e nao prazo.
     */
    CONSENTIMENTO_PENDENTE

}

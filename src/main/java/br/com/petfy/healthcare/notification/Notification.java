package br.com.petfy.healthcare.notification;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Mensagem pronta para envio.
 *
 * Quem monta o texto e o dominio, nao o canal: assim log e e-mail viram
 * transporte, e um lembrete de vacina e um aviso de vacina registrada usam a
 * mesma infraestrutura sem que ela precise conhecer nenhum dos dois.
 */
@Getter
@Builder
public class Notification {

    private final String toEmail;

    private final String toName;

    private final String subject;

    /** Corpo em linhas, para o canal decidir como separar. */
    private final List<String> lines;

    /**
     * Se este aviso pode sair por e-mail.
     *
     * <b>Falso quando o endereco ainda nao foi confirmado</b> — "o nome do animal e dos tutores nao
     * vai para a caixa de um estranho". A regra sempre existiu; o que mudou foi <b>onde</b> ela
     * mora.
     *
     * Ate a V48 cada notificador PULAVA o destinatario nao confirmado, e o aviso nao chegava a ser
     * montado. Com o canal in-app isso virou defeito: o in-app so aparece para quem ja entrou na
     * conta, entao ele nao vaza nada — e quem nao confirmou o e-mail e <b>exatamente quem mais
     * precisa dele</b>. Pular no dominio matava os dois canais para essa pessoa.
     *
     * Agora o aviso e sempre produzido, e cada canal decide se lhe cabe.
     */
    @Builder.Default
    private final boolean porEmail = true;

    /** O mesmo aviso, sem o canal de e-mail. */
    public Notification semEmail() {
        return Notification.builder()
                .toEmail(toEmail)
                .toName(toName)
                .subject(subject)
                .lines(lines)
                .porEmail(false)
                .build();
    }

    public String saudacao() {
        return toName != null && !toName.isBlank() ? "Ola, " + toName + "!" : "Ola!";
    }

}

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

    public String saudacao() {
        return toName != null && !toName.isBlank() ? "Ola, " + toName + "!" : "Ola!";
    }

}

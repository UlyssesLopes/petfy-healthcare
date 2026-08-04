package br.com.petfy.healthcare.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Ativado com petfy.notifications.channel=email, que tambem exige spring.mail.*
 * configurado - sem host de SMTP o Spring nao cria o JavaMailSender e o contexto
 * nao sobe.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petfy.notifications.channel", havingValue = "email")
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;

    @Value("${petfy.notifications.from:nao-responda@petfy.com.br}")
    private String remetente;

    @Override
    public void send(Notification notification) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(notification.getToEmail());
        mensagem.setSubject(notification.getSubject());
        mensagem.setText(notification.saudacao() + "\n\n" + String.join("\n", notification.getLines()));

        mailSender.send(mensagem);
    }

}

package br.com.petfy.healthcare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Canal padrao, usado quando nao ha SMTP configurado.
 *
 * Nao e um stub vazio de proposito: com ele os fluxos de notificacao rodam de
 * ponta a ponta em desenvolvimento, incluindo a montagem da mensagem. Ligar o
 * envio real vira so configuracao.
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("canalExterno")
@ConditionalOnProperty(name = "petfy.notifications.channel", havingValue = "log", matchIfMissing = true)
public class LoggingNotifier implements Notifier {

    @Override
    public void send(Notification notification) {
        log.info("[{}] para {}", notification.getSubject(), notification.getToEmail());
        notification.getLines().forEach(linha -> log.info("  {}", linha));
    }

}

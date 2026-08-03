package br.com.petfy.healthcare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Canal padrao, usado quando nao ha SMTP configurado.
 *
 * Nao e um stub vazio de proposito: com ele a rotina roda de ponta a ponta em
 * desenvolvimento - varre, agrupa, monta a mensagem e marca o envio - sem
 * depender de credencial de e-mail. Ligar o envio real vira so configuracao.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "petfy.reminders.channel", havingValue = "log", matchIfMissing = true)
public class LoggingReminderNotifier implements ReminderNotifier {

    @Override
    public void notify(VaccineReminder reminder) {
        log.info("Lembrete de vacina para {} ({} dose(s) pendente(s)):",
                reminder.getOwner().getEmail(), reminder.getItems().size());

        reminder.getItems().forEach(item -> log.info("  - {} / {} - {} ({})",
                item.getPetName(),
                item.getVaccineName(),
                item.isOverdue() ? "venceu ha " + Math.abs(item.getDaysUntilNextDose()) + " dia(s)"
                        : "vence em " + item.getDaysUntilNextDose() + " dia(s)",
                item.getNextDoseDate()));
    }

}

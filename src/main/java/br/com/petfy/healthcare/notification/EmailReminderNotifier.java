package br.com.petfy.healthcare.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Ativado com petfy.reminders.channel=email, que tambem exige spring.mail.*
 * configurado - sem host de SMTP o Spring nao cria o JavaMailSender e o contexto
 * nao sobe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petfy.reminders.channel", havingValue = "email")
public class EmailReminderNotifier implements ReminderNotifier {

    private final JavaMailSender mailSender;

    @Value("${petfy.reminders.from:nao-responda@petfy.com.br}")
    private String remetente;

    @Override
    public void notify(VaccineReminder reminder) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(reminder.getOwner().getEmail());
        mensagem.setSubject(assunto(reminder));
        mensagem.setText(corpo(reminder));

        mailSender.send(mensagem);
    }

    private String assunto(VaccineReminder reminder) {
        long vencidas = reminder.getItems().stream().filter(VaccineReminder.Item::isOverdue).count();

        return vencidas > 0
                ? "Vacina em atraso no Petfy"
                : "Vacina chegando no Petfy";
    }

    private String corpo(VaccineReminder reminder) {
        String saudacao = reminder.getOwner().getName() != null
                ? "Ola, " + reminder.getOwner().getName() + "!"
                : "Ola!";

        String linhas = reminder.getItems().stream()
                .map(item -> String.format("- %s: %s %s (%s)",
                        item.getPetName(),
                        item.getVaccineName(),
                        item.isOverdue()
                                ? "venceu ha " + Math.abs(item.getDaysUntilNextDose()) + " dia(s)"
                                : "vence em " + item.getDaysUntilNextDose() + " dia(s)",
                        item.getNextDoseDate()))
                .collect(Collectors.joining("\n"));

        return saudacao + "\n\nEstas doses precisam de atencao:\n\n" + linhas
                + "\n\nAcesse o Petfy para ver a carteira completa dos seus pets.";
    }

}

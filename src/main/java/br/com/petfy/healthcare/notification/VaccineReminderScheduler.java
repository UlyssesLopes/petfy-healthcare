package br.com.petfy.healthcare.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Desligado por padrao: com varias instancias da aplicacao no ar, todas
 * disparariam a rotina e o tutor receberia o lembrete repetido. Ligar exige
 * decidir quem executa - uma instancia so, ou um agendador externo chamando a
 * rotina. Enquanto isso nao existe, o default seguro e nao enviar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "petfy.reminders.enabled", havingValue = "true")
public class VaccineReminderScheduler {

    private final VaccineReminderService vaccineReminderService;

    @Scheduled(cron = "${petfy.reminders.cron:0 0 9 * * *}", zone = "America/Sao_Paulo")
    public void executar() {
        try {
            int notificados = vaccineReminderService.enviarLembretes();
            log.debug("Rotina de lembretes concluida: {} tutor(es)", notificados);
        } catch (Exception e) {
            // rotina agendada nao pode morrer por causa de uma execucao: sem o
            // catch, uma falha aqui derruba o agendamento e nenhum lembrete sai
            // mais ate reiniciar a aplicacao
            log.error("Falha na rotina de lembretes de vacina", e);
        }
    }

}

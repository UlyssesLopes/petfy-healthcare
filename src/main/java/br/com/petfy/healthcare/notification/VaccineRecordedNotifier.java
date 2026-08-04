package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Avisa o tutor quando uma clinica registra vacina no pet dele.
 *
 * Sem isso o tutor so descobre abrindo o app, o que e justamente o oposto de
 * confiar a carteira a terceiros: quem autorizou uma clinica precisa enxergar o
 * que ela escreveu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaccineRecordedNotifier {

    private final Notifier notifier;

    /**
     * Falha de notificacao nao pode desfazer o registro.
     *
     * E o oposto do lembrete, onde a excecao sobe de proposito: la o envio e o
     * unico efeito, e nao avisar significa nao ter feito nada. Aqui o efeito
     * principal e a vacina gravada no historico do pet - perde-la porque o SMTP
     * caiu seria trocar um problema pequeno por um grande.
     */
    public void notifyOwner(Vaccine vaccine) {
        try {
            notifier.send(montar(vaccine));
        } catch (Exception e) {
            log.error("Falha ao notificar o tutor sobre a vacina {} registrada no pet {}",
                    vaccine.getVaccineId(), vaccine.getPet().getPetId(), e);
        }
    }

    private Notification montar(Vaccine vaccine) {
        Owner owner = vaccine.getPet().getOwner();
        String clinica = vaccine.getClinic() != null ? vaccine.getClinic().getName() : "uma clinica";

        List<String> linhas = new ArrayList<>();
        linhas.add(String.format("%s registrou uma vacina no %s:", clinica, vaccine.getPet().getName()));
        linhas.add(String.format("- %s, aplicada em %s", vaccine.getVaccineName(), vaccine.getApplicationDate()));

        if (vaccine.getNextDoseDate() != null) {
            linhas.add(String.format("- proxima dose prevista para %s", vaccine.getNextDoseDate()));
        }

        linhas.add("");
        linhas.add("Se nao reconhece este registro, revogue o acesso da clinica no Petfy.");

        return Notification.builder()
                .toEmail(owner.getEmail())
                .toName(owner.getName())
                .subject("Nova vacina registrada em " + vaccine.getPet().getName())
                .lines(linhas)
                .build();
    }

}

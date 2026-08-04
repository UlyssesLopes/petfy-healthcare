package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Avisa o tutor sobre o que uma clinica escreveu no pet dele.
 *
 * Sem isso o tutor so descobre abrindo o app, o que e o oposto de confiar a
 * carteira a terceiros: quem autorizou uma clinica precisa enxergar o que ela
 * escreveu.
 *
 * Um componente para os quatro eventos, e nao um por evento: todos tem o mesmo
 * destinatario, a mesma politica de falha e a mesma orientacao final.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicActivityNotifier {

    private static final String ORIENTACAO = "Se nao reconhece este registro, revogue o acesso da clinica no Petfy.";

    private final Notifier notifier;

    public void vaccineRecorded(Vaccine vaccine) {
        enviar(vaccine.getPet().getOwner(), () -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s registrou uma vacina no %s:",
                    clinica(vaccine.getClinic() != null ? vaccine.getClinic().getName() : null),
                    vaccine.getPet().getName()));
            linhas.add(String.format("- %s, aplicada em %s",
                    vaccine.getVaccineName(), vaccine.getApplicationDate()));

            if (vaccine.getNextDoseDate() != null) {
                linhas.add(String.format("- proxima dose prevista para %s", vaccine.getNextDoseDate()));
            }

            return montar(vaccine.getPet().getOwner(),
                    "Nova vacina registrada em " + vaccine.getPet().getName(), linhas);
        }, "vacina registrada");
    }

    public void vaccineCorrected(Vaccine vaccine) {
        enviar(vaccine.getPet().getOwner(), () -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s corrigiu um registro de vacina do %s.",
                    clinica(vaccine.getClinic() != null ? vaccine.getClinic().getName() : null),
                    vaccine.getPet().getName()));
            linhas.add(String.format("Como esta agora: %s, aplicada em %s",
                    vaccine.getVaccineName(), vaccine.getApplicationDate()));

            if (vaccine.getNextDoseDate() != null) {
                linhas.add(String.format("- proxima dose prevista para %s", vaccine.getNextDoseDate()));
            }

            return montar(vaccine.getPet().getOwner(),
                    "Registro de vacina corrigido em " + vaccine.getPet().getName(), linhas);
        }, "vacina corrigida");
    }

    public void healthRecordRecorded(HealthRecord record) {
        enviar(record.getPet().getOwner(), () -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s registrou um atendimento do %s:",
                    clinica(record.getClinic() != null ? record.getClinic().getName() : null),
                    record.getPet().getName()));
            linhas.add(String.format("- %s em %s", record.getEventType(), record.getEventDate()));

            if (record.getDescription() != null && !record.getDescription().isBlank()) {
                linhas.add("- " + record.getDescription());
            }

            return montar(record.getPet().getOwner(),
                    "Novo atendimento registrado em " + record.getPet().getName(), linhas);
        }, "atendimento registrado");
    }

    public void healthRecordCorrected(HealthRecord record) {
        enviar(record.getPet().getOwner(), () -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s corrigiu um atendimento do %s.",
                    clinica(record.getClinic() != null ? record.getClinic().getName() : null),
                    record.getPet().getName()));
            linhas.add(String.format("Como esta agora: %s em %s",
                    record.getEventType(), record.getEventDate()));

            return montar(record.getPet().getOwner(),
                    "Atendimento corrigido em " + record.getPet().getName(), linhas);
        }, "atendimento corrigido");
    }

    /**
     * Falha de notificacao nao pode desfazer o que foi registrado.
     *
     * E o oposto do lembrete de vacina, onde a excecao sobe de proposito: la o
     * envio e o unico efeito, e nao avisar significa nao ter feito nada. Aqui o
     * efeito principal e o registro no historico do pet - perde-lo porque o SMTP
     * caiu seria trocar um problema pequeno por um grande.
     */
    /**
     * Recebe o tutor explicitamente, e nao apenas a mensagem pronta, para que a
     * checagem de e-mail confirmado valha para todo aviso desta classe - inclusive
     * um que venha a ser adicionado depois, que nao compila sem passar o tutor.
     *
     * Enquanto o e-mail nao for confirmado, nada sai para aquele endereco. O
     * silencio aqui e aceitavel: o registro no historico do pet continua sendo
     * feito e o tutor o ve ao abrir a carteira. O que nao pode e o nome do pet e
     * do tutor irem parar na caixa de um estranho.
     */
    private void enviar(Owner owner, java.util.function.Supplier<Notification> mensagem, String evento) {
        if (!owner.podeReceberNotificacao()) {
            log.info("Tutor {} ainda nao confirmou o e-mail; aviso de {} suprimido",
                    owner.getOwnerId(), evento);
            return;
        }

        try {
            notifier.send(mensagem.get());
        } catch (Exception e) {
            log.error("Falha ao notificar o tutor sobre {}", evento, e);
        }
    }

    private Notification montar(Owner owner, String assunto, List<String> linhas) {
        linhas.add("");
        linhas.add(ORIENTACAO);

        return Notification.builder()
                .toEmail(owner.getEmail())
                .toName(owner.getName())
                .subject(assunto)
                .lines(linhas)
                .build();
    }

    private String clinica(String nome) {
        return nome != null ? nome : "uma clinica";
    }

}

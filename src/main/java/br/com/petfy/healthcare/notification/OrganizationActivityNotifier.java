package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Avisa o tutor sobre o que uma clinica escreveu no animal dele.
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
public class OrganizationActivityNotifier {

    private static final String ORIENTACAO = "Se nao reconhece este registro, revogue o acesso da clinica no Petfy.";

    private final AsyncNotificationDispatcher dispatcher;
    private final br.com.petfy.healthcare.service.AnimalReach animalReach;

    public void vaccineRecorded(Vaccine vaccine) {
        enviar(vaccine.getAnimal(), destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s registrou uma vacina no %s:",
                    clinica(vaccine.getOrganization() != null ? vaccine.getOrganization().getName() : null),
                    vaccine.getAnimal().getName()));
            linhas.add(String.format("- %s, aplicada em %s",
                    vaccine.getVaccineName(), vaccine.getApplicationDate()));

            if (vaccine.getNextDoseDate() != null) {
                linhas.add(String.format("- proxima dose prevista para %s", vaccine.getNextDoseDate()));
            }

            return montar(destinatario,
                    "Nova vacina registrada em " + vaccine.getAnimal().getName(), linhas);
        }, "vacina registrada");
    }

    public void vaccineCorrected(Vaccine vaccine) {
        enviar(vaccine.getAnimal(), destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s corrigiu um registro de vacina do %s.",
                    clinica(vaccine.getOrganization() != null ? vaccine.getOrganization().getName() : null),
                    vaccine.getAnimal().getName()));
            linhas.add(String.format("Como esta agora: %s, aplicada em %s",
                    vaccine.getVaccineName(), vaccine.getApplicationDate()));

            if (vaccine.getNextDoseDate() != null) {
                linhas.add(String.format("- proxima dose prevista para %s", vaccine.getNextDoseDate()));
            }

            return montar(destinatario,
                    "Registro de vacina corrigido em " + vaccine.getAnimal().getName(), linhas);
        }, "vacina corrigida");
    }

    public void healthRecordRecorded(HealthRecord record) {
        enviar(record.getAnimal(), destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s registrou um atendimento do %s:",
                    clinica(record.getOrganization() != null ? record.getOrganization().getName() : null),
                    record.getAnimal().getName()));
            linhas.add(String.format("- %s em %s", record.getEventType(), record.getEventDate()));

            if (record.getDescription() != null && !record.getDescription().isBlank()) {
                linhas.add("- " + record.getDescription());
            }

            return montar(destinatario,
                    "Novo atendimento registrado em " + record.getAnimal().getName(), linhas);
        }, "atendimento registrado");
    }

    public void healthRecordCorrected(HealthRecord record) {
        enviar(record.getAnimal(), destinatario -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(String.format("%s corrigiu um atendimento do %s.",
                    clinica(record.getOrganization() != null ? record.getOrganization().getName() : null),
                    record.getAnimal().getName()));
            linhas.add(String.format("Como esta agora: %s em %s",
                    record.getEventType(), record.getEventDate()));

            return montar(destinatario,
                    "Atendimento corrigido em " + record.getAnimal().getName(), linhas);
        }, "atendimento corrigido");
    }

    /**
     * Falha de notificacao nao pode desfazer o que foi registrado.
     *
     * E o oposto do lembrete de vacina, onde a excecao sobe de proposito: la o
     * envio e o unico efeito, e nao avisar significa nao ter feito nada. Aqui o
     * efeito principal e o registro no historico do animal - perde-lo porque o SMTP
     * caiu seria trocar um problema pequeno por um grande.
     */
    /**
     * Recebe o tutor explicitamente, e nao apenas a mensagem pronta, para que a
     * checagem de e-mail confirmado valha para todo aviso desta classe - inclusive
     * um que venha a ser adicionado depois, que nao compila sem passar o tutor.
     *
     * Enquanto o e-mail nao for confirmado, nada sai para aquele endereco. O
     * silencio aqui e aceitavel: o registro no historico do animal continua sendo
     * feito e o tutor o ve ao abrir a carteira. O que nao pode e o nome do animal e
     * do tutor irem parar na caixa de um estranho.
     *
     * A mensagem e montada aqui, ainda na transacao de quem chamou, porque monta-la
     * passa por associacoes lazy. So o envio sai para outra thread: e ele que
     * depende de SMTP e que somava latencia a requisicao do veterinario.
     */
    private void enviar(Animal animal, java.util.function.Function<Person, Notification> mensagem, String evento) {
        // A partir da V15 um animal tem varios tutores, e o aviso vai para todos:
        // quem divide o cuidado do animal precisa saber que a clinica registrou
        // algo nele. O papel nao filtra - quem so le tambem quer saber que
        // apareceu vacina que ninguem da casa reconhece, que e justamente o caso
        // que a orientacao no rodape trata.
        for (Person destinatario : animalReach.pessoas(animal.getAnimalId())) {
            if (!destinatario.podeReceberNotificacao()) {
                log.info("Tutor {} ainda nao confirmou o e-mail; aviso de {} suprimido",
                        destinatario.getPersonId(), evento);
                continue;
            }

            // o dispatch entra no try junto com a montagem: ele so enfileira, mas
            // enfileirar falha se o pool estiver em shutdown, e uma
            // RejectedExecutionException subindo daqui desfaria o registro que acabou
            // de ser gravado - o problema que esta classe existe para evitar.
            // O try e por destinatario: falhar para um tutor nao pode calar os outros
            try {
                dispatcher.dispatch(mensagem.apply(destinatario), evento);
            } catch (Exception e) {
                log.error("Falha ao preparar o aviso de {} para o tutor {}",
                        evento, destinatario.getPersonId(), e);
            }
        }
    }

    private Notification montar(Person person, String assunto, List<String> linhas) {
        linhas.add("");
        linhas.add(ORIENTACAO);

        return Notification.builder()
                .toEmail(person.getEmail())
                .toName(person.getName())
                .subject(assunto)
                .lines(linhas)
                .build();
    }

    private String clinica(String nome) {
        return nome != null ? nome : "uma clinica";
    }

}

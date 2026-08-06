package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaccineReminderService {

    private final VaccineRepository vaccineRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final Notifier notifier;
    private final br.com.petfy.healthcare.service.AnimalReach animalReach;

    /** Quantos dias de antecedencia entram no lembrete. */
    @Value("${petfy.reminders.window-days:30}")
    private int windowDays;

    /** Intervalo minimo entre dois lembretes da mesma dose. */
    @Value("${petfy.reminders.cooldown-days:7}")
    private int cooldownDays;

    /**
     * Varre as doses pendentes (vacinas e antiparasitarios), manda um lembrete
     * por tutor e marca o envio.
     *
     * Um lembrete por tutor, e nao um por dose: quem tem tres animals atrasados
     * recebe um e-mail com tres linhas, nao tres e-mails. Antiparasitarios e
     * vacinas sao agrupados no mesmo e-mail porque pertencem ao mesmo tutor -
     * o canal nao muda, so a fonte dos dados.
     *
     * @return quantos tutores foram notificados
     */
    @Transactional
    public int enviarLembretes() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();
        LocalDate limite = hoje.plusDays(windowDays);

        // --- vacinas ---
        List<Vaccine> vacinasPendentes = vaccineRepository.findByNextDoseDateLessThanEqual(limite)
                .stream()
                .filter(v -> deveAvisarVacina(v, agora))
                .filter(v -> temTutorNotificavel(v.getAnimal()))
                .collect(Collectors.toList());

        // --- antiparasitarios ---
        List<Antiparasitic> antisPendentes = antiparasiticRepository.findByNextDoseDateLessThanEqual(limite)
                .stream()
                .filter(a -> deveAvisarAnti(a, agora))
                // tutor que ainda nao confirmou o e-mail fica de fora. O filtro
                // vem antes do agrupamento de proposito: assim a dose tambem nao
                // e marcada como avisada, e o lembrete sai na primeira varredura
                // depois que ele confirmar, em vez de se perder
                .filter(a -> temTutorNotificavel(a.getAnimal()))
                .collect(Collectors.toList());

        if (vacinasPendentes.isEmpty() && antisPendentes.isEmpty()) {
            // log tambem quando nao ha nada a enviar: sem esta linha, uma rotina
            // que rodou e nao achou dose fica indistinguivel de uma que nao rodou
            // ou que morreu no meio - e a diferenca so apareceria como tutor
            // reclamando de lembrete que nunca chegou
            log.info("Varredura de lembretes concluida sem doses a avisar (janela de {} dias)", windowDays);
            return 0;
        }

        // Agrupa por tutor para gerar um unico e-mail por pessoa, qualquer que
        // seja o tipo da dose.
        //
        // A partir da V15 um animal tem varios tutores, entao a mesma dose entra no
        // lembrete de cada um deles - quem divide o cuidado do animal precisa
        // saber da vacina vencendo, e nao so quem cadastrou o animal. Por isso o
        // agrupamento **explode** cada dose pelos tutores do animal, em vez de
        // indexar por um dono unico que nao existe mais.
        Map<Person, List<Vaccine>> vacinasPorTutor = new LinkedHashMap<>();
        for (Vaccine v : vacinasPendentes) {
            for (Person tutor : animalReach.pessoas(v.getAnimal().getAnimalId())) {
                if (tutor.podeReceberNotificacao()) {
                    vacinasPorTutor.computeIfAbsent(tutor, k -> new ArrayList<>()).add(v);
                }
            }
        }

        Map<Person, List<Antiparasitic>> antisPorTutor = new LinkedHashMap<>();
        for (Antiparasitic a : antisPendentes) {
            for (Person tutor : animalReach.pessoas(a.getAnimal().getAnimalId())) {
                if (tutor.podeReceberNotificacao()) {
                    antisPorTutor.computeIfAbsent(tutor, k -> new ArrayList<>()).add(a);
                }
            }
        }

        // uniao dos tutores que tem ao menos um item (vacina ou antiparasitario)
        java.util.Set<Person> tutores = new java.util.LinkedHashSet<>();
        tutores.addAll(vacinasPorTutor.keySet());
        tutores.addAll(antisPorTutor.keySet());

        int notificados = 0;

        for (Person tutor : tutores) {
            notifier.send(montarLembrete(
                    tutor,
                    vacinasPorTutor.getOrDefault(tutor, List.of()),
                    antisPorTutor.getOrDefault(tutor, List.of()),
                    hoje));
            notificados++;
        }

        // A marcacao vem depois de TODOS os envios, e nao dentro do laco, porque
        // uma dose de animal compartilhado aparece no lembrete de mais de um tutor -
        // marcada por tutor, ela seria escrita duas vezes.
        //
        // Continua valendo o que valia antes: marcar so depois de enviar. Se o
        // canal falhar, a excecao sobe, o rollback desfaz a marcacao e a dose
        // volta a ser elegivel na proxima varredura, em vez de constar como
        // avisada sem que ninguem tenha recebido.
        vacinasPendentes.forEach(v -> v.setLastReminderSentAt(agora));
        if (!vacinasPendentes.isEmpty()) vaccineRepository.saveAll(vacinasPendentes);

        antisPendentes.forEach(a -> a.setLastReminderSentAt(agora));
        if (!antisPendentes.isEmpty()) antiparasiticRepository.saveAll(antisPendentes);

        log.info("Lembretes enviados para {} tutor(es): {} vacina(s), {} antiparasitario(s)",
                notificados, vacinasPendentes.size(), antisPendentes.size());

        return notificados;
    }

    /**
     * Basta um tutor apto para a dose entrar na varredura. O filtro fica antes
     * do agrupamento de proposito, como antes da V15: um animal cujos tutores nao
     * confirmaram o e-mail nao tem a dose marcada como avisada, e o lembrete sai
     * na primeira varredura depois da confirmacao em vez de se perder.
     */
    private boolean temTutorNotificavel(Animal animal) {
        return animalReach.temAlguemNotificavel(animal.getAnimalId());
    }

    private Notification montarLembrete(Person person, List<Vaccine> vacinas, List<Antiparasitic> antis, LocalDate hoje) {
        List<String> linhas = new ArrayList<>();

        vacinas.stream()
                .sorted(Comparator.comparing(Vaccine::getNextDoseDate))
                .map(v -> linhaDaDoseVacina(v, hoje))
                .forEach(linhas::add);

        antis.stream()
                .sorted(Comparator.comparing(Antiparasitic::getNextDoseDate))
                .map(a -> linhaDaDoseAnti(a, hoje))
                .forEach(linhas::add);

        boolean temVencida = vacinas.stream().anyMatch(v -> v.getNextDoseDate().isBefore(hoje))
                || antis.stream().anyMatch(a -> a.getNextDoseDate().isBefore(hoje));

        return Notification.builder()
                .toEmail(person.getEmail())
                .toName(person.getName())
                .subject(temVencida ? "Aplicacao em atraso no Petfy" : "Aplicacao chegando no Petfy")
                .lines(linhas)
                .build();
    }

    private String linhaDaDoseVacina(Vaccine vaccine, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, vaccine.getNextDoseDate());

        return String.format("- %s: %s %s (%s)",
                vaccine.getAnimal().getName(),
                vaccine.getVaccineName(),
                dias < 0 ? "venceu ha " + Math.abs(dias) + " dia(s)" : "vence em " + dias + " dia(s)",
                vaccine.getNextDoseDate());
    }

    private String linhaDaDoseAnti(Antiparasitic anti, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, anti.getNextDoseDate());

        return String.format("- %s: %s %s (%s)",
                anti.getAnimal().getName(),
                anti.getName(),
                dias < 0 ? "venceu ha " + Math.abs(dias) + " dia(s)" : "vence em " + dias + " dia(s)",
                anti.getNextDoseDate());
    }

    /**
     * Dose vencida continua vencida todo dia. Sem o cooldown, o mesmo lembrete
     * sairia diariamente ate o tutor vacinar o animal.
     */
    private boolean deveAvisarVacina(Vaccine vaccine, LocalDateTime agora) {
        if (vaccine.getNextDoseDate() == null) {
            return false;
        }
        LocalDateTime ultimoEnvio = vaccine.getLastReminderSentAt();
        return ultimoEnvio == null || ultimoEnvio.isBefore(agora.minusDays(cooldownDays));
    }

    private boolean deveAvisarAnti(Antiparasitic anti, LocalDateTime agora) {
        if (anti.getNextDoseDate() == null) {
            return false;
        }
        LocalDateTime ultimoEnvio = anti.getLastReminderSentAt();
        return ultimoEnvio == null || ultimoEnvio.isBefore(agora.minusDays(cooldownDays));
    }

}

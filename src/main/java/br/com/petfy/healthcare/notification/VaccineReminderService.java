package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final Notifier notifier;

    /** Quantos dias de antecedencia entram no lembrete. */
    @Value("${petfy.reminders.window-days:30}")
    private int windowDays;

    /** Intervalo minimo entre dois lembretes da mesma dose. */
    @Value("${petfy.reminders.cooldown-days:7}")
    private int cooldownDays;

    /**
     * Varre as doses pendentes, manda um lembrete por tutor e marca o envio.
     *
     * Um lembrete por tutor, e nao um por dose: quem tem tres pets atrasados
     * recebe um e-mail com tres linhas, nao tres e-mails.
     *
     * @return quantos tutores foram notificados
     */
    @Transactional
    public int enviarLembretes() {
        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();

        List<Vaccine> pendentes = vaccineRepository.findByNextDoseDateLessThanEqual(hoje.plusDays(windowDays))
                .stream()
                .filter(vaccine -> deveAvisar(vaccine, agora))
                .collect(Collectors.toList());

        if (pendentes.isEmpty()) {
            return 0;
        }

        Map<UUID, List<Vaccine>> porTutor = pendentes.stream()
                .collect(Collectors.groupingBy(
                        vaccine -> vaccine.getPet().getOwner().getOwnerId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        porTutor.values().forEach(doTutor -> {
            notifier.send(montarLembrete(doTutor, hoje));

            // so marca depois do envio: se o canal falhar, a excecao sobe e o
            // rollback deixa a dose elegivel na proxima execucao, em vez de
            // registrar como avisada uma dose que ninguem recebeu
            doTutor.forEach(vaccine -> vaccine.setLastReminderSentAt(agora));
            vaccineRepository.saveAll(doTutor);
        });

        log.info("Lembretes de vacina enviados para {} tutor(es), cobrindo {} dose(s)",
                porTutor.size(), pendentes.size());

        return porTutor.size();
    }

    private Notification montarLembrete(List<Vaccine> doTutor, LocalDate hoje) {
        Owner owner = doTutor.get(0).getPet().getOwner();

        boolean temVencida = doTutor.stream()
                .anyMatch(v -> v.getNextDoseDate().isBefore(hoje));

        List<String> linhas = doTutor.stream()
                .sorted(Comparator.comparing(Vaccine::getNextDoseDate))
                .map(vaccine -> linhaDaDose(vaccine, hoje))
                .collect(Collectors.toList());

        return Notification.builder()
                .toEmail(owner.getEmail())
                .toName(owner.getName())
                .subject(temVencida ? "Vacina em atraso no Petfy" : "Vacina chegando no Petfy")
                .lines(linhas)
                .build();
    }

    private String linhaDaDose(Vaccine vaccine, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, vaccine.getNextDoseDate());

        return String.format("- %s: %s %s (%s)",
                vaccine.getPet().getName(),
                vaccine.getVaccineName(),
                dias < 0 ? "venceu ha " + Math.abs(dias) + " dia(s)" : "vence em " + dias + " dia(s)",
                vaccine.getNextDoseDate());
    }

    /**
     * Dose vencida continua vencida todo dia. Sem o cooldown, o mesmo lembrete
     * sairia diariamente ate o tutor vacinar o pet.
     */
    private boolean deveAvisar(Vaccine vaccine, LocalDateTime agora) {
        if (vaccine.getNextDoseDate() == null) {
            return false;
        }

        LocalDateTime ultimoEnvio = vaccine.getLastReminderSentAt();

        return ultimoEnvio == null || ultimoEnvio.isBefore(agora.minusDays(cooldownDays));
    }

}

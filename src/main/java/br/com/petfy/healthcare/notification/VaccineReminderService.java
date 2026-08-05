package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.Owner;
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
     * Um lembrete por tutor, e nao um por dose: quem tem tres pets atrasados
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
                .filter(v -> v.getPet().getOwner().podeReceberNotificacao())
                .collect(Collectors.toList());

        // --- antiparasitarios ---
        List<Antiparasitic> antisPendentes = antiparasiticRepository.findByNextDoseDateLessThanEqual(limite)
                .stream()
                .filter(a -> deveAvisarAnti(a, agora))
                // tutor que ainda nao confirmou o e-mail fica de fora. O filtro
                // vem antes do agrupamento de proposito: assim a dose tambem nao
                // e marcada como avisada, e o lembrete sai na primeira varredura
                // depois que ele confirmar, em vez de se perder
                .filter(a -> a.getPet().getOwner().podeReceberNotificacao())
                .collect(Collectors.toList());

        if (vacinasPendentes.isEmpty() && antisPendentes.isEmpty()) {
            // log tambem quando nao ha nada a enviar: sem esta linha, uma rotina
            // que rodou e nao achou dose fica indistinguivel de uma que nao rodou
            // ou que morreu no meio - e a diferenca so apareceria como tutor
            // reclamando de lembrete que nunca chegou
            log.info("Varredura de lembretes concluida sem doses a avisar (janela de {} dias)", windowDays);
            return 0;
        }

        // agrupa vacinas e antiparasitarios pelo mesmo tutor para gerar um
        // unico e-mail por tutor, independente do tipo da dose
        Map<UUID, List<Vaccine>> vacinasPorTutor = vacinasPendentes.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getPet().getOwner().getOwnerId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<UUID, List<Antiparasitic>> antisPorTutor = antisPendentes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getPet().getOwner().getOwnerId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        // uniao dos tutores que tem ao menos um item (vacina ou antiparasitario)
        java.util.Set<UUID> tutores = new java.util.LinkedHashSet<>();
        tutores.addAll(vacinasPorTutor.keySet());
        tutores.addAll(antisPorTutor.keySet());

        int notificados = 0;

        for (UUID ownerId : tutores) {
            List<Vaccine> vacinas = vacinasPorTutor.getOrDefault(ownerId, List.of());
            List<Antiparasitic> antis = antisPorTutor.getOrDefault(ownerId, List.of());

            notifier.send(montarLembrete(vacinas, antis, hoje));

            // so marca depois do envio: se o canal falhar, a excecao sobe e o
            // rollback deixa a dose elegivel na proxima execucao, em vez de
            // registrar como avisada uma dose que ninguem recebeu
            vacinas.forEach(v -> v.setLastReminderSentAt(agora));
            if (!vacinas.isEmpty()) vaccineRepository.saveAll(vacinas);

            antis.forEach(a -> a.setLastReminderSentAt(agora));
            if (!antis.isEmpty()) antiparasiticRepository.saveAll(antis);

            notificados++;
        }

        log.info("Lembretes enviados para {} tutor(es): {} vacina(s), {} antiparasitario(s)",
                notificados, vacinasPendentes.size(), antisPendentes.size());

        return notificados;
    }

    private Notification montarLembrete(List<Vaccine> vacinas, List<Antiparasitic> antis, LocalDate hoje) {
        Owner owner = vacinas.isEmpty()
                ? antis.get(0).getPet().getOwner()
                : vacinas.get(0).getPet().getOwner();

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
                .toEmail(owner.getEmail())
                .toName(owner.getName())
                .subject(temVencida ? "Aplicacao em atraso no Petfy" : "Aplicacao chegando no Petfy")
                .lines(linhas)
                .build();
    }

    private String linhaDaDoseVacina(Vaccine vaccine, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, vaccine.getNextDoseDate());

        return String.format("- %s: %s %s (%s)",
                vaccine.getPet().getName(),
                vaccine.getVaccineName(),
                dias < 0 ? "venceu ha " + Math.abs(dias) + " dia(s)" : "vence em " + dias + " dia(s)",
                vaccine.getNextDoseDate());
    }

    private String linhaDaDoseAnti(Antiparasitic anti, LocalDate hoje) {
        long dias = ChronoUnit.DAYS.between(hoje, anti.getNextDoseDate());

        return String.format("- %s: %s %s (%s)",
                anti.getPet().getName(),
                anti.getName(),
                dias < 0 ? "venceu ha " + Math.abs(dias) + " dia(s)" : "vence em " + dias + " dia(s)",
                anti.getNextDoseDate());
    }

    /**
     * Dose vencida continua vencida todo dia. Sem o cooldown, o mesmo lembrete
     * sairia diariamente ate o tutor vacinar o pet.
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

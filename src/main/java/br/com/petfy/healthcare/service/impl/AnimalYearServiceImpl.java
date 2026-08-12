package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalYearCaregiverDTO;
import br.com.petfy.healthcare.domain.dto.AnimalYearDTO;
import br.com.petfy.healthcare.domain.dto.AnimalYearLapseDTO;
import br.com.petfy.healthcare.domain.dto.AnimalYearPendingDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.AnimalYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O ano do animal (Tela 48).
 *
 * <b>"Nao e retrospectiva."</b> <i>"Um resumo anual de produto costuma ser sentimental e inutil:
 * colagem de fotos, numero de passos, 'que ano incrivel'. Este e um documento clinico e social do
 * animal, e serve para levar ao veterinario. Por isso ele inclui o que deu errado."</i>
 *
 * <b>Essa frase e a especificacao deste servico.</b> Metade do codigo abaixo existe para achar o que
 * deu errado — os dias em que uma vacina esteve vencida, e as condicoes cronicas que ninguem
 * reavaliou. Sao as duas partes que nenhum resumo automatico costuma ter, e as unicas que fazem o
 * documento valer alguma coisa numa consulta.
 *
 * <b>Le tudo do animal e agrega em memoria, e isso e deliberado.</b> Um resumo anual roda uma vez por
 * ano, por animal, e o maior conjunto que ele toca — a serie de vacinas — tem dezenas de linhas. As
 * consultas que valeriam a pena empurrar para o banco ja foram: a contagem de eventos, a de quem
 * cuidou e a de dias na creche sao agregadas em SQL, porque essas sim leem centenas.
 */
@Service
@RequiredArgsConstructor
public class AnimalYearServiceImpl implements AnimalYearService {

    private final AnimalAccessGuard animalAccessGuard;
    private final TimelineRepository timelineRepository;
    private final AttendanceRepository attendanceRepository;
    private final CustodyRepository custodyRepository;
    private final AnimalWeightHistoryRepository weightRepository;
    private final VaccineRepository vaccineRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AnimalHealthConditionRepository conditionRepository;

    @Override
    @Transactional(readOnly = true)
    public AnimalYearDTO resumo(UUID animalId, LocalDate ate) {
        Animal animal = animalAccessGuard.requireLeitura(animalId);

        LocalDate fim = ate == null ? LocalDate.now() : ate;
        LocalDate inicio = fim.minusYears(1);

        LocalDateTime deHora = inicio.atStartOfDay();
        LocalDateTime ateHora = fim.atTime(23, 59, 59);

        TimelineRepository.Tamanho tamanho =
                timelineRepository.tamanhoNoPeriodo(animalId, deHora, ateHora);

        return AnimalYearDTO.builder()
                .animalId(animalId)
                .animalName(animal.getName())
                .from(inicio)
                .to(fim)
                .yearOfLife(anoDeVida(animal, fim))
                .records(tamanho.getEventos())
                .people(tamanho.getPessoas())
                .organizations(tamanho.getOrganizacoes())
                .daycareDays(attendanceRepository
                        .contarDiasNaCrecheNoPeriodo(animalId, inicio, fim))
                .boardingDays(diasHospedado(animalId, inicio, fim))
                .weightNow(pesoEm(animalId, inicio, fim, true))
                .weightBefore(pesoEm(animalId, inicio, fim, false))
                .appointments(healthRecordRepository
                        .findByAnimalAnimalIdOrderByEventDateDesc(animalId).stream()
                        .filter(r -> dentro(r.getEventDate(), inicio, fim))
                        .count())
                .vaccines(vaccineRepository
                        .findByAnimalAnimalIdOrderByApplicationDateDesc(animalId).stream()
                        .filter(v -> dentro(v.getApplicationDate(), inicio, fim))
                        .count())
                .antiparasitics(antiparasiticRepository
                        .findByAnimalAnimalIdOrderByApplicationDateDesc(animalId).stream()
                        .filter(a -> dentro(a.getApplicationDate(), inicio, fim))
                        .count())
                .lapses(vacinasVencidasNoPeriodo(animalId, inicio, fim))
                .notReassessed(naoReavaliadas(animalId, deHora))
                .caregivers(quemCuidou(animalId, deHora, ateHora))
                .build();
    }

    /**
     * <b>O calculo que faz este documento valer.</b> "A antirrabica ficou 23 dias vencida em julho."
     *
     * A irregularidade nao esta gravada em lugar nenhum, e nao deveria estar: ela e a leitura de duas
     * linhas — a dose que venceu e a que veio depois. Um campo "esteve vencida" seria um fato
     * derivado gravado a mao, e ficaria errado no dia em que alguem corrigisse uma data.
     *
     * <b>Por nome de vacina, e nao pela serie inteira.</b> A antirrabica vencer nao tem nada a ver com
     * a V10 estar em dia, e juntar as duas produziria um numero que nao corresponde a doenca nenhuma.
     *
     * <b>Recorta a janela nas duas pontas.</b> Uma irregularidade que comecou em novembro do ano
     * anterior e terminou em fevereiro conta os dias de fevereiro, e nao os quatro meses: o documento
     * e sobre o ano dele, e dizer "112 dias vencida" num ano em que ele esteve irregular por 45 seria
     * mentir contra o proprio animal.
     */
    private List<AnimalYearLapseDTO> vacinasVencidasNoPeriodo(UUID animalId,
                                                              LocalDate inicio, LocalDate fim) {
        Map<String, List<Vaccine>> porNome = vaccineRepository
                .findByAnimalAnimalIdOrderByApplicationDateDesc(animalId).stream()
                .filter(v -> v.getVaccineName() != null)
                .collect(Collectors.groupingBy(Vaccine::getVaccineName));

        List<AnimalYearLapseDTO> lacunas = new ArrayList<>();

        for (Map.Entry<String, List<Vaccine>> serie : porNome.entrySet()) {
            List<Vaccine> doses = serie.getValue().stream()
                    .filter(v -> v.getApplicationDate() != null)
                    .sorted(Comparator.comparing(Vaccine::getApplicationDate))
                    .collect(Collectors.toList());

            for (int i = 0; i < doses.size(); i++) {
                LocalDate venceu = doses.get(i).getNextDoseDate();

                if (venceu == null) {
                    continue;
                }

                // a proxima dose DESTA vacina, se houve
                LocalDate regularizou = i + 1 < doses.size()
                        ? doses.get(i + 1).getApplicationDate()
                        : null;

                // ainda vencida: o fim da janela e ate onde se pode afirmar irregularidade
                LocalDate fimDaLacuna = regularizou == null ? fim : regularizou;

                if (!fimDaLacuna.isAfter(venceu)) {
                    continue;
                }

                LocalDate deFato = venceu.isBefore(inicio) ? inicio : venceu;
                LocalDate ateDeFato = fimDaLacuna.isAfter(fim) ? fim : fimDaLacuna;

                if (!ateDeFato.isAfter(deFato)) {
                    continue;
                }

                lacunas.add(AnimalYearLapseDTO.builder()
                        .vaccineName(serie.getKey())
                        .overdueSince(deFato)
                        .regularizedOn(regularizou)
                        .days(ChronoUnit.DAYS.between(deFato, ateDeFato))
                        .build());
            }
        }

        lacunas.sort(Comparator.comparing(AnimalYearLapseDTO::getOverdueSince));

        return lacunas;
    }

    /**
     * "A displasia dele nao foi reavaliada desde 2023."
     *
     * <b>O criterio e a propria linha da condicao</b>, e nao uma inferencia sobre atendimentos: se
     * ninguem editou aquele registro dentro da janela, ninguem o reavaliou. Deduzir da linha do tempo
     * — "houve consulta, logo foi reavaliada" — afirmaria algo que nenhum registro sustenta, e num
     * documento que vai ao veterinario isso e pior do que nao dizer nada.
     *
     * <b>So as ABERTAS.</b> Uma alergia resolvida em 2022 nao precisa de reavaliacao, e lista-la
     * transformaria a secao "o que ficou para tras" numa lista de tudo que ja aconteceu.
     */
    private List<AnimalYearPendingDTO> naoReavaliadas(UUID animalId, LocalDateTime inicio) {
        return conditionRepository.findByAnimalOrdenadasPorRelevancia(animalId).stream()
                .filter(c -> c.getResolvedAt() == null)
                .filter(c -> ultimoToque(c).isBefore(inicio))
                .map(c -> AnimalYearPendingDTO.builder()
                        .description(c.getDescription())
                        .lastTouchedOn(ultimoToque(c).toLocalDate())
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDateTime ultimoToque(AnimalHealthCondition condicao) {
        if (condicao.getUpdateDate() != null) {
            return condicao.getUpdateDate();
        }

        if (condicao.getCreationDate() != null) {
            return condicao.getCreationDate();
        }

        return condicao.getSince() == null
                ? LocalDateTime.now()
                : condicao.getSince().atStartOfDay();
    }

    private List<AnimalYearCaregiverDTO> quemCuidou(UUID animalId,
                                                    LocalDateTime de, LocalDateTime ate) {
        return timelineRepository.quemCuidouNoPeriodo(animalId, de, ate).stream()
                .map(linha -> AnimalYearCaregiverDTO.builder()
                        .personName(linha.getPessoa())
                        .organizationName(linha.getOrganizacao())
                        .records(linha.getRegistros())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * "E 7 de hospedagem."
     *
     * Soma as custodias transitorias de organizacao que se sobrepoem a janela, recortando nas duas
     * pontas: uma estadia que comecou em julho e terminou em agosto conta so os dias dentro do ano de
     * que o documento fala.
     */
    private long diasHospedado(UUID animalId, LocalDate inicio, LocalDate fim) {
        return custodyRepository.findByAnimalAnimalIdOrderByStartedAtAsc(animalId).stream()
                .filter(c -> c.getNature() == CustodyNature.TRANSITORIA)
                .filter(c -> c.getHolderOrganization() != null)
                .mapToLong(c -> {
                    LocalDate comecou = c.getStartedAt().toLocalDate();
                    LocalDate terminou = c.getEndedAt() == null
                            ? fim : c.getEndedAt().toLocalDate();

                    LocalDate de = comecou.isBefore(inicio) ? inicio : comecou;
                    LocalDate ate = terminou.isAfter(fim) ? fim : terminou;

                    return ate.isAfter(de) ? ChronoUnit.DAYS.between(de, ate) : 0;
                })
                .sum();
    }

    /**
     * O peso no fim e no comeco da janela.
     *
     * <b>A pesagem mais proxima de cada ponta, e nao a media.</b> "8,6 kg, era 7,5 kg em agosto
     * passado" e uma comparacao entre dois momentos, e uma media de doze meses esconderia exatamente
     * o que a frase mostra: que ele engordou.
     */
    private Double pesoEm(UUID animalId, LocalDate inicio, LocalDate fim, boolean oMaisRecente) {
        List<AnimalWeightHistory> naJanela = weightRepository
                .findByAnimalAnimalIdOrderByMeasuredAtDesc(animalId).stream()
                .filter(p -> dentro(p.getMeasuredAt(), inicio, fim))
                .collect(Collectors.toList());

        if (naJanela.isEmpty()) {
            return null;
        }

        // a consulta ja vem do mais recente para o mais antigo
        return oMaisRecente
                ? naJanela.get(0).getWeight()
                : naJanela.get(naJanela.size() - 1).getWeight();
    }

    /**
     * "Setimo ano dele."
     *
     * A partir de {@code bornDate}, e nao do cadastro: o animal que chegou ao Petfy aos cinco anos
     * esta no setimo ano de vida, e nao no segundo. O primeiro ano de vida e o ano 1.
     */
    private Integer anoDeVida(Animal animal, LocalDate fim) {
        if (animal.getBornDate() == null) {
            return null;
        }

        return (int) ChronoUnit.YEARS.between(animal.getBornDate(), fim) + 1;
    }

    private boolean dentro(LocalDate data, LocalDate inicio, LocalDate fim) {
        return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
    }

}

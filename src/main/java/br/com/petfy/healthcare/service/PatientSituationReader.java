package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.OrganizationPatientsSummaryDTO;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A situacao de saude de VARIOS animais de uma vez — o que a coluna "situacao" e o cabecalho da
 * Tela 03 precisavam e nao existia.
 *
 * <b>Tudo aqui e em lote, e isso e a razao da classe existir.</b> A tela mostra 20 animais por
 * pagina e conta sobre 318; perguntar animal por animal seria 318 leituras para desenhar uma
 * tabela — o motivo pelo qual a propria tela dizia que a coluna nao dava para montar no cliente.
 * Fazer o mesmo laco no servidor teria escondido o defeito em vez de resolve-lo.
 */
@Component
@RequiredArgsConstructor
public class PatientSituationReader {

    private final VaccineRepository vaccineRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final AnimalHealthConditionRepository animalHealthConditionRepository;
    private final VaccineStatusCalculator vaccineStatusCalculator;

    /** A janela do "vencendo": os mesmos 30 dias do cabecalho e da agenda do tutor. */
    public static final int JANELA_DIAS = 30;

    /**
     * O que a tabela mostra por animal.
     *
     * @param status       a dose mais urgente da carteira
     * @param ultimaVisita o registro de saude mais recente, ou nulo
     * @param emTratamento tem condicao de saude aberta
     */
    public record Situacao(VaccineStatus status, LocalDate ultimaVisita, boolean emTratamento) {

        static Situacao desconhecida() {
            return new Situacao(VaccineStatus.NO_NEXT_DOSE, null, false);
        }
    }

    public Map<UUID, Situacao> de(Collection<UUID> animalIds) {
        if (animalIds.isEmpty()) {
            // `in ()` nao e SQL valido, e a pergunta tambem nao faz sentido: sem animal nao ha
            // situacao a responder
            return Map.of();
        }

        LocalDate hoje = LocalDate.now();

        Map<UUID, VaccineStatus> statusPorAnimal = statusPorAnimal(animalIds, hoje);
        Map<UUID, LocalDate> ultimaVisita = ultimaVisitaPorAnimal(animalIds);
        Set<UUID> emTratamento = Set.copyOf(animalHealthConditionRepository.idsComCondicaoAberta(animalIds));

        Map<UUID, Situacao> situacoes = new HashMap<>();
        for (UUID animalId : animalIds) {
            situacoes.put(animalId, new Situacao(
                    statusPorAnimal.getOrDefault(animalId, VaccineStatus.NO_NEXT_DOSE),
                    ultimaVisita.get(animalId),
                    emTratamento.contains(animalId)));
        }

        return situacoes;
    }

    public Situacao de(UUID animalId) {
        return de(List.of(animalId)).getOrDefault(animalId, Situacao.desconhecida());
    }

    /**
     * Os quatro numeros do cabecalho, sobre o conjunto inteiro.
     *
     * <b>Vencidas entram no "vencendo em 30 dias".</b> Separar as duas daria ao cabecalho um
     * numero que esconde o pior caso: quem ja passou do prazo e mais urgente, e nao menos.
     */
    public OrganizationPatientsSummaryDTO resumo(Collection<UUID> animalIds) {
        if (animalIds.isEmpty()) {
            return OrganizationPatientsSummaryDTO.builder().build();
        }

        Map<UUID, Situacao> situacoes = de(animalIds);

        long vencendo = situacoes.values().stream()
                .filter(situacao -> situacao.status() == VaccineStatus.OVERDUE
                        || situacao.status() == VaccineStatus.DUE_SOON)
                .count();

        long emTratamento = situacoes.values().stream().filter(Situacao::emTratamento).count();

        LocalDate inicioDoMes = LocalDate.now().withDayOfMonth(1);

        return OrganizationPatientsSummaryDTO.builder()
                .dueIn30Days(vencendo)
                .underTreatment(emTratamento)
                .seenThisMonth(healthRecordRepository.contarAnimaisAtendidosDesde(animalIds, inicioDoMes))
                .total(animalIds.size())
                .build();
    }

    /**
     * A dose mais urgente de cada animal.
     *
     * <b>Vencida vence tudo, depois vencendo, depois em dia</b> — quem le a tabela decide a quem
     * ligar hoje, e para isso o pior caso e a unica informacao que serve. Um animal com dez doses
     * em dia e uma vencida nao esta em dia.
     */
    private Map<UUID, VaccineStatus> statusPorAnimal(Collection<UUID> animalIds, LocalDate hoje) {
        Map<UUID, VaccineStatus> pior = new HashMap<>();

        for (Vaccine vacina : vaccineRepository.findByAnimalAnimalIdIn(animalIds)) {
            UUID animalId = vacina.getAnimal().getAnimalId();
            VaccineStatus status = vaccineStatusCalculator.classify(vacina.getNextDoseDate(), hoje, JANELA_DIAS);

            pior.merge(animalId, status, PatientSituationReader::oPior);
        }

        return pior;
    }

    private static VaccineStatus oPior(VaccineStatus a, VaccineStatus b) {
        return urgencia(a) >= urgencia(b) ? a : b;
    }

    private static int urgencia(VaccineStatus status) {
        return switch (status) {
            case OVERDUE -> 3;
            case DUE_SOON -> 2;
            case UP_TO_DATE -> 1;
            // sem prazo nao e "em dia": pode ser dose unica, e pode ser carteira nao registrada.
            // Fica embaixo de tudo para nao mascarar um vencimento real.
            case NO_NEXT_DOSE -> 0;
        };
    }

    private Map<UUID, LocalDate> ultimaVisitaPorAnimal(Collection<UUID> animalIds) {
        Map<UUID, LocalDate> ultima = new HashMap<>();

        for (Object[] linha : healthRecordRepository.ultimaVisitaPorAnimal(animalIds)) {
            ultima.put((UUID) linha[0], (LocalDate) linha[1]);
        }

        return ultima;
    }

}

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionFulfillmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.PetTutorInviteRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.ConsentService;
import br.com.petfy.healthcare.service.DueItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Tudo que cobra acao de alguem, num lugar so.
 *
 * <b>Derivada, e nao gravada.</b> Nao existe tabela de pendencia: cada item e lido da
 * sua fonte a cada chamada. Uma tabela precisaria ser mantida em sincronia com cinco
 * fontes, e a primeira a divergir cobraria algo que ja foi feito - ou pior, deixaria de
 * cobrar algo que falta. E divergencia silenciosa foi a familia dos seis bugs da Fase 4.
 *
 * <b>Generaliza a agenda de vacinas</b>, que o roadmap ja reconhecia como "a unica parte
 * do sistema que produz informacao em vez de devolver o que foi gravado". A agenda
 * continua existindo com seu contrato; esta e a lista de que ela passa a ser um caso.
 *
 * <b>A quem se cobra: a quem alcanca o animal agora.</b> O produto diz "a quem tem
 * custodia agora, nao a quem cadastrou o animal", e a regra que ele recusa e cobrar um
 * dono historico. Isso ja esta satisfeito, porque alcance e sempre atual - concessao nao
 * e herdada na transferencia, entao ninguem que saiu continua sendo cobrado. Manter o
 * co-tutor na conta e deliberado: quem divide o cuidado divide a cobranca, e foi o que o
 * 8a decidiu para o lembrete.
 */
@Service
@RequiredArgsConstructor
public class DueItemServiceImpl implements DueItemService {

    private final CurrentPersonProvider currentPersonProvider;
    private final AnimalRepository animalRepository;
    private final VaccineRepository vaccineRepository;
    private final AntiparasiticRepository antiparasiticRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final CareInstructionFulfillmentRepository fulfillmentRepository;
    private final PetTutorInviteRepository petTutorInviteRepository;
    private final ConsentService consentService;

    @Override
    @Transactional(readOnly = true)
    public List<DueItemResponseDTO> doAutenticado(int windowDays) {
        Person person = currentPersonProvider.require();
        LocalDate hoje = LocalDate.now();
        LocalDate limite = hoje.plusDays(windowDays);

        List<Animal> alcancados = animalRepository.findAlcancadosPor(
                person.getPersonId(), LocalDateTime.now());

        List<DueItemResponseDTO> itens = new ArrayList<>();

        itens.addAll(dosesDeVacina(person, hoje, limite));
        itens.addAll(antiparasitarios(person, hoje, limite));
        itens.addAll(orientacoes(alcancados, hoje));
        itens.addAll(convitesPendentes(person, alcancados));
        itens.addAll(consentimentoPendente());

        // do mais atrasado ao menos urgente. O que nao tem data vai primeiro: e o
        // consentimento, que bloqueia o resto - deixa-lo no fim faria o usuario percorrer
        // a lista inteira para descobrir por que nada funciona
        itens.sort(Comparator.comparing(DueItemResponseDTO::getDueOn,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        return itens;
    }

    private List<DueItemResponseDTO> dosesDeVacina(Person person, LocalDate hoje, LocalDate limite) {
        return vaccineRepository.findAlcancadasPor(person.getPersonId(), LocalDateTime.now())
                .stream()
                .filter(v -> v.getNextDoseDate() != null && !v.getNextDoseDate().isAfter(limite))
                .map(v -> item(DueItemKind.DOSE_DE_VACINA, v.getVaccineId(), v.getAnimal(),
                        v.getVaccineName(), v.getNextDoseDate(), hoje))
                .toList();
    }

    private List<DueItemResponseDTO> antiparasitarios(Person person, LocalDate hoje, LocalDate limite) {
        return antiparasiticRepository.findAlcancadosPor(person.getPersonId(), LocalDateTime.now())
                .stream()
                .filter(a -> a.getNextDoseDate() != null && !a.getNextDoseDate().isAfter(limite))
                .map(a -> item(DueItemKind.ANTIPARASITARIO, a.getAntiparasiticId(), a.getAnimal(),
                        a.getName(), a.getNextDoseDate(), hoje))
                .toList();
    }

    /**
     * A orientacao vira pendencia quando o intervalo desde o ultimo cumprimento passou.
     *
     * Nunca cumprida cobra desde o inicio. Cumprida ontem, com intervalo de um dia, cobra
     * hoje. E o ultimo cumprimento vai na resposta - e o que impede dois tutores darem o
     * mesmo remedio sem saber que o outro deu.
     */
    private List<DueItemResponseDTO> orientacoes(List<Animal> alcancados, LocalDate hoje) {
        if (alcancados.isEmpty()) {
            return List.of();
        }

        List<UUID> animalIds = alcancados.stream().map(Animal::getAnimalId).toList();

        return careInstructionRepository.findVigentesNosAnimais(animalIds, hoje)
                .stream()
                .map(instrucao -> pendenciaDe(instrucao, hoje))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private DueItemResponseDTO pendenciaDe(CareInstruction instrucao, LocalDate hoje) {
        var ultimo = fulfillmentRepository
                .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(
                        instrucao.getCareInstructionId());

        LocalDate vence = ultimo
                .map(f -> f.getFulfilledAt().toLocalDate().plusDays(instrucao.getIntervalDays()))
                .orElse(instrucao.getStartsOn());

        // ainda dentro do intervalo: nao cobra. Cobrar antes da hora treina o usuario a
        // ignorar a lista, que e o caminho mais curto para a pendencia perder valor
        if (vence.isAfter(hoje)) {
            return null;
        }

        return DueItemResponseDTO.builder()
                .kind(DueItemKind.ORIENTACAO)
                .sourceId(instrucao.getCareInstructionId())
                .animalId(instrucao.getAnimal().getAnimalId())
                .animalName(instrucao.getAnimal().getName())
                .description(instrucao.getDescription())
                .dueOn(vence)
                .overdue(vence.isBefore(hoje))
                .lastFulfilledAt(ultimo.map(f -> f.getFulfilledAt()).orElse(null))
                .lastFulfilledByName(ultimo.map(f -> f.getConfirmedBy().getName()).orElse(null))
                .build();
    }

    /**
     * Convite aguardando resposta cobra <b>quem convidou</b>.
     *
     * Quem recebeu talvez nem tenha conta, e o produto nao cobra quem nao alcanca. Para
     * quem convidou, e acao pendente de verdade: o link foi entregue a mao, e so ele sabe
     * se precisa reenviar.
     */
    private List<DueItemResponseDTO> convitesPendentes(Person person, List<Animal> alcancados) {
        if (alcancados.isEmpty()) {
            return List.of();
        }

        LocalDateTime agora = LocalDateTime.now();

        return alcancados.stream()
                .flatMap(animal -> petTutorInviteRepository
                        .findByAnimalAnimalIdOrderByCreationDateDesc(animal.getAnimalId())
                        .stream()
                        .filter(i -> i.isUsable(agora))
                        .filter(i -> i.getCreatedBy() != null
                                && i.getCreatedBy().getPersonId().equals(person.getPersonId()))
                        .map(i -> DueItemResponseDTO.builder()
                                .kind(DueItemKind.CONVITE_PENDENTE)
                                .sourceId(i.getPetTutorInviteId())
                                .animalId(animal.getAnimalId())
                                .animalName(animal.getName())
                                .description(i.getEmail())
                                .dueOn(i.getExpiresAt().toLocalDate())
                                .overdue(false)
                                .build()))
                .toList();
    }

    /**
     * Consentimento pendente e <b>estado, e nao prazo</b>: sem data, e cobrando agora.
     *
     * Entra na mesma lista de proposito. Ele bloqueia o resto do produto, e ter um lugar
     * separado para avisar isso significaria o usuario descobrir o bloqueio ao bater
     * nele.
     */
    private List<DueItemResponseDTO> consentimentoPendente() {
        return !consentService.statusDoAutenticado().isTudoAceito()
                ? List.of(DueItemResponseDTO.builder()
                        .kind(DueItemKind.CONSENTIMENTO_PENDENTE)
                        .description("Aceite da versao vigente da politica de privacidade")
                        .overdue(true)
                        .build())
                : List.of();
    }

    private DueItemResponseDTO item(DueItemKind kind, UUID sourceId, Animal animal,
                                    String descricao, LocalDate vence, LocalDate hoje) {
        return DueItemResponseDTO.builder()
                .kind(kind)
                .sourceId(sourceId)
                .animalId(animal.getAnimalId())
                .animalName(animal.getName())
                .description(descricao)
                .dueOn(vence)
                .overdue(vence.isBefore(hoje))
                .build();
    }

}

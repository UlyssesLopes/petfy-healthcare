package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.DueItemResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.DueItemKind;
import br.com.petfy.healthcare.domain.entity.DueItemSilence;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.DueItemSilenceRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.springframework.http.HttpStatus;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final DueItemSilenceRepository silenceRepository;

    /**
     * A janela usada para conferir se a pendencia existe antes de silenciar.
     *
     * Larga de proposito, e diferente do default do feed: o item pode estar fora dos 30 dias
     * e ainda assim ser algo que a pessoa quer calar de vespera - a dose do ano que vem, por
     * exemplo, que ela nao quer ver toda vez que abre a lista com janela grande.
     */
    private static final int JANELA_DE_CONFERENCIA_EM_DIAS = 3650;

    @Override
    @Transactional(readOnly = true)
    public List<DueItemResponseDTO> doAutenticado(int windowDays, boolean includeSilenced) {
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

        // Uma consulta para o feed inteiro, e nao uma por item: perguntar "esta silenciada?"
        // por pendencia seria N consultas na leitura que a area do tutor faz todo dia.
        Set<String> silenciadas = silenciadasDe(person);

        itens.forEach(item -> item.setSilenced(silenciadas.contains(chave(item.getKind(), item.getSourceId()))));

        if (!includeSilenced) {
            itens.removeIf(DueItemResponseDTO::isSilenced);
        }

        // do mais atrasado ao menos urgente. O que nao tem data vai primeiro: e o
        // consentimento, que bloqueia o resto - deixa-lo no fim faria o usuario percorrer
        // a lista inteira para descobrir por que nada funciona
        itens.sort(Comparator.comparing(DueItemResponseDTO::getDueOn,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        return itens;
    }

    /**
     * <b>So se silencia o que esta sendo cobrado de voce.</b>
     *
     * A pendencia e derivada, entao nao ha o que validar por chave estrangeira: sem esta
     * conferencia qualquer par de tipo e id gravaria uma linha, e a tabela acumularia
     * silencio de pendencia que nunca existiu. A regra tambem e a mais fiel ao 5.3 - a acao
     * mora <i>na</i> pendencia, e nao existe silenciar algo que nao esta na sua lista.
     *
     * A janela usada na conferencia e larga de proposito: o item pode estar fora dos 30 dias
     * do feed default e ainda assim ser algo que a pessoa quer calar de vespera.
     */
    @Override
    @Transactional
    public void silenciar(DueItemKind kind, UUID sourceId) {
        recusarConsentimento(kind);

        Person person = currentPersonProvider.require();

        boolean existe = doAutenticado(JANELA_DE_CONFERENCIA_EM_DIAS, true).stream()
                .anyMatch(item -> item.getKind() == kind && sourceId.equals(item.getSourceId()));

        if (!existe) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.DUE_ITEM_NOT_FOUND.getMessage(),
                    ErrorMessageEnum.DUE_ITEM_NOT_FOUND.getCode(),
                    HttpStatus.NOT_FOUND);
        }

        // idempotente: silenciar duas vezes e o mesmo que silenciar uma, e o indice unico no
        // banco garante isso mesmo em duas requisicoes simultaneas
        if (silenceRepository.findByPersonPersonIdAndKindAndSourceId(
                person.getPersonId(), kind, sourceId).isPresent()) {
            return;
        }

        silenceRepository.save(DueItemSilence.builder()
                .person(person)
                .kind(kind)
                .sourceId(sourceId)
                .silencedAt(LocalDateTime.now())
                .build());
    }

    /**
     * Voltar a cobrar nao confere se a pendencia existe.
     *
     * <b>E deliberado:</b> se ela deixou de existir, o silencio dela e lixo e apagar e certo
     * de qualquer forma. Exigir que exista deixaria a pessoa sem como limpar o que silenciou
     * de um registro que ja saiu.
     */
    @Override
    @Transactional
    public void voltarACobrar(DueItemKind kind, UUID sourceId) {
        Person person = currentPersonProvider.require();

        silenceRepository.findByPersonPersonIdAndKindAndSourceId(person.getPersonId(), kind, sourceId)
                .ifPresent(silenceRepository::delete);
    }

    /**
     * Consentimento nao se silencia.
     *
     * O 4.2 diz que ele <b>bloqueia o resto do produto</b>, e o servico o devolve sem data,
     * cobrando agora. Silencia-lo esconderia o bloqueio, e o usuario descobriria ao bater
     * nele - exatamente o cenario que o documento descreve como o que nao pode acontecer. E
     * ele nao tem {@code sourceId}, entao nao havia nem o que gravar.
     */
    private void recusarConsentimento(DueItemKind kind) {
        if (kind == DueItemKind.CONSENTIMENTO_PENDENTE) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CONSENT_CANNOT_BE_SILENCED.getMessage(),
                    ErrorMessageEnum.CONSENT_CANNOT_BE_SILENCED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private Set<String> silenciadasDe(Person person) {
        return silenceRepository.findByPersonPersonId(person.getPersonId())
                .stream()
                .map(s -> chave(s.getKind(), s.getSourceId()))
                .collect(Collectors.toSet());
    }

    /** Tipo mais fonte, que e o que identifica uma pendencia derivada. */
    private String chave(DueItemKind kind, UUID sourceId) {
        return kind + ":" + sourceId;
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

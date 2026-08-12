package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ColonyAnimalDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalSightingRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ColonyService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A lista da colônia (Tela 43).
 *
 * <b>"Visto por último" é o sinal vital daqui</b>, e é ele que separa esta lista da do abrigo: um
 * gato de rua não falta à creche nem deixa de comer em casa — ele some.
 *
 * <b>Três consultas para a lista inteira, e não três por linha.</b> Os animais, os últimos
 * avistamentos em lote e as orientações vigentes em lote. Uma colônia de catorze gatos custaria
 * quarenta e três idas ao banco pelo caminho ingênuo — e esta é a tela mais aberta do grupo.
 */
@Service
@RequiredArgsConstructor
public class ColonyServiceImpl implements ColonyService {

    /**
     * O corte de "sumido", em dias.
     *
     * <b>Quinze é do desenho</b>, e é um número de produto e não de código: menos que isso
     * acusaria todo gato que passou uma semana em outro quarteirão, e mais deixaria a busca
     * começar tarde demais.
     */
    private static final int DIAS_PARA_SUMIDO = 15;

    private final CustodyRepository custodyRepository;
    private final AnimalSightingRepository animalSightingRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final MembershipRepository membershipRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    @Override
    @Transactional(readOnly = true)
    public List<ColonyAnimalDTO> listar(String filtro) {
        Organization grupo = exigirGrupoDeclarado();

        List<Animal> animais = custodyRepository
                .buscarEmCursoDaOrganizacao(grupo.getOrganizationId(), "%",
                        org.springframework.data.domain.Pageable.unpaged())
                .map(Custody::getAnimal)
                .getContent();

        if (animais.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = animais.stream().map(Animal::getAnimalId).toList();
        LocalDate hoje = LocalDate.now();

        /* O primeiro de cada animal vence: a consulta ja ordena por `recordedAt desc`, e o
           `merge` mantem o primeiro visto. Sem isso, duas pessoas marcando o mesmo dia fariam a
           celula alternar de nome entre dois carregamentos da mesma tela. */
        Map<UUID, AnimalSightingRepository.UltimoAvistamento> ultimos = animalSightingRepository
                .ultimoDeCada(ids).stream()
                .collect(Collectors.toMap(
                        AnimalSightingRepository.UltimoAvistamento::getAnimalId,
                        Function.identity(),
                        (primeiro, seguinte) -> primeiro));

        Map<UUID, String> emTratamento = careInstructionRepository
                .findVigentesNosAnimais(ids, hoje).stream()
                .collect(Collectors.toMap(
                        instrucao -> instrucao.getAnimal().getAnimalId(),
                        CareInstruction::getDescription,
                        (primeira, seguinte) -> primeira));

        return animais.stream()
                .map(animal -> toDto(animal, ultimos.get(animal.getAnimalId()),
                        emTratamento.get(animal.getAnimalId()), hoje))
                .filter(linha -> passaNoFiltro(linha, filtro))
                .toList();
    }

    /**
     * Os quatro filtros do desenho.
     *
     * <b>"Sem informação" NÃO é "sumido", e essa é a distinção que a tela faz e o filtro tem de
     * respeitar.</b> Um gato cadastrado ontem, que ninguém marcou ainda, não está desaparecido —
     * ele está sem registro. Contá-lo entre os sumidos mandaria o grupo procurar um animal que
     * está na praça, e ensinaria a desconfiar do número.
     */
    private boolean passaNoFiltro(ColonyAnimalDTO linha, String filtro) {
        if (filtro == null || filtro.isBlank() || "TODOS".equalsIgnoreCase(filtro)) {
            return true;
        }

        return switch (filtro.toUpperCase()) {
            case "FALTA_CASTRAR" -> !Boolean.TRUE.equals(linha.getNeutered());
            case "EM_TRATAMENTO" -> linha.getOngoingCare() != null;
            case "SUMIDOS" -> linha.getDaysSinceLastSeen() != null
                    && linha.getDaysSinceLastSeen() > DIAS_PARA_SUMIDO;
            default -> throw new PetfyHealthcareException(
                    "filtro desconhecido: use TODOS, FALTA_CASTRAR, EM_TRATAMENTO ou SUMIDOS",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        };
    }

    private ColonyAnimalDTO toDto(Animal animal,
                                  AnimalSightingRepository.UltimoAvistamento ultimo,
                                  String tratamento,
                                  LocalDate hoje) {
        return ColonyAnimalDTO.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .neutered(animal.getCastrated())
                .neuteredAt(animal.getCastratedAt())
                .neuteringScheduledFor(animal.getNeuteringScheduledFor())
                .ongoingCare(tratamento)
                .lastSeenOn(ultimo == null ? null : ultimo.getSeenOn())
                .lastSeenBy(ultimo == null ? null : ultimo.getQuem())
                .daysSinceLastSeen(ultimo == null ? null
                        : ChronoUnit.DAYS.between(ultimo.getSeenOn(), hoje))
                .build();
    }

    /** Exige organização declarada e vínculo ativo, como todo o resto do grupo. */
    private Organization exigirGrupoDeclarado() {
        Person eu = currentPersonProvider.require();

        Organization grupo = currentProfessionalProvider.organizacaoDeclarada(eu)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));

        if (membershipRepository
                .findAtivoDaPessoaNaOrganizacao(eu.getPersonId(), grupo.getOrganizationId())
                .isEmpty()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NOT_A_GROUP_MEMBER.getMessage(),
                    ErrorMessageEnum.NOT_A_GROUP_MEMBER.getCode(),
                    HttpStatus.FORBIDDEN);
        }

        return grupo;
    }

}

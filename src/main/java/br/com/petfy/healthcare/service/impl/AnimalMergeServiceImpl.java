package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestResponseDTO;
import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestResponseDTO.Divergencia;
import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestResponseDTO.LadoDaUniao;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalMergeRequest;
import br.com.petfy.healthcare.domain.entity.AnimalMergeStatus;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.AnimalMergeRequestRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalMergeService;
import br.com.petfy.healthcare.service.AnimalMerger;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A uniao de dois cadastros do mesmo animal (Tela 32).
 *
 * <b>QUEM PERCEBE NAO E QUEM DECIDE, e essa separacao e a tela inteira.</b> A clinica tem o leitor
 * de microchip na mao e ve a duplicata; quem responde pelo animal e quem aceita. "Voce nao pode
 * unir sozinha. (...) E a mesma regra da transferencia: ninguem mexe na vida registrada de um
 * animal sem quem responde por ele."
 *
 * <b>Por que a uniao nao pode ser da clinica:</b> ela e irreversivel. Desfazer exigiria saber de
 * qual cadastro cada evento veio, o que so seria possivel gravando a origem em cada uma das
 * dezoito tabelas que apontam para animal. Ato irreversivel sobre a vida registrada de um animal
 * e de quem responde por ele, e nao de quem tem acesso.
 */
@Service
@RequiredArgsConstructor
public class AnimalMergeServiceImpl implements AnimalMergeService {

    private final AnimalRepository animalRepository;
    private final AnimalMergeRequestRepository mergeRequestRepository;
    private final CustodyRepository custodyRepository;
    private final TimelineRepository timelineRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final AnimalMerger animalMerger;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LadoDaUniao> duplicatasDe(UUID animalId) {
        Animal animal = animalAccessGuard.requireLeitura(animalId);

        if (animal.getMicrochipNumber() == null || animal.getMicrochipNumber().isBlank()) {
            return List.of();
        }

        /*
         * NAO FILTRA POR QUEM QUEM PEDE ALCANCA, e isso e deliberado.
         *
         * A clinica que acabou de cadastrar o animal quase nunca alcanca o cadastro antigo — e
         * justamente por isso ela nao sabia que ele existia. Esconder a duplicata de quem nao a
         * alcanca faria a deteccao nao funcionar exatamente no caso que ela existe para resolver.
         *
         * O que atravessa aqui e o minimo para a comparacao da tela: nome, especie, raca,
         * microchip, quem responde e o tamanho da linha do tempo. Nao vai um evento sequer — o
         * prontuario continua protegido pelo escopo, e quem pede a uniao nao le nada do outro
         * cadastro alem de "ele existe e e deste tamanho".
         */
        return animalRepository
                .findOutrosComMicrochip(animal.getMicrochipNumber(), animalId)
                .stream()
                .map(this::ladoDe)
                .toList();
    }

    @Override
    @Transactional
    public AnimalMergeRequestResponseDTO pedir(UUID animalId, AnimalMergeRequestDTO request) {
        /*
         * Leitura nos DOIS, e nao custodia em nenhum: quem pede e quem percebeu, e ela alcanca
         * pelo menos o cadastro que ela mesma criou. Exigir custodia aqui trancaria o pedido para
         * a unica pessoa que tem como perceber a duplicata.
         */
        Animal sobrevivente = animalAccessGuard.requireLeitura(animalId);
        Animal absorvido = animalAccessGuard.requireLeitura(request.getAbsorbedAnimalId());

        if (sobrevivente.getAnimalId().equals(absorvido.getAnimalId())) {
            throw new PetfyHealthcareException(
                    "um cadastro nao pode ser unido a ele mesmo",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }

        exigirNaoAbsorvido(sobrevivente);
        exigirNaoAbsorvido(absorvido);

        /* Confere os dois sentidos: duas clinicas podem pedir a mesma uniao em direcoes opostas,
           e para quem decide sao o mesmo pedido — com respostas que se contradizem. */
        mergeRequestRepository
                .findPendenteEntre(sobrevivente.getAnimalId(), absorvido.getAnimalId(),
                        AnimalMergeStatus.PENDENTE)
                .ifPresent(existente -> {
                    throw new PetfyHealthcareException(
                            ErrorMessageEnum.MERGE_REQUEST_ALREADY_PENDING.getMessage(),
                            ErrorMessageEnum.MERGE_REQUEST_ALREADY_PENDING.getCode(),
                            HttpStatus.CONFLICT);
                });

        Person eu = currentPersonProvider.require();

        AnimalMergeRequest pedido = mergeRequestRepository.save(AnimalMergeRequest.builder()
                .absorbed(absorvido)
                .surviving(sobrevivente)
                .requestedBy(eu)
                .organization(currentProfessionalProvider.organizacaoDeclarada(eu).orElse(null))
                .reason(request.getReason().trim())
                .status(AnimalMergeStatus.PENDENTE)
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnimalMergeRequestResponseDTO> pendentesDe(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return mergeRequestRepository
                .findBySurvivingAnimalIdAndStatus(animalId, AnimalMergeStatus.PENDENTE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnimalMergeRequestResponseDTO> historicoDe(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return mergeRequestRepository.findEnvolvendo(animalId).stream().map(this::toResponse).toList();
    }

    /**
     * O aceite, e a ordem dele importa.
     *
     * <b>As divergencias sao calculadas e gravadas ANTES de mover qualquer coisa.</b> "Onde ha
     * conflito, o cadastro mais antigo prevalece e o outro valor fica guardado no evento da
     * uniao" — e um valor descartado que nao foi guardado no instante em que se descartou nao
     * volta nunca.
     *
     * <b>O sobrevivente NAO recebe os campos do absorvido</b>, nem quando os dele estao vazios.
     * Preencher o que falta parece gentileza e e escolha em silencio: a tela promete que "nada e
     * escolhido em silencio", e um nome que aparece do nada no cadastro de quem decidiu quebraria
     * exatamente essa promessa. O valor fica guardado, e quem quiser o copia olhando.
     */
    @Override
    @Transactional
    public AnimalMergeRequestResponseDTO aceitar(UUID animalMergeRequestId) {
        AnimalMergeRequest pedido = pendente(animalMergeRequestId);

        /* Quem decide e quem RESPONDE pelo sobrevivente. E aqui que o "voce nao pode unir
           sozinha" vira 403 para a clinica que pediu. */
        animalAccessGuard.requireCustodia(pedido.getSurviving().getAnimalId());

        Animal absorvido = pedido.getAbsorbed();
        Animal sobrevivente = pedido.getSurviving();

        exigirNaoAbsorvido(absorvido);
        exigirNaoAbsorvido(sobrevivente);

        pedido.setDiscardedValues(comoJson(divergenciasEntre(sobrevivente, absorvido)));

        animalMerger.mover(absorvido.getAnimalId(), sobrevivente.getAnimalId());

        /* O contexto foi limpo pelo merger, entao a entidade precisa ser relida antes de gravar —
           salvar a instancia antiga sobrescreveria com um estado anterior ao movimento. */
        Animal absorvidoRelido = animalRepository.findById(absorvido.getAnimalId())
                .orElseThrow(this::naoEncontrado);
        absorvidoRelido.setMergedIntoAnimalId(sobrevivente.getAnimalId());
        animalRepository.save(absorvidoRelido);

        AnimalMergeRequest relido = mergeRequestRepository.findById(animalMergeRequestId)
                .orElseThrow(this::naoEncontrado);
        relido.setStatus(AnimalMergeStatus.ACEITO);
        relido.setDecidedBy(currentPersonProvider.require());
        relido.setDecidedAt(LocalDateTime.now());

        return toResponse(mergeRequestRepository.save(relido));
    }

    @Override
    @Transactional
    public AnimalMergeRequestResponseDTO recusar(UUID animalMergeRequestId) {
        AnimalMergeRequest pedido = pendente(animalMergeRequestId);

        animalAccessGuard.requireCustodia(pedido.getSurviving().getAnimalId());

        /*
         * A RECUSA MARCA OS DOIS, e nao so arquiva o pedido.
         *
         * "Se forem diferentes, o microchip repetido fica marcado nos dois cadastros —
         * provavelmente ha um erro de digitacao em algum lugar, e alguem vai precisar saber
         * disso." O produto nao sabe qual dos dois esta errado, e adivinhar apagaria o numero
         * certo metade das vezes: quem sabe e quem tem o animal na frente e o leitor na mao.
         */
        marcarConflito(pedido.getSurviving());
        marcarConflito(pedido.getAbsorbed());

        pedido.setStatus(AnimalMergeStatus.RECUSADO);
        pedido.setDecidedBy(currentPersonProvider.require());
        pedido.setDecidedAt(LocalDateTime.now());

        return toResponse(mergeRequestRepository.save(pedido));
    }

    private void marcarConflito(Animal animal) {
        if (!animal.isMicrochipConflict()) {
            animal.setMicrochipConflict(true);
            animalRepository.save(animal);
        }
    }

    private AnimalMergeRequest pendente(UUID animalMergeRequestId) {
        AnimalMergeRequest pedido = mergeRequestRepository.findById(animalMergeRequestId)
                .orElseThrow(this::naoEncontrado);

        /* Decidir duas vezes nao e idempotencia: a segunda decisao viria de alguem que leu a
           comparacao ANTES da primeira uniao, sobre um estado que ja nao existe. */
        if (!pedido.estaPendente()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.MERGE_REQUEST_ALREADY_DECIDED.getMessage(),
                    ErrorMessageEnum.MERGE_REQUEST_ALREADY_DECIDED.getCode(),
                    HttpStatus.CONFLICT);
        }

        return pedido;
    }

    /** Cadastro absorvido e apontador: nao recebe registro novo nem entra em pedido novo. */
    private void exigirNaoAbsorvido(Animal animal) {
        if (animal.foiAbsorvido()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ANIMAL_ALREADY_MERGED.getMessage(),
                    ErrorMessageEnum.ANIMAL_ALREADY_MERGED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * "Onde os dois discordam."
     *
     * <b>So entra campo em que os DOIS disseram algo, e disseram coisas diferentes.</b> Um campo
     * que so um deles preencheu nao e divergencia — e informacao que existe de um lado so, e
     * chama-la de conflito faria quem decide procurar uma contradicao que nao ha.
     */
    private List<Divergencia> divergenciasEntre(Animal sobrevivente, Animal absorvido) {
        List<Divergencia> divergencias = new ArrayList<>();

        comparar(divergencias, "name", sobrevivente.getName(), absorvido.getName());
        comparar(divergencias, "breed", sobrevivente.getBreed(), absorvido.getBreed());
        comparar(divergencias, "gender", sobrevivente.getGender(), absorvido.getGender());
        comparar(divergencias, "color", sobrevivente.getColor(), absorvido.getColor());
        comparar(divergencias, "bornDate", texto(sobrevivente.getBornDate()), texto(absorvido.getBornDate()));
        comparar(divergencias, "generalRegistry",
                sobrevivente.getGeneralRegistry(), absorvido.getGeneralRegistry());
        comparar(divergencias, "species", texto(sobrevivente.getSpecies()), texto(absorvido.getSpecies()));

        return divergencias;
    }

    private void comparar(List<Divergencia> onde, String campo, String doSobrevivente, String doAbsorvido) {
        boolean ambosDisseram = temTexto(doSobrevivente) && temTexto(doAbsorvido);

        if (ambosDisseram && !Objects.equals(doSobrevivente.trim(), doAbsorvido.trim())) {
            onde.add(Divergencia.builder()
                    .field(campo)
                    .survivingValue(doSobrevivente)
                    .absorbedValue(doAbsorvido)
                    .build());
        }
    }

    private static boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    /**
     * As divergencias viram JSON no momento da decisao.
     *
     * A falha de serializacao nao pode derrubar a uniao — ela ja foi decidida por quem responde
     * pelo animal, e recusa-la aqui por um problema de formato transformaria uma decisao humana
     * num erro tecnico. O que se perde e o registro do valor descartado, e isso vai para o log.
     */
    private String comoJson(List<Divergencia> divergencias) {
        if (divergencias.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(divergencias);
        } catch (JsonProcessingException falha) {
            return null;
        }
    }

    private List<Divergencia> deJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Divergencia.class));
        } catch (JsonProcessingException falha) {
            return List.of();
        }
    }

    private PetfyHealthcareException naoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.MERGE_REQUEST_NOT_FOUND.getMessage(),
                ErrorMessageEnum.MERGE_REQUEST_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private AnimalMergeRequestResponseDTO toResponse(AnimalMergeRequest pedido) {
        return AnimalMergeRequestResponseDTO.builder()
                .animalMergeRequestId(pedido.getAnimalMergeRequestId())
                .status(pedido.getStatus())
                .requestedByName(pedido.getRequestedBy().getName())
                .organizationName(pedido.getOrganization() == null ? null : pedido.getOrganization().getName())
                .reason(pedido.getReason())
                .creationDate(pedido.getCreationDate())
                .decidedAt(pedido.getDecidedAt())
                .decidedByName(pedido.getDecidedBy() == null ? null : pedido.getDecidedBy().getName())
                .absorbed(ladoDe(pedido.getAbsorbed()))
                .surviving(ladoDe(pedido.getSurviving()))
                /* Pendente: calcula, porque os dois cadastros ainda existem e podem ate mudar.
                   Decidido: le o que foi guardado, porque o valor descartado so existe la. */
                .divergences(pedido.estaPendente()
                        ? divergenciasEntre(pedido.getSurviving(), pedido.getAbsorbed())
                        : deJson(pedido.getDiscardedValues()))
                .build();
    }

    private LadoDaUniao ladoDe(Animal animal) {
        TimelineRepository.Tamanho tamanho = timelineRepository.tamanhoDe(animal.getAnimalId());

        return LadoDaUniao.builder()
                .animalId(animal.getAnimalId())
                .name(animal.getName())
                .species(texto(animal.getSpecies()))
                .breed(animal.getBreed())
                .microchipNumber(animal.getMicrochipNumber())
                .holderName(quemResponde(animal.getAnimalId()).orElse(null))
                .eventCount(tamanho == null ? 0 : tamanho.getEventos())
                .firstEventAt(tamanho == null ? null : tamanho.getPrimeiro())
                .peopleCount(tamanho == null ? 0 : tamanho.getPessoas())
                .organizationCount(tamanho == null ? 0 : tamanho.getOrganizacoes())
                .build();
    }

    /** Pessoa ou organizacao: o abrigo que resgatou responde pelo animal, e nao ha tutor humano. */
    private Optional<String> quemResponde(UUID animalId) {
        return custodyRepository.findEmCurso(animalId).map(AnimalMergeServiceImpl::nomeDoResponsavel);
    }

    private static String nomeDoResponsavel(Custody custodia) {
        if (custodia.getHolderPerson() != null) {
            return custodia.getHolderPerson().getName();
        }

        return custodia.getHolderOrganization() == null ? null : custodia.getHolderOrganization().getName();
    }

}

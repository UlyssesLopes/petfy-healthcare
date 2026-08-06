package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.CareInstructionFulfillment;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.CareInstructionFulfillmentRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.CareInstructionService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CareInstructionServiceImpl implements CareInstructionService {

    private final CareInstructionRepository careInstructionRepository;
    private final CareInstructionFulfillmentRepository fulfillmentRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;

    /**
     * Emitir orientacao e escrita: manda alguem fazer algo com o animal.
     *
     * <b>Nao exige credencial profissional</b>, e isso e deliberado. Prescricao de
     * veterinario, medicacao que o tutor combinou e tema de casa da creche tem a mesma
     * forma, e so a primeira envolve CRMV - exigir credencial aqui excluiria os outros
     * dois usos, que sao dois tercos do motivo de a orientacao existir. O que separa uma
     * prescricao de um recado e a autoria, que fica gravada e visivel: quem le o
     * prontuario ve quem mandou, e por qual organizacao.
     */
    @Override
    @Transactional
    public CareInstructionResponseDTO create(UUID animalId, CareInstructionRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Person person = currentPersonProvider.require();

        recusarPrazoIncoerente(request.getStartsOn(), request.getEndsOn());

        Organization organization = organizacaoAssinante(person);

        CareInstruction instrucao = careInstructionRepository.save(CareInstruction.builder()
                .animal(animal)
                .recordedBy(person)
                .organization(organization)
                .description(request.getDescription())
                .intervalDays(request.getIntervalDays())
                .startsOn(request.getStartsOn())
                .endsOn(request.getEndsOn())
                .creationDate(LocalDateTime.now())
                .build());

        return toResponse(instrucao);
    }

    /**
     * Ler exige apenas leitura: saber que o animal toma anticonvulsivante e informacao
     * de quem acompanha, e nao privilegio de quem escreve.
     *
     * <b>A encerrada vem na lista.</b> Um tratamento suspenso pelo veterinario e
     * informacao clinica, e a proxima pessoa a ler o historico precisa saber que houve.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CareInstructionResponseDTO> listByAnimal(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        return careInstructionRepository.findByAnimalAnimalIdOrderByStartsOnDesc(animalId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Encerra antes do prazo, sem apagar.
     *
     * <b>Idempotente</b>, como a revogacao de concessao: a segunda chamada devolve o
     * estado e nao mexe na data da primeira. Quem encerrou o tratamento e quando ficam
     * como estavam - reescrever isso apagaria de quem foi a decisao clinica.
     */
    @Override
    @Transactional
    public CareInstructionResponseDTO revoke(UUID animalId, UUID careInstructionId) {
        animalAccessGuard.requireEscrita(animalId);

        CareInstruction instrucao = buscarDoAnimal(careInstructionId, animalId);

        if (instrucao.getRevokedAt() != null) {
            return toResponse(instrucao);
        }

        instrucao.setRevokedAt(LocalDateTime.now());
        instrucao.setRevokedBy(currentPersonProvider.require());

        return toResponse(careInstructionRepository.save(instrucao));
    }

    /**
     * Confirma que a orientacao foi cumprida.
     *
     * <b>Escrita, e nao leitura:</b> o cumprimento entra na linha do tempo e altera o que
     * o proximo cuidador ve como pendente. Quem so acompanha o animal nao decide que o
     * remedio foi dado.
     */
    @Override
    @Transactional
    public CareInstructionFulfillmentResponseDTO confirmFulfillment(
            UUID animalId, UUID careInstructionId, CareInstructionFulfillmentRequestDTO request) {
        animalAccessGuard.requireEscrita(animalId);

        CareInstruction instrucao = buscarDoAnimal(careInstructionId, animalId);

        LocalDateTime cumpridoEm = request.getFulfilledAt() != null
                ? request.getFulfilledAt()
                : LocalDateTime.now();

        // Vigencia no instante do FATO, e nao no de hoje: o tutor confirma as 22h de
        // segunda o remedio que deu no domingo, e o tratamento pode ter terminado no
        // sabado - ou pode ter terminado ontem, e o cumprimento de ontem ser legitimo.
        // Conferir contra hoje recusaria registro verdadeiro e aceitaria registro falso.
        if (!instrucao.estaVigenteEm(cumpridoEm.toLocalDate())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CARE_INSTRUCTION_NOT_IN_EFFECT.getMessage(),
                    ErrorMessageEnum.CARE_INSTRUCTION_NOT_IN_EFFECT.getCode(),
                    HttpStatus.CONFLICT);
        }

        CareInstructionFulfillment cumprimento = fulfillmentRepository.save(
                CareInstructionFulfillment.builder()
                        .careInstruction(instrucao)
                        .confirmedBy(currentPersonProvider.require())
                        .fulfilledAt(cumpridoEm)
                        .recordedAt(LocalDateTime.now())
                        .note(request.getNote())
                        .build());

        return toResponse(cumprimento);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareInstructionFulfillmentResponseDTO> listFulfillments(UUID animalId,
                                                                       UUID careInstructionId) {
        animalAccessGuard.requireLeitura(animalId);

        buscarDoAnimal(careInstructionId, animalId);

        return fulfillmentRepository
                .findByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(careInstructionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * A organizacao em nome de quem a orientacao esta sendo emitida, quando ha uma.
     *
     * <b>A capacidade exigida e REGISTRAR_OBSERVACAO, e nao REGISTRAR_ATO_CLINICO.</b>
     * Orientacao nao e necessariamente ato clinico: tema de casa da creche e rotina do
     * lar transitorio sao orientacoes, e exigir a capacidade clinica faria a creche nao
     * poder mandar nada - reintroduzindo pela porta de tras a exigencia de ser clinica
     * que o P3 acabou de remover. Quem prescreve remedio e reconhecido pela credencial na
     * autoria, e nao por uma capacidade que barraria os outros usos.
     */
    private Organization organizacaoAssinante(Person person) {
        Optional<Organization> declarada = currentProfessionalProvider.organizacaoDeclarada(person);

        declarada.ifPresent(organization -> {
            if (!organization.pode(OrganizationCapability.REGISTRAR_OBSERVACAO)) {
                throw new PetfyHealthcareException(
                        ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getMessage(),
                        ErrorMessageEnum.CAPABILITY_NOT_GRANTED.getCode(),
                        HttpStatus.FORBIDDEN);
            }
        });

        return declarada.orElse(null);
    }

    /**
     * Prazo que termina antes de comecar e erro de digitacao, e o banco tambem recusa por
     * CHECK. As duas nao sao redundantes: o CHECK impede um insert direto de contornar, e
     * aqui o cliente recebe 400 em vez de erro de integridade como se fosse falha nossa.
     */
    private void recusarPrazoIncoerente(LocalDate startsOn, LocalDate endsOn) {
        if (endsOn != null && endsOn.isBefore(startsOn)) {
            throw new PetfyHealthcareException(
                    "endsOn nao pode ser anterior a startsOn",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * A orientacao tem de ser do animal informado. Sem o filtro, quem tem escrita num
     * animal cumpriria a orientacao de outro so por ter o id - e a autorizacao teria sido
     * feita sobre o animal errado. 404, e nao 403: quem pergunta nao descobre que aquele
     * id existe.
     */
    private CareInstruction buscarDoAnimal(UUID careInstructionId, UUID animalId) {
        return careInstructionRepository.findById(careInstructionId)
                .filter(i -> i.getAnimal().getAnimalId().equals(animalId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CARE_INSTRUCTION_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CARE_INSTRUCTION_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private CareInstructionResponseDTO toResponse(CareInstruction i) {
        Optional<CareInstructionFulfillment> ultimo = fulfillmentRepository
                .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(
                        i.getCareInstructionId());

        return CareInstructionResponseDTO.builder()
                .careInstructionId(i.getCareInstructionId())
                .animalId(i.getAnimal().getAnimalId())
                .description(i.getDescription())
                .intervalDays(i.getIntervalDays())
                .startsOn(i.getStartsOn())
                .endsOn(i.getEndsOn())
                .revokedAt(i.getRevokedAt())
                .vigente(i.estaVigenteEm(LocalDate.now()))
                .recordedByName(i.getRecordedBy() != null ? i.getRecordedBy().getName() : null)
                .organizationName(i.getOrganization() != null ? i.getOrganization().getName() : null)
                .lastFulfilledAt(ultimo.map(CareInstructionFulfillment::getFulfilledAt).orElse(null))
                .lastFulfilledByName(ultimo.map(f -> f.getConfirmedBy().getName()).orElse(null))
                .creationDate(i.getCreationDate())
                .build();
    }

    private CareInstructionFulfillmentResponseDTO toResponse(CareInstructionFulfillment f) {
        return CareInstructionFulfillmentResponseDTO.builder()
                .careInstructionFulfillmentId(f.getCareInstructionFulfillmentId())
                .careInstructionId(f.getCareInstruction().getCareInstructionId())
                .confirmedByName(f.getConfirmedBy().getName())
                .fulfilledAt(f.getFulfilledAt())
                .recordedAt(f.getRecordedAt())
                .note(f.getNote())
                .build();
    }

}

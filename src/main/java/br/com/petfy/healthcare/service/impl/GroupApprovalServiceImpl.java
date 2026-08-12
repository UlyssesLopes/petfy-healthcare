package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.dto.GroupApprovalRequestDTO;
import br.com.petfy.healthcare.domain.dto.GroupApprovalResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.GroupApproval;
import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import br.com.petfy.healthcare.domain.entity.GroupApprovalStatus;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.GroupApprovalRepository;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalDeathService;
import br.com.petfy.healthcare.service.GroupApprovalService;
import br.com.petfy.healthcare.service.PetTutorService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * O acordo de duas pessoas (Telas 43 e 44).
 *
 * <b>"Sem dono, a proteção contra o gesto irreversível de uma pessoa só é o acordo de duas."</b>
 *
 * Num animal com tutor, o irreversível é barrado por quem responde: o {@code requireCustodia}
 * pergunta "é você?" e acabou. Na colônia essa pergunta não tem resposta útil — seis pessoas
 * respondem juntas, e todas passam no {@code requireCustodia} pela organização. Sem esta classe,
 * uma pessoa daria um gato para adoção ou encerraria uma linha do tempo sozinha.
 *
 * <b>O que NÃO passa por aqui é metade da decisão.</b> Marcar que viu, registrar ferida, levar ao
 * veterinário e lançar o que foi feito são de qualquer um. Travar qualquer um deles mataria a
 * tela, porque o valor da colônia está em o registro ser barato — a proteção existe apenas para o
 * que não se desfaz.
 */
@Service
@RequiredArgsConstructor
public class GroupApprovalServiceImpl implements GroupApprovalService {

    private final GroupApprovalRepository groupApprovalRepository;
    private final MembershipRepository membershipRepository;
    private final AnimalRepository animalRepository;
    private final PersonRepository personRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final PetTutorService petTutorService;
    private final AnimalDeathService animalDeathService;

    @Override
    @Transactional
    public GroupApprovalResponseDTO pedir(GroupApprovalRequestDTO dto) {
        Person eu = currentPersonProvider.require();
        Organization grupo = exigirGrupoDeclarado(eu);

        exigirAlvoCoerente(dto);
        exigirSemPendenteIgual(grupo, dto);

        GroupApproval pedido = groupApprovalRepository.save(GroupApproval.builder()
                .organization(grupo)
                .kind(dto.getKind())
                .animal(dto.getAnimalId() == null ? null : carregarAnimal(dto.getAnimalId()))
                .targetPerson(dto.getTargetPersonId() == null ? null : carregarPessoa(dto.getTargetPersonId()))
                .toPerson(dto.getToPersonId() == null ? null : carregarPessoa(dto.getToPersonId()))
                .reason(vazioVira(dto.getReason()))
                .deceasedOn(dto.getDeceasedOn())
                .status(GroupApprovalStatus.PENDENTE)
                .requestedBy(eu)
                .requestedAt(LocalDateTime.now())
                .build());

        return toResponse(pedido, eu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupApprovalResponseDTO> pendentes() {
        Person eu = currentPersonProvider.require();
        Organization grupo = exigirGrupoDeclarado(eu);

        return groupApprovalRepository.findPendentesDoGrupo(grupo.getOrganizationId()).stream()
                .map(pedido -> toResponse(pedido, eu))
                .toList();
    }

    /**
     * A segunda pessoa concorda, <b>e o ato acontece na mesma transação</b>.
     *
     * Guardar "concordado, falta executar" criaria um estado em que o grupo acha que deu e o
     * animal não mudou de mão — e ninguém saberia de quem é a vez de agir.
     */
    @Override
    @Transactional
    public GroupApprovalResponseDTO concordar(UUID groupApprovalId) {
        Person eu = currentPersonProvider.require();
        GroupApproval pedido = pendente(groupApprovalId);

        exigirMembroDoGrupo(eu, pedido.getOrganization());
        exigirQueNaoSejaQuemPediu(eu, pedido);

        executar(pedido);

        pedido.setStatus(GroupApprovalStatus.CONCORDADO);
        pedido.setDecidedBy(eu);
        pedido.setDecidedAt(LocalDateTime.now());

        return toResponse(groupApprovalRepository.save(pedido), eu);
    }

    /**
     * Recusar também exige ser do grupo e não ser quem pediu.
     *
     * <b>Quem pediu não desiste por aqui</b>, e isso é uma consequência aceita: para desistir, ele
     * pede a alguém que recuse, ou o pedido fica pendente. Deixar o autor recusar o próprio pedido
     * abriria o caminho de "peço, ninguém olha, eu mesmo fecho" — e a assimetria entre concordar e
     * recusar seria uma porta a mais para manter.
     */
    @Override
    @Transactional
    public GroupApprovalResponseDTO recusar(UUID groupApprovalId) {
        Person eu = currentPersonProvider.require();
        GroupApproval pedido = pendente(groupApprovalId);

        exigirMembroDoGrupo(eu, pedido.getOrganization());
        exigirQueNaoSejaQuemPediu(eu, pedido);

        pedido.setStatus(GroupApprovalStatus.RECUSADO);
        pedido.setDecidedBy(eu);
        pedido.setDecidedAt(LocalDateTime.now());

        return toResponse(groupApprovalRepository.save(pedido), eu);
    }

    /**
     * O que cada concordância faz de fato.
     *
     * <b>A adoção vira um CONVITE, e não uma transferência direta</b> — e essa é a decisão menos
     * óbvia deste arquivo. O desenho diz "Paula passa a responder por ela", mas o produto inteiro
     * exige que quem recebe um animal consinta: é assim na transferência de titularidade e na
     * adoção do abrigo. Passar a custódia sem o aceite dela colocaria um animal sob a
     * responsabilidade de alguém que ainda não disse sim — o único ponto do produto em que isso
     * aconteceria.
     *
     * <b>O óbito reusa o fluxo da Tela 33 inteiro</b>: ele encerra a custódia, encerra matrículas
     * e avisa quem cuidava. A data vem do pedido, porque é de quem viu.
     */
    private void executar(GroupApproval pedido) {
        switch (pedido.getKind()) {
            case ADOCAO -> petTutorService.invite(pedido.getAnimal().getAnimalId(),
                    PetTutorInviteRequestDTO.builder()
                            .email(pedido.getToPerson().getEmail())
                            .role(PetTutorRole.HOLDER)
                            .build());

            case OBITO -> animalDeathService.registrar(pedido.getAnimal().getAnimalId(),
                    AnimalDeathRequestDTO.builder()
                            .deceasedOn(pedido.getDeceasedOn())
                            .farewellNote(pedido.getReason())
                            .build());

            case REMOCAO_DE_MEMBRO -> desligar(pedido);
        }
    }

    private void desligar(GroupApproval pedido) {
        Membership vinculo = membershipRepository
                .findAtivoDaPessoaNaOrganizacao(pedido.getTargetPerson().getPersonId(),
                        pedido.getOrganization().getOrganizationId())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.MEMBERSHIP_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.MEMBERSHIP_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        vinculo.setLeftAt(LocalDateTime.now());
        membershipRepository.save(vinculo);
    }

    /**
     * O grupo em cujo nome a pessoa está agindo.
     *
     * <b>Exige declaração explícita, como toda ação por organização neste produto.</b> Quem cuida
     * de duas colônias precisa dizer de qual está falando — pedir a adoção na colônia errada seria
     * pior que ser recusado.
     */
    private Organization exigirGrupoDeclarado(Person eu) {
        Organization grupo = currentProfessionalProvider.organizacaoDeclarada(eu)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));

        exigirMembroDoGrupo(eu, grupo);
        return grupo;
    }

    /**
     * Só quem é do grupo pede e decide.
     *
     * <b>Não é sobre hierarquia:</b> um voluntário concorda tanto quanto a administradora. É sobre
     * pertencer — quem não cuida daqueles animais não tem como saber se a adoção faz sentido.
     */
    private void exigirMembroDoGrupo(Person pessoa, Organization grupo) {
        if (membershipRepository
                .findAtivoDaPessoaNaOrganizacao(pessoa.getPersonId(), grupo.getOrganizationId())
                .isEmpty()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NOT_A_GROUP_MEMBER.getMessage(),
                    ErrorMessageEnum.NOT_A_GROUP_MEMBER.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    /** A regra inteira, numa linha: quem pede não concorda consigo. */
    private void exigirQueNaoSejaQuemPediu(Person eu, GroupApproval pedido) {
        if (pedido.getRequestedBy().getPersonId().equals(eu.getPersonId())) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.CANNOT_APPROVE_OWN_REQUEST.getMessage(),
                    ErrorMessageEnum.CANNOT_APPROVE_OWN_REQUEST.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    /**
     * O alvo tem de existir, e tem de ser o do tipo.
     *
     * O CHECK do banco diz o mesmo, e os dois existem pela razão de sempre: o banco impede o
     * insert direto, e o serviço impede que o cliente receba erro de integridade como 500.
     */
    private void exigirAlvoCoerente(GroupApprovalRequestDTO dto) {
        boolean coerente = switch (dto.getKind()) {
            case ADOCAO -> dto.getAnimalId() != null && dto.getToPersonId() != null
                    && dto.getTargetPersonId() == null;
            case OBITO -> dto.getAnimalId() != null && dto.getDeceasedOn() != null
                    && dto.getToPersonId() == null && dto.getTargetPersonId() == null;
            case REMOCAO_DE_MEMBRO -> dto.getTargetPersonId() != null && dto.getAnimalId() == null
                    && dto.getToPersonId() == null;
        };

        if (!coerente) {
            throw new PetfyHealthcareException(
                    "o pedido nao traz o alvo que este tipo exige",
                    ErrorMessageEnum.INVALID_REQUEST.getCode(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void exigirSemPendenteIgual(Organization grupo, GroupApprovalRequestDTO dto) {
        Optional<GroupApproval> existente = dto.getAnimalId() != null
                ? groupApprovalRepository
                        .findByOrganizationOrganizationIdAndKindAndAnimalAnimalIdAndStatus(
                                grupo.getOrganizationId(), dto.getKind(), dto.getAnimalId(),
                                GroupApprovalStatus.PENDENTE)
                : groupApprovalRepository
                        .findByOrganizationOrganizationIdAndKindAndTargetPersonPersonIdAndStatus(
                                grupo.getOrganizationId(), dto.getKind(), dto.getTargetPersonId(),
                                GroupApprovalStatus.PENDENTE);

        if (existente.isPresent()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.GROUP_APPROVAL_ALREADY_PENDING.getMessage(),
                    ErrorMessageEnum.GROUP_APPROVAL_ALREADY_PENDING.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    private GroupApproval pendente(UUID groupApprovalId) {
        GroupApproval pedido = groupApprovalRepository.findById(groupApprovalId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.GROUP_APPROVAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.GROUP_APPROVAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        if (!pedido.estaPendente()) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.GROUP_APPROVAL_ALREADY_DECIDED.getMessage(),
                    ErrorMessageEnum.GROUP_APPROVAL_ALREADY_DECIDED.getCode(),
                    HttpStatus.CONFLICT);
        }

        return pedido;
    }

    private Animal carregarAnimal(UUID animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Person carregarPessoa(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PERSON_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PERSON_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private String vazioVira(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private GroupApprovalResponseDTO toResponse(GroupApproval pedido, Person eu) {
        return GroupApprovalResponseDTO.builder()
                .groupApprovalId(pedido.getGroupApprovalId())
                .kind(pedido.getKind())
                .status(pedido.getStatus())
                .animalId(pedido.getAnimal() == null ? null : pedido.getAnimal().getAnimalId())
                .animalName(pedido.getAnimal() == null ? null : pedido.getAnimal().getName())
                .targetPersonId(pedido.getTargetPerson() == null ? null : pedido.getTargetPerson().getPersonId())
                .targetPersonName(pedido.getTargetPerson() == null ? null : pedido.getTargetPerson().getName())
                .toPersonId(pedido.getToPerson() == null ? null : pedido.getToPerson().getPersonId())
                .toPersonName(pedido.getToPerson() == null ? null : pedido.getToPerson().getName())
                .reason(pedido.getReason())
                .requestedByName(pedido.getRequestedBy().getName())
                .requestedAt(pedido.getRequestedAt())
                .decidedByName(pedido.getDecidedBy() == null ? null : pedido.getDecidedBy().getName())
                .decidedAt(pedido.getDecidedAt())
                // quem pediu vê o próprio pedido na lista, e vê que não pode decidi-lo
                .canDecide(pedido.estaPendente()
                        && !pedido.getRequestedBy().getPersonId().equals(eu.getPersonId()))
                .build();
    }

}

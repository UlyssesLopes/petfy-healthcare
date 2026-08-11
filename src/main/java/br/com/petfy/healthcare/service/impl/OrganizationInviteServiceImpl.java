package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInvitePreviewResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteResponseDTO;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.OrganizationInvite;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationInviteRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.OrganizationInviteService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationInviteServiceImpl implements OrganizationInviteService {

    private final OrganizationInviteRepository organizationInviteRepository;
    private final PersonRepository personRepository;
    private final MembershipRepository membershipRepository;
    private final ProfessionalCredentialRepository professionalCredentialRepository;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final CurrentPersonProvider currentPersonProvider;
    private final OpaqueTokenService opaqueTokenService;
    private final MembershipResponseFactory membershipResponseFactory;

    @Value("${petfy.organization-invite.default-expiration-days:7}")
    private int defaultExpirationDays;

    @Override
    public OrganizationInviteResponseDTO create(OrganizationInviteRequestDTO request) {
        Person emissor = currentPersonProvider.require();

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        OrganizationInvite invite = organizationInviteRepository.save(OrganizationInvite.builder()
                .organization(organizacaoDoContextoOuFalha())
                .createdBy(emissor)
                .tokenHash(opaqueTokenService.hash(token))
                .email(request != null ? request.getEmail() : null)
                .role(request != null ? request.getRole() : null)
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .creationDate(LocalDateTime.now())
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(invite, token);
    }

    @Override
    public List<OrganizationInviteResponseDTO> listFromMyOrganization() {
        UUID organizationId = organizacaoDoContexto();

        return organizationInviteRepository.findByOrganizationOrganizationIdOrderByCreationDateDesc(organizationId)
                .stream()
                .map(invite -> toResponse(invite, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revoke(UUID organizationInviteId) {
        UUID organizationId = organizacaoDoContexto();

        OrganizationInvite invite = organizationInviteRepository.findById(organizationInviteId)
                .filter(i -> i.getOrganization().getOrganizationId().equals(organizationId))
                .orElseThrow(this::inviteInvalido);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (invite.getRevokedAt() == null) {
            invite.setRevokedAt(LocalDateTime.now());
            organizationInviteRepository.save(invite);
        }
    }

    /**
     * Token inexistente, expirado, revogado, ja usado e destinado a outro email
     * respondem igual. Distinguir diria a quem tenta adivinhar qual parte errou -
     * e o convite e o que separa um estranho dos animals de uma clinica inteira.
     */
    @Override
    public OrganizationInvite validate(String token, String email) {
        return organizationInviteRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(invite -> invite.isUsable(LocalDateTime.now()))
                .filter(invite -> aceitaEmail(invite, email))
                .orElseThrow(this::inviteInvalido);
    }

    @Override
    public void markAccepted(OrganizationInvite invite, UUID acceptedByVetId) {
        Person aceitante = personRepository.findById(acceptedByVetId)
                .orElseThrow(this::inviteInvalido);

        invite.setAcceptedAt(LocalDateTime.now());
        invite.setAcceptedBy(aceitante);

        organizationInviteRepository.save(invite);
    }

    /**
     * Ler o convite sem gastar.
     *
     * <b>Valida com o e-mail de quem esta autenticado</b>, e nao com um e-mail do request: um
     * convite enderecado a alguem nao pode ser lido por quem apenas tem o link. E como a
     * validacao e a mesma do aceite, o preview nunca mostra um convite que o aceite recusaria —
     * que seria a pior tela possivel, a que oferece um botao condenado.
     */
    @Override
    @Transactional(readOnly = true)
    public OrganizationInvitePreviewResponseDTO preview(String token) {
        OrganizationInvite invite = validate(token, currentPersonProvider.require().getEmail());

        return OrganizationInvitePreviewResponseDTO.builder()
                .organizationId(invite.getOrganization().getOrganizationId())
                .organizationName(invite.getOrganization().getName())
                .role(invite.getRole())
                .invitedByName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .build();
    }

    /**
     * O aceite de quem ja tem conta.
     *
     * <b>A ordem importa e nao e a obvia.</b> Confere o vinculo existente ANTES de consumir o
     * convite: consumir primeiro gastaria um convite de uso unico para em seguida recusar o
     * aceite, e a pessoa ficaria sem vinculo e sem convite — precisando de um novo para nada.
     *
     * <b>Quem saiu pode voltar.</b> O vinculo desligado tem {@code leftAt} e a busca de ativo nao
     * o encontra, entao o aceite cria um vinculo NOVO em vez de reabrir o antigo. E o desfecho
     * certo: reabrir devolveria a funcao antiga e a data de entrada antiga, e as duas seriam
     * mentira — quem volta volta agora, e com a funcao que o convite de agora diz.
     */
    @Override
    @Transactional
    public MembershipResponseDTO accept(String token) {
        Person eu = currentPersonProvider.require();
        OrganizationInvite invite = validate(token, eu.getEmail());
        Organization organizacao = invite.getOrganization();

        membershipRepository.findAtivoDaPessoaNaOrganizacao(eu.getPersonId(), organizacao.getOrganizationId())
                .ifPresent(vinculo -> {
                    throw new PetfyHealthcareException(
                            ErrorMessageEnum.ALREADY_ORGANIZATION_MEMBER.getMessage(),
                            ErrorMessageEnum.ALREADY_ORGANIZATION_MEMBER.getCode(),
                            HttpStatus.CONFLICT);
                });

        Membership vinculo = membershipRepository.save(Membership.builder()
                .person(eu)
                .organization(organizacao)
                .role(funcaoDoConvite(invite, eu))
                .joinedAt(LocalDateTime.now())
                .build());

        markAccepted(invite, eu.getPersonId());

        return membershipResponseFactory.toResponse(vinculo);
    }

    /**
     * A funcao vem do convite, escrita por quem ja e da organizacao — nunca do request, que seria
     * o cliente escolhendo a propria permissao.
     *
     * <b>Convite antigo, sem funcao, NAO cai em ADMINISTRADOR aqui</b> — e essa e a diferenca
     * deliberada em relacao ao cadastro. La a deducao "sem CRMV, logo ADMINISTRADOR" fala de quem
     * CRIA a propria organizacao, e faz sentido: administra quem a cadastrou. Aqui a pessoa esta
     * ENTRANDO numa organizacao que ja existe e que nao e dela, e um convite que se esqueceu de
     * dizer a funcao nao pode ser lido como "entregue a administracao da clinica a esta pessoa".
     * O piso e VOLUNTARIO, que e a funcao que pode menos; quem tem credencial entra como
     * VETERINARIO, que e a leitura honesta de um convite antigo, de quando o Petfy so tinha vet.
     * Um administrador ajusta depois, na Tela 16, e o caminho para isso ja existe.
     */
    private MembershipRole funcaoDoConvite(OrganizationInvite invite, Person pessoa) {
        if (invite.getRole() != null) {
            return invite.getRole();
        }

        return professionalCredentialRepository.findByPersonPersonId(pessoa.getPersonId()).isEmpty()
                ? MembershipRole.VOLUNTARIO
                : MembershipRole.VETERINARIO;
    }

    /** Convite sem email e aberto a quem tiver o link; com email, so aquele. */
    private boolean aceitaEmail(OrganizationInvite invite, String email) {
        return invite.getEmail() == null || invite.getEmail().equalsIgnoreCase(email);
    }

    private PetfyHealthcareException inviteInvalido() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.INVITE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.INVITE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private OrganizationInviteResponseDTO toResponse(OrganizationInvite invite, String token) {
        return OrganizationInviteResponseDTO.builder()
                .organizationInviteId(invite.getOrganizationInviteId())
                .organizationId(invite.getOrganization().getOrganizationId())
                .organizationName(invite.getOrganization().getName())
                .token(token)
                .email(invite.getEmail())
                .role(invite.getRole())
                .createdByVetName(invite.getCreatedBy().getName())
                .expiresAt(invite.getExpiresAt())
                .acceptedAt(invite.getAcceptedAt())
                .revokedAt(invite.getRevokedAt())
                .usable(invite.isUsable(LocalDateTime.now()))
                .build();
    }

    /**
     * A organizacao em nome de quem a pessoa esta agindo.
     *
     * Convidar membro e manter o cadastro sao atos <b>da organizacao</b>, nao da
     * pessoa: quem atua por si nao tem organizacao para convidar ninguem, e a resposta
     * certa e dizer isso, nao um NullPointerException onde antes havia um campo sempre
     * preenchido.
     *
     * <b>`organizacaoDeclarada`, e nao `requireContext` — a mesma correcao que a creche cobrou.</b>
     * O `requireContext` exige credencial profissional ativa, e a administradora de um abrigo nao
     * tem CRMV nem deveria ter. Enquanto foi assim, quem monta a equipe da organizacao que a Tela
     * 16 serve nao conseguia convidar ninguem: recebia "an active professional credential is
     * required" para um ato que nao tem nada a ver com credencial. O que autoriza aqui e o
     * VINCULO, e o `organizacaoDeclarada` ja o confere.
     */
    private Organization organizacaoDoContextoOuFalha() {
        return currentProfessionalProvider.organizacaoDeclarada(currentPersonProvider.require())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));
    }

    private java.util.UUID organizacaoDoContexto() {
        return organizacaoDoContextoOuFalha().getOrganizationId();
    }

}

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationResponseDTO;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.OrganizationService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;

    private final MembershipRepository membershipRepository;

    private final CurrentPersonProvider currentPersonProvider;

    private final CurrentProfessionalProvider currentProfessionalProvider;

    /**
     * Cria a organizacao <b>e o vinculo de quem a criou</b>.
     *
     * <b>O vinculo faltava, e sem ele a organizacao nascia inalcancavel.</b> Ate 2026-08-15 este
     * metodo salvava a {@link Organization} e mais nada: nenhum {@link Membership}. Como o
     * contexto ativo lista organizacoes por {@code findAtivosDaPessoa}, a organizacao recem-criada
     * nao aparecia no "Agindo como" de ninguem — nem de quem acabou de cria-la. O produto levava
     * a pessoa direto para a equipe da organizacao, e a tela abria com "escolha em nome de qual
     * esta agindo" sobre uma lista que nao continha a organizacao. Nao havia conserto pela
     * interface: a unica saida seria um INSERT na mao.
     *
     * A tela sempre prometeu "voce fica como responsavel". Agora o servidor cumpre.
     *
     * <b>ADMINISTRADOR, e nao VETERINARIO</b>, pelo mesmo criterio do cadastro de pessoa em
     * {@code PersonServiceImpl.registrarVinculo}: quem chega sem convite e sem credencial declarada
     * e o dono do negocio, nao o clinico. Quem tem CRMV continua declarando na conta, e as duas
     * coisas sao independentes — a organizacao nao verifica o registro de ninguem.
     *
     * <b>{@link CurrentPersonProvider}, e NAO o {@code CurrentProfessionalProvider}.</b> O
     * {@code require()} do segundo exige credencial profissional ativa e responde 403 sem ela:
     * usa-lo aqui faria criar uma organizacao virar privilegio de veterinario, e "criar clinica
     * nao deve exigir ser veterinario — o tutor precisa registrar onde vacinou" e regra escrita
     * neste servico desde sempre, com teste proprio. Quem cria pode ser o dono da creche.
     *
     * <b>Transacional porque sao duas escritas.</b> Uma organizacao salva sem o vinculo e
     * exatamente o defeito que este metodo acabou de deixar de ter, e o meio do caminho nao pode
     * sobreviver a uma falha da segunda escrita.
     */
    @Override
    @Transactional
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO request) {
        Organization organization = Organization.builder()
                .name(request.getName())
                .ownerVetName(request.getOwnerVetName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .cnpj(request.getCnpj())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .cep(request.getCep())
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .build();

        Organization salva = organizationRepository.save(organization);

        membershipRepository.save(Membership.builder()
                .person(currentPersonProvider.require())
                .organization(salva)
                .role(MembershipRole.ADMINISTRADOR)
                .joinedAt(LocalDateTime.now())
                .build());

        return toResponse(salva);
    }

    @Override
    public OrganizationResponseDTO getOrganizationById(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .map(this::toResponse)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<OrganizationResponseDTO> listAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public OrganizationResponseDTO updateOrganization(UUID organizationId, OrganizationRequestDTO request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND));

        exigirVetDaClinica(organizationId);

        organization.setName(request.getName() != null ? request.getName() : organization.getName());
        organization.setOwnerVetName(request.getOwnerVetName() != null ? request.getOwnerVetName() : organization.getOwnerVetName());
        organization.setPhone(request.getPhone() != null ? request.getPhone() : organization.getPhone());
        organization.setEmail(request.getEmail() != null ? request.getEmail() : organization.getEmail());
        organization.setCnpj(request.getCnpj() != null ? request.getCnpj() : organization.getCnpj());
        organization.setAddress(request.getAddress() != null ? request.getAddress() : organization.getAddress());
        organization.setCity(request.getCity() != null ? request.getCity() : organization.getCity());
        organization.setState(request.getState() != null ? request.getState() : organization.getState());
        organization.setCep(request.getCep() != null ? request.getCep() : organization.getCep());
        organization.setDescription(request.getDescription() != null ? request.getDescription() : organization.getDescription());
        organization.setUpdateDate(LocalDateTime.now());

        return toResponse(organizationRepository.save(organization));
    }

    @Override
    public void deleteOrganization(UUID organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new PetfyHealthcareException(ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(), ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
        }

        exigirVetDaClinica(organizationId);

        organizationRepository.deleteById(organizationId);
    }

    /**
     * Leitura e criacao seguem abertas a qualquer autenticado - o tutor precisa
     * consultar clinicas e registrar onde vacinou. Alterar e remover, nao: quem
     * mantem o cadastro de uma clinica e quem trabalha nela.
     *
     * Responde 403 e nao 404 de proposito. Aqui, diferente dos recursos do
     * tutor, a existencia da clinica nao e segredo: ela ja aparece na listagem
     * publica, entao esconder o motivo so confundiria.
     */
    private void exigirVetDaClinica(UUID organizationId) {
        UUID clinicaDoVet = organizacaoDoContexto();

        if (!clinicaDoVet.equals(organizationId)) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getMessage(),
                    ErrorMessageEnum.NOT_CLINIC_MEMBER.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    private OrganizationResponseDTO toResponse(Organization organization) {
        return OrganizationResponseDTO.builder()
                .organizationId(organization.getOrganizationId())
                .name(organization.getName())
                .ownerVetName(organization.getOwnerVetName())
                .phone(organization.getPhone())
                .email(organization.getEmail())
                .cnpj(organization.getCnpj())
                .address(organization.getAddress())
                .city(organization.getCity())
                .state(organization.getState())
                .cep(organization.getCep())
                .description(organization.getDescription())
                .creationDate(organization.getCreationDate())
                .updateDate(organization.getUpdateDate())
                .build();
    }

    /**
     * A organizacao em nome de quem a pessoa esta agindo.
     *
     * Convidar membro e manter o cadastro sao atos <b>da organizacao</b>, nao da
     * pessoa: quem atua por si nao tem organizacao para convidar ninguem, e a resposta
     * certa e dizer isso, nao um NullPointerException onde antes havia um campo sempre
     * preenchido.
     */
    private Organization organizacaoDoContextoOuFalha() {
        return currentProfessionalProvider.requireContext().organizacao()
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));
    }

    private java.util.UUID organizacaoDoContexto() {
        return organizacaoDoContextoOuFalha().getOrganizationId();
    }

}

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.MembershipResponseDTO;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.OrganizationMemberService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A equipe da organizacao em que a pessoa esta agindo.
 *
 * <b>Autoriza por VINCULO, e nao por credencial</b> — a mesma licao que os doze casos da creche
 * cobraram: `requireContext` exige CRMV, e a administradora de um abrigo nao tem nem deveria ter.
 * Quem responde por equipe e quem e da equipe.
 *
 * <b>Ler e escrever pedem coisas diferentes, de proposito.</b> Qualquer membro ve com quem
 * trabalha — esconder a lista da propria equipe nao protege ninguem e faz o produto parecer
 * quebrado. Ajustar funcao e desligar sao atos de administracao, e so o ADMINISTRADOR faz.
 *
 * <b>A classe e {@code @Transactional} nas escritas</b> pela razao que ja custou cinco 500 neste
 * projeto: o DTO da resposta le `person.getName()` de entidade que veio do repositorio, e montar
 * DTO a partir de entidade fora de transacao estoura o proxy.
 */
@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private final MembershipRepository membershipRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final MembershipResponseFactory membershipResponseFactory;

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponseDTO> listMembers() {
        Organization organizacao = organizacaoDoContexto();

        return membershipRepository.findAtivosDaOrganizacao(organizacao.getOrganizationId())
                .stream()
                .sorted(Comparator.comparing(Membership::getJoinedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    /**
     * O "ajustar" do desenho.
     *
     * <b>Rebaixar o ultimo administrador e recusado</b>, e nao e zelo excessivo: sem
     * administrador ninguem convida, ajusta funcao nem desliga — a organizacao vira um cadastro
     * que so o suporte destrava. A recusa vale inclusive quando a pessoa rebaixa a si mesma, que
     * e o caminho mais provavel de chegar la sem perceber.
     */
    @Override
    @Transactional
    public MembershipResponseDTO changeMemberRole(UUID membershipId, MembershipRole role) {
        Organization organizacao = organizacaoDoContexto();
        exigirAdministrador(organizacao);

        Membership alvo = vinculoDaOrganizacao(membershipId, organizacao);

        if (alvo.getRole() == MembershipRole.ADMINISTRADOR
                && role != MembershipRole.ADMINISTRADOR
                && ehOUltimoAdministrador(organizacao, alvo)) {
            throw ultimoAdministrador();
        }

        alvo.setRole(role);

        return toResponse(membershipRepository.save(alvo));
    }

    /**
     * O desligamento.
     *
     * <b>Marca a saida, e nao apaga o vinculo.</b> O que a pessoa registrou continua no historico
     * dos animais, e apagar o vinculo deixaria esses registros orfaos de contexto — "quem era
     * essa pessoa e por que ela podia registrar isso" e uma pergunta que o historico precisa
     * responder anos depois.
     *
     * Idempotente: desligar quem ja saiu nao e erro, e a primeira data e que vale.
     */
    @Override
    @Transactional
    public void removeMember(UUID membershipId) {
        Organization organizacao = organizacaoDoContexto();
        exigirAdministrador(organizacao);

        Membership alvo = vinculoDaOrganizacao(membershipId, organizacao);

        if (alvo.getLeftAt() != null) {
            return;
        }

        if (alvo.getRole() == MembershipRole.ADMINISTRADOR && ehOUltimoAdministrador(organizacao, alvo)) {
            throw ultimoAdministrador();
        }

        alvo.setLeftAt(LocalDateTime.now());
        membershipRepository.save(alvo);
    }

    /**
     * A organizacao em nome de quem a pessoa age. O `organizacaoDeclarada` ja confere o vinculo —
     * e o vinculo e o que autoriza.
     */
    private Organization organizacaoDoContexto() {
        return currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getMessage(),
                        ErrorMessageEnum.ORGANIZATION_CONTEXT_REQUIRED.getCode(),
                        HttpStatus.CONFLICT));
    }

    private void exigirAdministrador(Organization organizacao) {
        Person eu = currentPersonProvider.require();

        boolean administra = membershipRepository
                .findAtivoDaPessoaNaOrganizacao(eu.getPersonId(), organizacao.getOrganizationId())
                .map(vinculo -> vinculo.getRole() == MembershipRole.ADMINISTRADOR)
                .orElse(false);

        if (!administra) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.ADMINISTRATOR_ROLE_REQUIRED.getMessage(),
                    ErrorMessageEnum.ADMINISTRATOR_ROLE_REQUIRED.getCode(),
                    HttpStatus.FORBIDDEN);
        }
    }

    /** 404 para vinculo de outra organizacao: 403 confirmaria que aquele id existe. */
    private Membership vinculoDaOrganizacao(UUID membershipId, Organization organizacao) {
        return membershipRepository.findById(membershipId)
                .filter(vinculo -> vinculo.getOrganization().getOrganizationId()
                        .equals(organizacao.getOrganizationId()))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.MEMBERSHIP_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.MEMBERSHIP_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    /** Nao ha outro administrador ativo alem do alvo. */
    private boolean ehOUltimoAdministrador(Organization organizacao, Membership alvo) {
        return membershipRepository.findAtivosDaOrganizacao(organizacao.getOrganizationId())
                .stream()
                .filter(vinculo -> vinculo.getRole() == MembershipRole.ADMINISTRADOR)
                .noneMatch(vinculo -> !vinculo.getMembershipId().equals(alvo.getMembershipId()));
    }

    /* O `noneMatch` acima le "nenhum administrador que nao seja o alvo", que e a forma direta de
     * "o alvo e o unico". Contar e comparar com 1 daria o mesmo numero e uma pergunta a mais. */

    private PetfyHealthcareException ultimoAdministrador() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.LAST_ADMINISTRATOR.getMessage(),
                ErrorMessageEnum.LAST_ADMINISTRATOR.getCode(),
                HttpStatus.CONFLICT);
    }

    private MembershipResponseDTO toResponse(Membership vinculo) {
        return membershipResponseFactory.toResponse(vinculo);
    }

}

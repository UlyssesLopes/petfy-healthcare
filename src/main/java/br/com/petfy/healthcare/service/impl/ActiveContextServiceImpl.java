package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.ActiveContextResponseDTO;
import br.com.petfy.healthcare.domain.dto.ContextKind;
import br.com.petfy.healthcare.domain.dto.ContextOptionDTO;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ActiveContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * <b>Espelha a regra do {@link CurrentProfessionalProvider#requireContext()} de
 * proposito, em vez de inventar outra.</b> O valor desta leitura e dizer a verdade
 * sobre o que aconteceria na proxima escrita; se as duas divergirem, o cliente
 * desenha uma tela que mente.
 *
 * A diferenca esta so no desfecho ambiguo: la ele e 409, aqui e um campo. O erro nao
 * cabe no meio de uma operacao, e a pergunta cabe na entrada (PRODUTO 9.5).
 *
 * <b>Usa o CurrentPersonProvider, e nao o CurrentProfessionalProvider.require().</b>
 * Aquele exige credencial profissional ativa, e o monitor da creche e membro sem
 * CRMV - exigir credencial aqui esconderia dele os proprios contextos.
 */
@Service
@RequiredArgsConstructor
public class ActiveContextServiceImpl implements ActiveContextService {

    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final MembershipRepository membershipRepository;
    private final ProfessionalCredentialRepository credentialRepository;

    @Override
    public ActiveContextResponseDTO contextoAtivo(String organizacaoDeclarada) {
        Person person = currentPersonProvider.require();
        List<Membership> vinculos = membershipRepository.findAtivosDaPessoa(person.getPersonId());

        // resolve o header primeiro: se o cliente declarou, a resposta e essa, e um
        // vinculo inexistente vira 403 aqui e nao mais adiante
        ContextOptionDTO declarado = currentProfessionalProvider
                .organizacaoDeclarada(person, organizacaoDeclarada)
                .map(organizacao -> porOrganizacao(organizacao, papelNa(vinculos, organizacao)))
                .orElse(null);

        boolean ambiguo = declarado == null && vinculos.size() > 1;
        ContextOptionDTO ativo = declarado != null ? declarado : semDeclaracao(vinculos, ambiguo);

        return ActiveContextResponseDTO.builder()
                .personId(person.getPersonId())
                .personName(person.getName())
                .professional(credentialRepository.existsAtivaPorEmail(person.getEmail(), CredentialStatus.SUSPENSO))
                // O mesmo predicado que decide se a pessoa recebe notificacao, e nao uma segunda
                // leitura do instante: se um dia a regra de "pode receber" mudar, a faixa muda com
                // ela em vez de continuar dizendo o que era verdade antes.
                .emailVerified(person.podeReceberNotificacao())
                .active(ativo)
                .ambiguous(ambiguo)
                .available(disponiveis(vinculos))
                .build();
    }

    /** O que valeria numa requisicao sem header, e nulo quando nem isso vale. */
    private ContextOptionDTO semDeclaracao(List<Membership> vinculos, boolean ambiguo) {
        if (ambiguo) {
            return null;
        }

        return vinculos.isEmpty() ? pessoa() : porVinculo(vinculos.get(0));
    }

    /**
     * O contexto de pessoa entra so quando nao ha vinculo, porque e so ai que ele e
     * alcancavel: o autonomo e "pessoa com credencial e sem vinculo" (9.3), e quem tem
     * vinculo nao tem como declarar "quero atuar por mim" - a ausencia do header
     * significa "decida por mim", nao "nenhuma organizacao".
     */
    private List<ContextOptionDTO> disponiveis(List<Membership> vinculos) {
        return vinculos.isEmpty()
                ? List.of(pessoa())
                : vinculos.stream().map(this::porVinculo).toList();
    }

    private ContextOptionDTO pessoa() {
        return ContextOptionDTO.builder()
                .kind(ContextKind.PESSOA)
                .capabilities(Set.of())
                .build();
    }

    private ContextOptionDTO porVinculo(Membership vinculo) {
        return porOrganizacao(vinculo.getOrganization(), vinculo.getRole());
    }

    private ContextOptionDTO porOrganizacao(Organization organizacao,
                                            br.com.petfy.healthcare.domain.entity.MembershipRole papel) {
        return ContextOptionDTO.builder()
                .kind(ContextKind.ORGANIZACAO)
                .organizationId(organizacao.getOrganizationId())
                .organizationName(organizacao.getName())
                .role(papel)
                .capabilities(organizacao.getCapabilities() == null ? Set.of() : organizacao.getCapabilities())
                .build();
    }

    /**
     * O papel na organizacao declarada.
     *
     * Vem da lista que ja foi carregada em vez de uma segunda consulta - o provider ja
     * confirmou o vinculo, entao ele esta aqui. Nulo seria um vinculo que existe para
     * o provider e nao para esta lista, o que nao acontece: as duas consultas filtram
     * vinculo ativo da mesma pessoa.
     */
    private br.com.petfy.healthcare.domain.entity.MembershipRole papelNa(List<Membership> vinculos,
                                                                        Organization organizacao) {
        return vinculos.stream()
                .filter(v -> v.getOrganization().getOrganizationId().equals(organizacao.getOrganizationId()))
                .map(Membership::getRole)
                .findFirst()
                .orElse(null);
    }

}

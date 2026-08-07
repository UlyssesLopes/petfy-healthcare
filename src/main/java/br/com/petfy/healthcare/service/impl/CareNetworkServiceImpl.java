package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.CareNetworkMemberDTO;
import br.com.petfy.healthcare.domain.dto.CareNetworkReach;
import br.com.petfy.healthcare.domain.dto.ContextKind;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.CareNetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareNetworkServiceImpl implements CareNetworkService {

    private final CustodyRepository custodyRepository;
    private final GrantRepository grantRepository;
    private final TimelineRepository timelineRepository;
    private final AnimalAccessGuard animalAccessGuard;

    /**
     * <b>Exige apenas leitura.</b> Saber quem mais cuida do animal nao e privilegio de quem
     * responde por ele: a creche precisa saber que existe um veterinario, e o co-tutor
     * precisa ver quem mais tem alcance. Conceder acesso, isso sim, continua exigindo mais -
     * e continua nas rotas proprias.
     *
     * <b>Quatro consultas, e nenhuma por membro.</b> Custodia, concessoes, e duas agregacoes
     * de ultima contribuicao. Resolver a contribuicao membro a membro seria o mesmo N+1 que
     * a V29 tirou da linha do tempo.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CareNetworkMemberDTO> doAnimal(UUID animalId) {
        animalAccessGuard.requireLeitura(animalId);

        Map<UUID, LocalDateTime> porPessoa = ultimaContribuicao(
                timelineRepository.ultimaContribuicaoPorPessoa(animalId));
        Map<UUID, LocalDateTime> porOrganizacao = ultimaContribuicao(
                timelineRepository.ultimaContribuicaoPorOrganizacao(animalId));

        List<CareNetworkMemberDTO> rede = new ArrayList<>();

        // quem responde vem primeiro, sempre - era o que a ordenacao por papel fazia antes
        // de o papel deixar de existir na Fase 6
        custodyRepository.findEmCurso(animalId)
                .map(custodia -> deCustodia(custodia, porPessoa, porOrganizacao))
                .ifPresent(rede::add);

        // Uma consulta para as duas pontas da concessao, e o filtro separa.
        //
        // Concessao sem pessoa e sem organizacao e link de compartilhamento, e link fica
        // FORA: e alcance anonimo, nao alguem que cuida, e o token dele e a credencial.
        grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animalId)
                .stream()
                .filter(this::vigente)
                .filter(g -> g.getGranteePerson() != null || g.getGranteeOrganization() != null)
                .map(g -> deConcessao(g, porPessoa, porOrganizacao))
                .forEach(rede::add);

        return rede;
    }

    private Map<UUID, LocalDateTime> ultimaContribuicao(List<TimelineRepository.UltimaContribuicao> linhas) {
        return linhas.stream().collect(Collectors.toMap(
                TimelineRepository.UltimaContribuicao::getPessoaId,
                TimelineRepository.UltimaContribuicao::getEm));
    }

    /**
     * Vigencia conferida aqui, e nao no banco, porque a mesma consulta serve as duas pontas.
     *
     * Concessao revogada ou vencida <b>nao aparece na rede</b>: a pergunta que a 5.4 faz e
     * quem alcanca o animal <i>agora</i>, e mostrar acesso vencido como se fosse ativo e o
     * pior erro possivel numa tela cujo assunto e quem pode ver o que.
     */
    private boolean vigente(Grant concessao) {
        return concessao.getRevokedAt() == null
                && (concessao.getExpiresAt() == null || concessao.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private CareNetworkMemberDTO deCustodia(Custody custodia,
                                            Map<UUID, LocalDateTime> porPessoa,
                                            Map<UUID, LocalDateTime> porOrganizacao) {
        boolean dePessoa = custodia.getHolderPerson() != null;

        // A custodia de organizacao entra, e e por isso que esta rota existe alem da de
        // tutores: aquela filtra holderPerson, entao um animal sob custodia de uma ONG
        // aparecia sem ninguem respondendo por ele.
        UUID id = dePessoa
                ? custodia.getHolderPerson().getPersonId()
                : custodia.getHolderOrganization().getOrganizationId();

        return CareNetworkMemberDTO.builder()
                .kind(dePessoa ? ContextKind.PESSOA : ContextKind.ORGANIZACAO)
                .personId(dePessoa ? id : null)
                .organizationId(dePessoa ? null : id)
                .name(dePessoa
                        ? custodia.getHolderPerson().getName()
                        : custodia.getHolderOrganization().getName())
                .reach(CareNetworkReach.CUSTODIA)
                .holder(true)
                // vazio, e nao um conjunto inventado: quem responde alcanca tudo, e listar
                // escopos sugeriria um limite que a custodia nao tem
                .scopes(Set.of())
                .since(custodia.getStartedAt())
                .lastContributionAt((dePessoa ? porPessoa : porOrganizacao).get(id))
                .build();
    }

    private CareNetworkMemberDTO deConcessao(Grant concessao,
                                             Map<UUID, LocalDateTime> porPessoa,
                                             Map<UUID, LocalDateTime> porOrganizacao) {
        boolean dePessoa = concessao.getGranteePerson() != null;

        UUID id = dePessoa
                ? concessao.getGranteePerson().getPersonId()
                : concessao.getGranteeOrganization().getOrganizationId();

        return CareNetworkMemberDTO.builder()
                .kind(dePessoa ? ContextKind.PESSOA : ContextKind.ORGANIZACAO)
                .personId(dePessoa ? id : null)
                .organizationId(dePessoa ? null : id)
                .name(dePessoa
                        ? concessao.getGranteePerson().getName()
                        : concessao.getGranteeOrganization().getName())
                .reach(CareNetworkReach.CONCESSAO)
                .holder(false)
                .scopes(concessao.getScopes() == null ? Set.of() : concessao.getScopes())
                .since(concessao.getGrantedAt())
                .expiresAt(concessao.getExpiresAt())
                .lastContributionAt((dePessoa ? porPessoa : porOrganizacao).get(id))
                .grantedByName(concessao.getGrantedBy() == null ? null : concessao.getGrantedBy().getName())
                .build();
    }

}

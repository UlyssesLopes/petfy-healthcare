package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.OrganizationAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationAccessResponseDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.OrganizationAccessService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O tutor concede, lista e revoga o acesso de uma clinica.
 *
 * Substituiu o PetOrganizationAccessServiceImpl: a tabela propria virou um Grant com
 * beneficiario do tipo clinica. O comportamento externo continua o mesmo, com uma
 * adicao - o tutor escolhe o escopo.
 */
@Service
@RequiredArgsConstructor
public class OrganizationAccessServiceImpl implements OrganizationAccessService {

    /**
     * O que uma clinica recebe quando o tutor nao diz nada.
     *
     * E o escopo clinico inteiro, e nao o minimo, porque e o que a concessao a uma
     * clinica sempre significou - trocar o default por algo menor sem o tutor pedir
     * quebraria o fluxo do veterinario em silencio. Quem quiser menos, diz.
     */
    private static final Set<GrantScope> ESCOPO_CLINICO = Set.of(
            GrantScope.CARTEIRA, GrantScope.CONDICOES, GrantScope.PRONTUARIO,
            GrantScope.PESO, GrantScope.ANEXOS, GrantScope.CONTATO);

    private final GrantRepository grantRepository;
    private final OrganizationRepository organizationRepository;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;

    @Override
    public OrganizationAccessResponseDTO grant(UUID animalId, OrganizationAccessRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);
        Organization organization = buscarClinica(request.getOrganizationId());

        LocalDateTime agora = LocalDateTime.now();

        // reconceder reativa a concessao vigente em vez de acumular linhas: o tutor
        // que revogou e mudou de ideia espera voltar a ter acesso liberado, nao
        // ganhar uma segunda concessao ao lado da primeira
        Grant grant = grantRepository
                .findVigenteDaClinicaNoAnimal(animalId, request.getOrganizationId(), agora)
                .orElseGet(() -> Grant.builder()
                        .animal(animal)
                        .granteeOrganization(organization)
                        .level(GrantLevel.EDITOR)
                        .grantedBy(currentPersonProvider.require())
                        .build());

        grant.setRevokedAt(null);
        grant.setGrantedAt(agora);
        grant.setExpiresAt(request.getExpiresAt());
        grant.setScopes(escopoPedido(request));

        return toResponse(grantRepository.save(grant));
    }

    @Override
    public List<OrganizationAccessResponseDTO> list(UUID animalId) {
        animalAccessGuard.requireEscrita(animalId);

        return grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animalId)
                .stream()
                .filter(g -> g.getGranteeOrganization() != null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void revoke(UUID animalId, UUID organizationId) {
        animalAccessGuard.requireEscrita(animalId);

        // busca sem filtro de vigencia de proposito: revogar duas vezes nao e erro, e
        // com a consulta de vigentes a segunda chamada responderia 404 em vez de nao
        // fazer nada
        Grant grant = grantRepository
                .findFirstByAnimalAnimalIdAndGranteeOrganizationOrganizationIdOrderByGrantedAtDesc(animalId, organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_ACCESS_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_ACCESS_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));

        // a primeira data e que vale
        if (grant.getRevokedAt() == null) {
            grant.setRevokedAt(LocalDateTime.now());
            grantRepository.save(grant);
        }
    }

    /**
     * Escopo vazio nao e "sem restricao": e um acesso que nao alcanca nada, e
     * gravar assim seria negar em silencio. Sem pedido explicito, vale o default.
     */
    private Set<GrantScope> escopoPedido(OrganizationAccessRequestDTO request) {
        if (request.getScopes() == null || request.getScopes().isEmpty()) {
            return new LinkedHashSet<>(ESCOPO_CLINICO);
        }

        return new LinkedHashSet<>(request.getScopes());
    }

    private Organization buscarClinica(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.CLINIC_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private OrganizationAccessResponseDTO toResponse(Grant grant) {
        return OrganizationAccessResponseDTO.builder()
                .grantId(grant.getGrantId())
                .animalId(grant.getAnimal().getAnimalId())
                .organizationId(grant.getGranteeOrganization().getOrganizationId())
                .organizationName(grant.getGranteeOrganization().getName())
                .scopes(grant.getScopes())
                .grantedAt(grant.getGrantedAt())
                .expiresAt(grant.getExpiresAt())
                .revokedAt(grant.getRevokedAt())
                .active(grant.estaVigente(LocalDateTime.now()))
                .build();
    }

}

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalShareResponseDTO;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.OpaqueTokenService;
import br.com.petfy.healthcare.service.AnimalContacts;
import br.com.petfy.healthcare.service.AnimalShareService;
import br.com.petfy.healthcare.service.SensitiveAccessLogger;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O link de carteira, agora como um Grant com token e escopo.
 *
 * O que mudou em relacao ao AnimalShare que ele substitui: o tutor escolhe o que o
 * link mostra. Sem escolha explicita continua sendo so a carteira, que e o que ele
 * sempre mostrou.
 */
@Service
@RequiredArgsConstructor
public class AnimalShareServiceImpl implements AnimalShareService {

    private final GrantRepository grantRepository;
    private final VaccineRepository vaccineRepository;
    private final AnimalHealthConditionRepository conditionRepository;
    private final CareInstructionRepository careInstructionRepository;
    private final AnimalContacts animalContacts;
    private final AnimalAccessGuard animalAccessGuard;
    private final CurrentPersonProvider currentPersonProvider;
    private final VaccineStatusCalculator vaccineStatusCalculator;
    private final OpaqueTokenService opaqueTokenService;
    private final SensitiveAccessLogger sensitiveAccessLogger;

    @Value("${petfy.share.default-expiration-days:30}")
    private int defaultExpirationDays;

    /** Mesma janela da agenda, para o link e o app nao discordarem sobre o que e "vencendo". */
    @Value("${petfy.reminders.window-days:30}")
    private int windowDays;

    @Override
    public AnimalShareResponseDTO createShare(UUID animalId, AnimalShareRequestDTO request) {
        Animal animal = animalAccessGuard.requireEscrita(animalId);

        int validade = request != null && request.getExpiresInDays() != null
                ? request.getExpiresInDays()
                : defaultExpirationDays;

        String token = opaqueTokenService.generate();

        Grant grant = grantRepository.save(Grant.builder()
                .animal(animal)
                .tokenHash(opaqueTokenService.hash(token))
                .level(GrantLevel.VIEWER)
                .scopes(escopoPedido(request))
                .grantedBy(currentPersonProvider.require())
                .grantedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(validade))
                .build());

        // unico momento em que o token existe fora do cliente
        return toResponse(grant, token);
    }

    @Override
    public List<AnimalShareResponseDTO> listShares(UUID animalId) {
        animalAccessGuard.requireEscrita(animalId);

        return grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(animalId)
                .stream()
                .filter(g -> g.getTokenHash() != null)
                .map(g -> toResponse(g, null))
                .collect(Collectors.toList());
    }

    @Override
    public void revokeShare(UUID grantId) {
        // basta alcancar o animal: quem cuida do animal pode cortar um link que corre
        // por fora, sem depender de quem o criou
        Grant grant = grantRepository.findById(grantId)
                .filter(g -> g.getTokenHash() != null)
                .filter(g -> animalAccessGuard.alcanca(g.getAnimal().getAnimalId()))
                .orElseThrow(this::linkNaoEncontrado);

        // revogar duas vezes nao e erro, mas a primeira data e que vale
        if (grant.getRevokedAt() == null) {
            grant.setRevokedAt(LocalDateTime.now());
            grantRepository.save(grant);
        }
    }

    /**
     * <b>Transacional porque a sessao precisa estar aberta quando os escopos forem lidos.</b>
     *
     * O {@code Grant.scopes} e uma colecao lazy, e todo bloco deste cartao passa por ela — sem
     * transacao, a primeira pergunta "este link alcanca a carteira?" estoura com
     * LazyInitializationException e a rota inteira responde 500. Nao e leitura pura: abrir o
     * cartao grava uma linha no log de acesso do animal.
     */
    @Override
    @Transactional
    public SharedVaccineCardDTO viewSharedCard(String token) {
        LocalDateTime agora = LocalDateTime.now();

        // token invalido, revogado e expirado respondem igual: distinguir diria a
        // quem tem um link velho que aquele animal existe
        Grant grant = grantRepository.findByTokenHash(opaqueTokenService.hash(token))
                .filter(g -> g.estaVigente(agora))
                .orElseThrow(this::linkNaoEncontrado);

        Animal animal = grant.getAnimal();
        LocalDate hoje = LocalDate.now();

        // Registra depois de o link ser validado, e nao antes: token invalido nao e
        // acesso ao animal, e logar tentativa de token inexistente enche a tabela de um
        // animal que ninguem conseguiu abrir - ou, pior, de um animalId que nao existe.
        sensitiveAccessLogger.linkPublicoAberto(animal);

        // Cada bloco so entra se o escopo permitir. E o mesmo link servindo de carteira
        // para a creche e de cartao de emergencia para quem socorre, sem que um deles
        // precise entregar mais do que o tutor autorizou.
        List<SharedVaccineCardDTO.SharedVaccineDTO> vacinas = grant.alcanca(GrantScope.CARTEIRA)
                ? vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(animal.getAnimalId())
                        .stream()
                        .map(vaccine -> toSharedVaccine(vaccine, hoje))
                        .collect(Collectors.toList())
                : List.of();

        List<SharedVaccineCardDTO.SharedConditionDTO> condicoes = grant.alcanca(GrantScope.CONDICOES)
                ? conditionRepository.findByAnimalOrdenadasPorRelevancia(animal.getAnimalId())
                        .stream()
                        .map(c -> SharedVaccineCardDTO.SharedConditionDTO.builder()
                                .kind(c.getKind())
                                .description(c.getDescription())
                                .severity(c.getSeverity())
                                .resolvedAt(c.getResolvedAt())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        // A medicacao acompanha a condicao: quem concede "o que ele tem" concede junto "o
        // que ele esta tomando por causa disso". So o que vale hoje - orientacao encerrada
        // faria quem socorre agir sobre um remedio que o animal ja nao toma.
        List<String> emCurso = grant.alcanca(GrantScope.CONDICOES)
                ? careInstructionRepository.findVigentesNosAnimais(List.of(animal.getAnimalId()), hoje)
                        .stream()
                        .map(CareInstruction::getDescription)
                        .toList()
                : List.of();

        List<SharedVaccineCardDTO.SharedContactDTO> contatos = grant.alcanca(GrantScope.CONTATO)
                ? animalContacts.de(animal).stream()
                        .map(contato -> SharedVaccineCardDTO.SharedContactDTO.builder()
                                .name(contato.name())
                                .phone(contato.phone())
                                .kind(contato.kind())
                                .build())
                        .toList()
                : List.of();

        return SharedVaccineCardDTO.builder()
                .animalName(animal.getName())
                .animalType(animal.getType())
                .animalBreed(animal.getBreed())
                .animalBornDate(animal.getBornDate())
                .animalGender(animal.getGender())
                .personName(animal.getHolder().map(Person::getName).orElse(null))
                .contacts(contatos)
                .scopes(grant.getScopes())
                .referenceDate(hoje)
                .expiresAt(grant.getExpiresAt())
                .vaccines(vacinas)
                .conditions(condicoes)
                .ongoingCare(emCurso)
                .build();
    }

    /**
     * Escopo vazio nao e "sem restricao": e um link que nao mostra nada. Sem pedido
     * explicito vale CARTEIRA, que e o que o link sempre mostrou.
     */
    private Set<GrantScope> escopoPedido(AnimalShareRequestDTO request) {
        if (request == null || request.getScopes() == null || request.getScopes().isEmpty()) {
            return new LinkedHashSet<>(Set.of(GrantScope.CARTEIRA));
        }

        return new LinkedHashSet<>(request.getScopes());
    }

    private PetfyHealthcareException linkNaoEncontrado() {
        return new PetfyHealthcareException(
                ErrorMessageEnum.SHARE_NOT_FOUND.getMessage(),
                ErrorMessageEnum.SHARE_NOT_FOUND.getCode(),
                HttpStatus.NOT_FOUND);
    }

    private SharedVaccineCardDTO.SharedVaccineDTO toSharedVaccine(Vaccine vaccine, LocalDate hoje) {
        return SharedVaccineCardDTO.SharedVaccineDTO.builder()
                .vaccineName(vaccine.getVaccineName())
                .applicationDate(vaccine.getApplicationDate())
                .nextDoseDate(vaccine.getNextDoseDate())
                .status(vaccineStatusCalculator.classify(vaccine.getNextDoseDate(), hoje, windowDays))
                .organizationName(vaccine.getOrganization() != null ? vaccine.getOrganization().getName() : null)
                .build();
    }

    private AnimalShareResponseDTO toResponse(Grant grant, String token) {
        return AnimalShareResponseDTO.builder()
                .grantId(grant.getGrantId())
                .animalId(grant.getAnimal().getAnimalId())
                .token(token)
                .scopes(grant.getScopes())
                .expiresAt(grant.getExpiresAt())
                .revokedAt(grant.getRevokedAt())
                .creationDate(grant.getGrantedAt())
                .active(grant.estaVigente(LocalDateTime.now()))
                .build();
    }

}

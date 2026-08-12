package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.SponsoredEventDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipCostLineDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipOfferDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipRequestDTO;
import br.com.petfy.healthcare.domain.dto.SponsorshipResponseDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.SponsorshipRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.notification.SponsorshipNotifier;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.SponsorshipService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Apadrinhar: quem banca parte do custo de um animal de abrigo (Tela 46).
 *
 * <b>ESTE SERVICO NAO MOVE DINHEIRO.</b> Nao ha meio de pagamento em lugar nenhum deste produto, e o
 * que o desenho promete e outra coisa, que o produto ja sabe fazer: <i>"quando o remedio dele for
 * comprado, voce vai ver o evento — com data, valor e quem comprou."</i> O apadrinhamento e um
 * compromisso registrado; o valor corre fora.
 *
 * <b>E O PADRINHO NAO ALCANCA O ANIMAL.</b> Nenhum metodo aqui chama o {@code AnimalAccessGuard} para
 * o padrinho — a guarda dele e ser o dono do proprio apadrinhamento, e o que ele le sao eventos de
 * CUSTO, que moram fora da linha do tempo desde a Tela 40 justamente porque "nenhum escopo de acesso
 * concede preco junto com saude". E essa separacao, feita tres blocos atras por outra razao, que torna
 * esta tela possivel sem inventar escopo nenhum.
 */
@Service
@RequiredArgsConstructor
public class SponsorshipServiceImpl implements SponsorshipService {

    /** Os trinta dias que a tela promete ao abrigo. */
    private static final int DIAS_DE_AVISO = 30;

    private final SponsorshipRepository sponsorshipRepository;
    private final AnimalRepository animalRepository;
    private final AnimalCostRepository animalCostRepository;
    private final CustodyRepository custodyRepository;
    private final CurrentPersonProvider currentPersonProvider;
    private final CurrentProfessionalProvider currentProfessionalProvider;
    private final SponsorshipNotifier sponsorshipNotifier;

    @Override
    @Transactional(readOnly = true)
    public SponsorshipOfferDTO oferta(UUID animalId) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> erro(ErrorMessageEnum.ANIMAL_NOT_FOUND, HttpStatus.NOT_FOUND));

        Custody custodia = exigirOfertaAberta(animal);
        Organization abrigo = custodia.getHolderOrganization();

        Person eu = currentPersonProvider.require();

        Set<String> jaBancoPorMim = sponsorshipRepository
                .findVivosDoAnimal(animalId).stream()
                .filter(s -> s.getSponsor().getPersonId().equals(eu.getPersonId()))
                .map(s -> s.getDescription().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> jaBancadoPorAlguem = sponsorshipRepository
                .findVivosDoAnimal(animalId).stream()
                .map(s -> s.getDescription().toLowerCase())
                .collect(Collectors.toSet());

        List<SponsorshipCostLineDTO> linhas = custoMensal(animalId).stream()
                .map(custo -> SponsorshipCostLineDTO.builder()
                        .costId(custo.getAnimalCostId())
                        .description(custo.getDescription())
                        .amount(custo.getAmount())
                        .alreadySponsored(jaBancadoPorAlguem.contains(
                                custo.getDescription().toLowerCase()))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = linhas.stream()
                .map(SponsorshipCostLineDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SponsorshipOfferDTO.builder()
                .animalId(animalId)
                .name(animal.getName())
                .species(animal.getSpecies() == null ? null : animal.getSpecies().name())
                .breed(animal.getBreed())
                .bornDate(animal.getBornDate())
                .organizationName(abrigo.getName())
                .atOrganizationSince(custodia.getStartedAt() == null
                        ? null : custodia.getStartedAt().toLocalDate())
                .monthlyCosts(linhas)
                .monthlyTotal(total)
                .sponsorCount((int) sponsorshipRepository.findVivosDoAnimal(animalId).stream()
                        .map(s -> s.getSponsor().getPersonId())
                        .distinct()
                        .count())
                .canSponsor(!agindoPeloAbrigo(abrigo) && jaBancoPorMim.size() < linhas.size() + 1)
                .build();
    }

    @Override
    @Transactional
    public SponsorshipResponseDTO apadrinhar(UUID animalId, SponsorshipRequestDTO dto) {
        Animal animal = animalRepository.findById(animalId)
                .orElseThrow(() -> erro(ErrorMessageEnum.ANIMAL_NOT_FOUND, HttpStatus.NOT_FOUND));

        Custody custodia = exigirOfertaAberta(animal);
        Organization abrigo = custodia.getHolderOrganization();

        if (agindoPeloAbrigo(abrigo)) {
            throw erro(ErrorMessageEnum.CANNOT_SPONSOR_OWN_ANIMAL, HttpStatus.CONFLICT);
        }

        Person padrinho = currentPersonProvider.require();
        String descricao = dto.getDescription().trim();

        sponsorshipRepository
                .findVivoDoPadrinho(animalId, padrinho.getPersonId(), descricao)
                .ifPresent(existente -> {
                    throw erro(ErrorMessageEnum.SPONSORSHIP_ALREADY_ACTIVE, HttpStatus.CONFLICT);
                });

        exigirCustoDoAnimal(animalId, dto.getSourceCostId());

        Sponsorship apadrinhamento = sponsorshipRepository.save(Sponsorship.builder()
                .animal(animal)
                .sponsor(padrinho)
                .organization(abrigo)
                .description(descricao)
                .amount(dto.getAmount())
                .sourceCostId(dto.getSourceCostId())
                .startedOn(LocalDate.now())
                .status(SponsorshipStatus.ATIVO)
                .creationDate(LocalDateTime.now())
                .build());

        sponsorshipNotifier.apadrinhado(apadrinhamento);

        return toResponse(apadrinhamento, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SponsorshipResponseDTO> meus() {
        Person eu = currentPersonProvider.require();

        return sponsorshipRepository.findDoPadrinho(eu.getPersonId()).stream()
                // sem o nome de quem le: o produto nao precisa dizer a pessoa o nome dela
                .map(s -> toResponse(s, false))
                .collect(Collectors.toList());
    }

    /**
     * O feed do padrinho.
     *
     * <b>Os eventos vem do CUSTO, e o recorte e por descricao do que ele banca.</b> Nao e um filtro
     * bonito: e o unico recorte que a promessa da tela admite. "Ele ve o que banca" — o padrinho do
     * remedio ve as compras do remedio, e nao a consulta que outro padrinho banca.
     *
     * <b>Quem banca "Outro · valor livre" ve tudo do animal</b>, e isso e coerente: ele nao apontou
     * um custo, apontou o animal. Um feed vazio seria a leitura literal e a pior — ele bancaria no
     * escuro, que e exatamente o que esta tela existe para acabar.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SponsoredEventDTO> eventos(UUID sponsorshipId) {
        Sponsorship apadrinhamento = meuApadrinhamento(sponsorshipId);

        String oQueBanco = apadrinhamento.getDescription().toLowerCase();

        boolean apontouUmCusto = apadrinhamento.getSourceCostId() != null;

        return animalCostRepository
                .findByAnimalAnimalIdOrderByOccurredAtDesc(
                        apadrinhamento.getAnimal().getAnimalId()).stream()
                // so o que aconteceu DEPOIS de ele comecar a bancar: mostrar a compra de marco a
                // quem entrou em agosto sugeriria que o dinheiro dele pagou aquilo
                .filter(custo -> !custo.getOccurredAt().toLocalDate()
                        .isBefore(apadrinhamento.getStartedOn()))
                .filter(custo -> !apontouUmCusto
                        || custo.getDescription().toLowerCase().contains(oQueBanco)
                        || oQueBanco.contains(custo.getDescription().toLowerCase()))
                .map(custo -> SponsoredEventDTO.builder()
                        .costId(custo.getAnimalCostId())
                        .description(custo.getDescription())
                        .amount(custo.getAmount())
                        .occurredAt(custo.getOccurredAt())
                        .recordedByName(custo.getRecordedBy() == null
                                ? null : custo.getRecordedBy().getName())
                        .organizationName(custo.getOrganization() == null
                                ? null : custo.getOrganization().getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Parar de bancar, com trinta dias.
     *
     * <i>"Pode parar quando quiser, sem justificar. O abrigo e avisado com 30 dias para se
     * organizar."</i> Por isso nao ha campo de motivo, e por isso o estado nao vai direto para
     * {@code ENCERRADO}: ate a data, o padrinho continua cobrindo o custo, e o abrigo continua
     * contando com ele.
     */
    @Override
    @Transactional
    public SponsorshipResponseDTO encerrar(UUID sponsorshipId) {
        Sponsorship apadrinhamento = meuApadrinhamento(sponsorshipId);

        if (apadrinhamento.getStatus() != SponsorshipStatus.ATIVO) {
            throw erro(ErrorMessageEnum.SPONSORSHIP_ALREADY_ENDING, HttpStatus.CONFLICT);
        }

        apadrinhamento.setStatus(SponsorshipStatus.ENCERRAMENTO_PEDIDO);
        apadrinhamento.setCancelRequestedAt(LocalDateTime.now());
        apadrinhamento.setEndsOn(LocalDate.now().plusDays(DIAS_DE_AVISO));

        Sponsorship salvo = sponsorshipRepository.save(apadrinhamento);
        sponsorshipNotifier.apadrinhamentoVaiAcabar(salvo);

        return toResponse(salvo, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SponsorshipResponseDTO> daOrganizacao() {
        Organization abrigo = currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .orElseThrow(() -> erro(ErrorMessageEnum.AMBIGUOUS_CONTEXT, HttpStatus.CONFLICT));

        return sponsorshipRepository.findVivosDaOrganizacao(abrigo.getOrganizationId()).stream()
                // aqui o nome do padrinho VEM: sem ele a lista do abrigo e uma coluna de valores
                // sem ninguem atras, e ele nao tem a quem agradecer nem a quem perguntar
                .map(s -> toResponse(s, true))
                .collect(Collectors.toList());
    }

    /**
     * O animal esta aberto a padrinho?
     *
     * <b>Duas condicoes, e as duas sao necessarias.</b> A primeira e a decisao do abrigo
     * ({@code accepts_sponsorship}); a segunda e responder por ele uma ORGANIZACAO. A segunda nao e
     * detalhe tecnico: apadrinhar o cachorro de uma pessoa seria pagar a conta de alguem, e nao bancar
     * o cuidado de um animal que nao tem quem pague. E o animal adotado deixa de aceitar padrinho no
     * mesmo instante em que a custodia passa, sem ninguem precisar desmarcar nada.
     */
    private Custody exigirOfertaAberta(Animal animal) {
        boolean aceita = Boolean.TRUE.equals(animal.getAcceptsSponsorship());

        Optional<Custody> custodia = custodyRepository.findEmCurso(animal.getAnimalId())
                .filter(c -> c.getHolderOrganization() != null);

        if (!aceita || custodia.isEmpty()) {
            throw erro(ErrorMessageEnum.SPONSORSHIP_NOT_OFFERED, HttpStatus.NOT_FOUND);
        }

        return custodia.get();
    }

    /**
     * Quem esta lendo age em nome do proprio abrigo?
     *
     * A pergunta e sobre a organizacao DECLARADA, e nao sobre participar dela — a mesma regra do
     * {@code requireCustodia}. Um voluntario do abrigo agindo como PESSOA pode apadrinhar: ele esta
     * tirando do bolso dele, e recusar seria paternalismo.
     */
    private boolean agindoPeloAbrigo(Organization abrigo) {
        return currentProfessionalProvider
                .organizacaoDeclarada(currentPersonProvider.require())
                .map(declarada -> declarada.getOrganizationId().equals(abrigo.getOrganizationId()))
                .orElse(false);
    }

    /**
     * O gasto apontado e daquele animal?
     *
     * Sem esta conferencia, um cliente poderia apontar a linha de custo de outro animal — e a lista do
     * abrigo passaria a somar apadrinhamento de um gasto que nao e dele, sem nada denunciar.
     */
    private void exigirCustoDoAnimal(UUID animalId, UUID costId) {
        if (costId == null) {
            return;
        }

        boolean eDoAnimal = animalCostRepository.findById(costId)
                .map(custo -> custo.getAnimal().getAnimalId().equals(animalId))
                .orElse(false);

        if (!eDoAnimal) {
            throw erro(ErrorMessageEnum.COST_NOT_FROM_ANIMAL, HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    /**
     * "O que o abrigo gasta com ele por mes."
     *
     * <b>So o que se repete.</b> Uma consulta avulsa de tres anos atras nao e custo mensal, e somar
     * tudo daria ao padrinho um numero que nao corresponde a nada que ele possa bancar — o oposto de
     * "voce banca uma coisa concreta".
     */
    private List<AnimalCost> custoMensal(UUID animalId) {
        return animalCostRepository.findByAnimalAnimalIdOrderByOccurredAtDesc(animalId).stream()
                .filter(custo -> custo.getRecurrence() == CostRecurrence.MENSAL)
                .collect(Collectors.toList());
    }

    /**
     * O apadrinhamento de quem esta lendo.
     *
     * <b>404 e nao 403 para o de outra pessoa</b>, pela mesma razao do {@code AnimalAccessGuard}: um
     * 403 confirmaria que aquele id existe, e permitiria varrer ids para descobrir quem banca o que.
     */
    private Sponsorship meuApadrinhamento(UUID sponsorshipId) {
        UUID euId = currentPersonProvider.require().getPersonId();

        return sponsorshipRepository.findById(sponsorshipId)
                .filter(s -> s.getSponsor().getPersonId().equals(euId))
                .orElseThrow(() -> erro(ErrorMessageEnum.SPONSORSHIP_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private SponsorshipResponseDTO toResponse(Sponsorship s, boolean comNomeDoPadrinho) {
        return SponsorshipResponseDTO.builder()
                .sponsorshipId(s.getSponsorshipId())
                .animalId(s.getAnimal().getAnimalId())
                .animalName(s.getAnimal().getName())
                .organizationName(s.getOrganization().getName())
                .sponsorName(comNomeDoPadrinho ? s.getSponsor().getName() : null)
                .description(s.getDescription())
                .amount(s.getAmount())
                .status(s.getStatus())
                .startedOn(s.getStartedOn())
                .cancelRequestedAt(s.getCancelRequestedAt())
                .endsOn(s.getEndsOn())
                .build();
    }

    private PetfyHealthcareException erro(ErrorMessageEnum mensagem, HttpStatus status) {
        return new PetfyHealthcareException(mensagem.getMessage(), mensagem.getCode(), status);
    }

}

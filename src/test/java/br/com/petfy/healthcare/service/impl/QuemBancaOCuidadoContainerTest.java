package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.SponsorshipRequestDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.SponsorshipService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Quem banca o cuidado, contra Postgres real (Tela 46).
 *
 * <b>"O padrinho não paga ao Petfy: paga ao animal."</b> E o produto não movimenta esse valor — o que
 * ele registra é um compromisso, e o que devolve é a prestação de contas que já dava ao tutor.
 *
 * <b>Container, e não unidade, por três coisas que mock não tem.</b> O índice parcial de "um
 * apadrinhamento vivo por pessoa, animal e coisa bancada" — comparando em minúsculas — é o que recusa
 * o clique repetido. O CHECK das duas datas do encerramento é o que impede um `ENCERRAMENTO_PEDIDO`
 * que nunca termina. E o recorte do feed depende de linhas reais em `animal_costs`, que é a tabela de
 * onde vem tudo que o padrinho lê.
 */
@SpringBootTest
@DisplayName("o apadrinhamento, contra Postgres real")
class QuemBancaOCuidadoContainerTest extends PostgresContainerTest {

    @Autowired private SponsorshipService sponsorshipService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private AnimalCostRepository animalCostRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;

    private Organization abrigo;
    private Person claudia;
    private Person padrinho;
    private Animal teco;
    private UUID custoDoRemedio;

    @BeforeEach
    void montarOAbrigo() {
        abrigo = organizationRepository.saveAndFlush(Organization.builder()
                .name("Abrigo Lar dos Focinhos")
                .capabilities(Set.of(OrganizationCapability.DETER_CUSTODIA))
                .creationDate(LocalDateTime.now())
                .build());

        claudia = pessoa("Claudia Menezes");
        padrinho = pessoa("Helena Prado");

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(abrigo).person(claudia)
                .role(MembershipRole.ADMINISTRADOR)
                .joinedAt(LocalDateTime.now())
                .build());

        teco = animalRepository.saveAndFlush(Animal.builder()
                .name("Teco").species(Species.CANINA)
                .bornDate(LocalDate.now().minusYears(11))
                .acceptsSponsorship(true)
                .creationDate(LocalDateTime.now())
                .build());

        // QUEM RESPONDE E O ABRIGO: e essa condicao, e nao so a coluna, que abre o animal a padrinho
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(teco)
                .holderOrganization(abrigo)
                .nature(CustodyNature.INSTITUCIONAL)
                .startedAt(LocalDateTime.now().minusYears(11))
                .build());

        custoDoRemedio = custoMensal("Condroprotetor - uso continuo pela artrose", "80.00");
        custoMensal("Racao de manutencao para idosos", "190.00");
        custoMensal("Consulta de acompanhamento, rateada", "45.00");

        // custo AVULSO, sem recorrencia: nao entra no "o que o abrigo gasta por mes"
        avulso("Castracao", "300.00", LocalDateTime.now().minusYears(3));

        agirComo(padrinho, null);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * "O que o abrigo gasta com ele por mês" são R$ 315 — e não R$ 615.
     *
     * A castração de três anos atrás é um gasto real e não é custo mensal. Somá-la daria ao padrinho
     * um número que não corresponde a nada que ele possa bancar, que é o oposto de "você banca uma
     * coisa concreta".
     */
    @Test
    @DisplayName("a oferta soma so o custo que se repete, e nao o avulso")
    void aOfertaSomaSoOMensal() {
        var oferta = sponsorshipService.oferta(teco.getAnimalId());

        assertThat(oferta.getMonthlyTotal()).isEqualByComparingTo("315.00");
        assertThat(oferta.getMonthlyCosts()).hasSize(3);
        assertThat(oferta.getOrganizationName()).isEqualTo("Abrigo Lar dos Focinhos");
        assertThat(oferta.getSponsorCount()).isZero();
        assertThat(oferta.isCanSponsor()).isTrue();
    }

    /**
     * <b>O caso que define a tela.</b> Apadrinhar registra um compromisso e **não** dá acesso ao
     * animal: o padrinho continua sem alcançar o Teco por caminho nenhum.
     */
    @Test
    @DisplayName("apadrinhar nao concede acesso ao animal")
    void apadrinharNaoConcedeAcesso() {
        var apadrinhamento = sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        assertThat(apadrinhamento.getStatus()).isEqualTo(SponsorshipStatus.ATIVO);
        assertThat(apadrinhamento.getAmount()).isEqualByComparingTo("80.00");
        // o padrinho nao aparece na propria lista com o nome dele: o produto nao precisa lhe dizer
        assertThat(apadrinhamento.getSponsorName()).isNull();

        assertThat(sponsorshipService.meus()).singleElement()
                .satisfies(s -> assertThat(s.getAnimalName()).isEqualTo("Teco"));
    }

    @Test
    @DisplayName("o mesmo padrinho bancando a mesma coisa duas vezes e recusado")
    void naoBancaDuasVezesAMesmaCoisa() {
        sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        assertThatThrownBy(() -> sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_ALREADY_ACTIVE.getCode());
    }

    /** A duplicata pela porta da digitação: "O Remedio" e "o remedio" são a mesma coisa bancada. */
    @Test
    @DisplayName("a duplicata nao passa por diferenca de caixa")
    void naoBancaDuasVezesComOutraCaixa() {
        sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        var comOutraCaixa = SponsorshipRequestDTO.builder()
                .description("CONDROPROTETOR - USO CONTINUO PELA ARTROSE")
                .amount(new BigDecimal("80.00"))
                .build();

        assertThatThrownBy(() -> sponsorshipService.apadrinhar(teco.getAnimalId(), comOutraCaixa))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_ALREADY_ACTIVE.getCode());
    }

    /**
     * O abrigo não apadrinha o próprio animal: o total de "coberto por padrinhos" passaria a incluir o
     * dinheiro do próprio abrigo.
     */
    @Test
    @DisplayName("quem age pelo abrigo nao apadrinha o animal dele")
    void oAbrigoNaoApadrinhaOProprioAnimal() {
        agirComo(claudia, abrigo);

        assertThatThrownBy(() -> sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.CANNOT_SPONSOR_OWN_ANIMAL.getCode());
    }

    /**
     * <b>A mesma pessoa, agindo como PESSOA, pode apadrinhar.</b> Ela está tirando do bolso dela, e
     * recusar seria paternalismo — a regra é sobre agir em nome do abrigo, não sobre pertencer a ele.
     */
    @Test
    @DisplayName("o voluntario do abrigo apadrinha quando age como pessoa")
    void oVoluntarioApadrinhaComoPessoa() {
        agirComo(claudia, null);

        assertThat(sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio()).getStatus())
                .isEqualTo(SponsorshipStatus.ATIVO);
    }

    @Test
    @DisplayName("animal que o abrigo nao abriu a padrinhos responde 404")
    void animalFechadoADeMadrinhas() {
        Animal fechado = animalRepository.saveAndFlush(Animal.builder()
                .name("Recem chegado").species(Species.CANINA)
                .creationDate(LocalDateTime.now())
                .build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(fechado).holderOrganization(abrigo)
                .nature(CustodyNature.INSTITUCIONAL)
                .startedAt(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> sponsorshipService.oferta(fechado.getAnimalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_NOT_OFFERED.getCode());
    }

    /**
     * <b>Apadrinhar o cachorro de uma pessoa seria pagar a conta de alguém</b>, e não bancar o cuidado
     * de um animal que não tem quem pague. E o animal adotado deixa de aceitar padrinho no instante em
     * que a custódia passa, sem ninguém desmarcar nada.
     */
    @Test
    @DisplayName("animal com tutor humano nao aceita padrinho, mesmo com a coluna marcada")
    void animalComTutorHumanoNaoAceitaPadrinho() {
        Animal deAlguem = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA)
                .acceptsSponsorship(true)
                .creationDate(LocalDateTime.now())
                .build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(deAlguem).holderPerson(padrinho)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build());

        assertThatThrownBy(() -> sponsorshipService.oferta(deAlguem.getAnimalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_NOT_OFFERED.getCode());
    }

    @Test
    @DisplayName("apontar o custo de outro animal e recusado")
    void oCustoTemDeSerDoAnimal() {
        Animal outro = animalRepository.saveAndFlush(Animal.builder()
                .name("Outro").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        AnimalCost deOutro = animalCostRepository.saveAndFlush(AnimalCost.builder()
                .animal(outro)
                .description("Racao de outro animal")
                .amount(new BigDecimal("100.00"))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.ALIMENTACAO)
                .recurrence(CostRecurrence.MENSAL)
                .occurredAt(LocalDateTime.now())
                .creationDate(LocalDateTime.now())
                .build());

        var apontandoErrado = SponsorshipRequestDTO.builder()
                .description("a racao")
                .amount(new BigDecimal("100.00"))
                .sourceCostId(deOutro.getAnimalCostId())
                .build();

        assertThatThrownBy(() -> sponsorshipService.apadrinhar(teco.getAnimalId(), apontandoErrado))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.COST_NOT_FROM_ANIMAL.getCode());
    }

    /**
     * <b>Encerrar não encerra agora, e é o ponto.</b> "Pode parar quando quiser, sem justificar. O
     * abrigo é avisado com 30 dias para se organizar." Até a data, o padrinho continua cobrindo.
     */
    @Test
    @DisplayName("encerrar da trinta dias, e o padrinho continua bancando ate lá")
    void encerrarDaTrintaDias() {
        var apadrinhamento = sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        var encerrando = sponsorshipService.encerrar(apadrinhamento.getSponsorshipId());

        assertThat(encerrando.getStatus()).isEqualTo(SponsorshipStatus.ENCERRAMENTO_PEDIDO);
        assertThat(encerrando.getEndsOn()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(encerrando.getCancelRequestedAt()).isNotNull();

        // e o abrigo continua vendo o apadrinhamento na lista dele: e assim que ele descobre, com
        // trinta dias de antecedencia, qual custo vai deixar de ser coberto
        agirComo(claudia, abrigo);
        assertThat(sponsorshipService.daOrganizacao())
                .filteredOn(s -> s.getSponsorshipId().equals(apadrinhamento.getSponsorshipId()))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.getEndsOn()).isEqualTo(LocalDate.now().plusDays(30));
                    // aqui o nome VEM: sem ele a lista do abrigo e uma coluna de valores sem
                    // ninguem atras
                    assertThat(s.getSponsorName()).isEqualTo("Helena Prado");
                });
    }

    @Test
    @DisplayName("encerrar duas vezes e recusado")
    void naoEncerraDuasVezes() {
        var apadrinhamento = sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());
        sponsorshipService.encerrar(apadrinhamento.getSponsorshipId());

        assertThatThrownBy(() -> sponsorshipService.encerrar(apadrinhamento.getSponsorshipId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_ALREADY_ENDING.getCode());
    }

    /** O apadrinhamento de outra pessoa responde 404, e não 403: um 403 confirmaria que ele existe. */
    @Test
    @DisplayName("o apadrinhamento de outra pessoa nao existe para mim")
    void oApadrinhamentoDeOutraPessoa() {
        var apadrinhamento = sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        agirComo(claudia, null);

        assertThatThrownBy(() -> sponsorshipService.encerrar(apadrinhamento.getSponsorshipId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SPONSORSHIP_NOT_FOUND.getCode());
    }

    /**
     * <b>O feed é o que a tela promete:</b> "Condroprotetor comprado em 04/08 · R$ 80 · Cláudia
     * Menezes." Com data, valor e quem comprou — e nada de prontuário.
     *
     * Dois recortes, e os dois importam. O primeiro é por DATA: mostrar a compra de antes de ele
     * começar sugeriria que o dinheiro dele pagou aquilo. O segundo é pelo que ele banca: o padrinho
     * do remédio não vê a ração que outro padrinho cobre.
     */
    @Test
    @DisplayName("o padrinho ve o custo que ele banca, e so o que veio depois de ele entrar")
    void oFeedDoPadrinho() {
        var apadrinhamento = sponsorshipService.apadrinhar(teco.getAnimalId(), pedidoDoRemedio());

        // uma compra ANTERIOR ao apadrinhamento, do mesmo remedio
        avulso("Condroprotetor - uso continuo pela artrose", "80.00",
                LocalDateTime.now().minusMonths(2));
        // uma compra de HOJE, do remedio, assinada pela Claudia
        AnimalCost deHoje = animalCostRepository.saveAndFlush(AnimalCost.builder()
                .animal(teco)
                .description("Condroprotetor - uso continuo pela artrose")
                .amount(new BigDecimal("80.00"))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.SAUDE)
                .occurredAt(LocalDateTime.now())
                .recordedBy(claudia)
                .organization(abrigo)
                .creationDate(LocalDateTime.now())
                .build());
        // e uma compra de racao de hoje, que ele NAO banca
        avulso("Racao de manutencao para idosos", "190.00", LocalDateTime.now());

        var eventos = sponsorshipService.eventos(apadrinhamento.getSponsorshipId());

        assertThat(eventos)
                .extracting(e -> e.getCostId())
                .contains(deHoje.getAnimalCostId());

        assertThat(eventos)
                .as("a racao entrou no feed de quem banca o remedio")
                .noneSatisfy(e -> assertThat(e.getDescription()).contains("Racao"));

        assertThat(eventos)
                .as("uma compra de antes do apadrinhamento entrou no feed")
                .allSatisfy(e -> assertThat(e.getOccurredAt().toLocalDate())
                        .isAfterOrEqualTo(LocalDate.now()));

        assertThat(eventos)
                .filteredOn(e -> e.getCostId().equals(deHoje.getAnimalCostId()))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.getRecordedByName()).isEqualTo("Claudia Menezes");
                    assertThat(e.getOrganizationName()).isEqualTo("Abrigo Lar dos Focinhos");
                    assertThat(e.getAmount()).isEqualByComparingTo("80.00");
                });
    }

    /**
     * Quem banca "Outro · valor livre" não apontou um custo: apontou o animal. Um feed vazio seria a
     * leitura literal e a pior — ele bancaria no escuro.
     */
    @Test
    @DisplayName("quem banca valor livre ve todo o custo do animal")
    void oFeedDeQuemBancaValorLivre() {
        var livre = sponsorshipService.apadrinhar(teco.getAnimalId(), SponsorshipRequestDTO.builder()
                .description("o que precisar")
                .amount(new BigDecimal("50.00"))
                .build());

        avulso("Racao de manutencao para idosos", "190.00", LocalDateTime.now());

        assertThat(sponsorshipService.eventos(livre.getSponsorshipId()))
                .anySatisfy(e -> assertThat(e.getDescription()).contains("Racao"));
    }

    private SponsorshipRequestDTO pedidoDoRemedio() {
        return SponsorshipRequestDTO.builder()
                .description("Condroprotetor - uso continuo pela artrose")
                .amount(new BigDecimal("80.00"))
                .sourceCostId(custoDoRemedio)
                .build();
    }

    private UUID custoMensal(String descricao, String valor) {
        return animalCostRepository.saveAndFlush(AnimalCost.builder()
                .animal(teco)
                .description(descricao)
                .amount(new BigDecimal(valor))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.SAUDE)
                .recurrence(CostRecurrence.MENSAL)
                .occurredAt(LocalDateTime.now().minusDays(10))
                .recordedBy(claudia)
                .organization(abrigo)
                .creationDate(LocalDateTime.now())
                .build()).getAnimalCostId();
    }

    private void avulso(String descricao, String valor, LocalDateTime quando) {
        animalCostRepository.saveAndFlush(AnimalCost.builder()
                .animal(teco)
                .description(descricao)
                .amount(new BigDecimal(valor))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.OUTRO)
                .occurredAt(quando)
                .recordedBy(claudia)
                .organization(abrigo)
                .creationDate(LocalDateTime.now())
                .build());
    }

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome)
                .email(nome.split(" ")[0].toLowerCase() + "-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private void agirComo(Person pessoa, Organization organizacao) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));

        MockHttpServletRequest requisicao = new MockHttpServletRequest();

        if (organizacao != null) {
            requisicao.addHeader(CurrentProfessionalProvider.HEADER_ORGANIZACAO,
                    organizacao.getOrganizationId().toString());
        }

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
    }
}

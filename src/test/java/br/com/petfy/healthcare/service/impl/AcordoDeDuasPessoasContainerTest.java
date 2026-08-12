package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AnimalSightingRequestDTO;
import br.com.petfy.healthcare.domain.dto.GroupApprovalRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.GroupApprovalKind;
import br.com.petfy.healthcare.domain.entity.GroupApprovalStatus;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalSightingService;
import br.com.petfy.healthcare.service.GroupApprovalService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O animal que é de todos, contra Postgres real (Telas 43 e 44).
 *
 * <b>"Sem dono, a proteção contra o gesto irreversível de uma pessoa só é o acordo de duas."</b> É
 * essa frase que estes casos guardam — e ela não tem onde mais ser guardada: num animal com tutor
 * o irreversível é barrado por quem responde, e aqui todas as seis pessoas do grupo passam no
 * {@code requireCustodia} pela organização.
 *
 * <b>Container, e não unidade, por duas coisas que mock não tem.</b> Os CHECKs de coerência do
 * alvo e de "quem pede não decide" moram no banco além do serviço — e é justamente a duplicata que
 * protege a regra de uma refatoração futura. E o índice único do avistamento (animal, pessoa, dia)
 * é o que torna o gesto idempotente: sem ele, o segundo toque criaria linha nova e nenhum teste de
 * mock veria.
 */
@SpringBootTest
@DisplayName("o acordo de duas pessoas, contra Postgres real")
class AcordoDeDuasPessoasContainerTest extends PostgresContainerTest {

    @Autowired private GroupApprovalService groupApprovalService;
    @Autowired private AnimalSightingService animalSightingService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;

    private Organization grupo;
    private Person sandra;
    private Person marta;
    private Person paula;
    private Animal branquinha;

    @BeforeEach
    void montarAColonia() {
        grupo = organizationRepository.saveAndFlush(Organization.builder()
                .name("Grupo Gatos da Benedito")
                .capabilities(Set.of(OrganizationCapability.DETER_CUSTODIA))
                .creationDate(LocalDateTime.now())
                .build());

        sandra = pessoa("Sandra Prado");
        marta = pessoa("Marta Nogueira");
        paula = pessoa("Paula Rezende");

        membro(sandra, "Alimenta de manha");
        membro(marta, "Alimenta a noite");

        branquinha = animalRepository.saveAndFlush(Animal.builder()
                .name("Branquinha").species(Species.FELINA).creationDate(LocalDateTime.now()).build());

        // NINGUEM E DONO: quem responde e o grupo, e nao uma pessoa. E este estado que faz o
        // requireCustodia deixar as seis passarem — e que exige o acordo de duas.
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(branquinha)
                .holderOrganization(grupo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(3))
                .build());

        agirComo(sandra, grupo);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome)
                .email(nome.split(" ")[0].toLowerCase() + "-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private void membro(Person quem, String turno) {
        membershipRepository.saveAndFlush(Membership.builder()
                .organization(grupo).person(quem)
                .role(MembershipRole.VOLUNTARIO)
                .contribution(turno)
                .joinedAt(LocalDateTime.now())
                .build());
    }

    private GroupApprovalRequestDTO pedidoDeAdocao() {
        return GroupApprovalRequestDTO.builder()
                .kind(GroupApprovalKind.ADOCAO)
                .animalId(branquinha.getAnimalId())
                .toPersonId(paula.getPersonId())
                .reason("Paula visita a praca ha meses e ja leva a Branquinha ao veterinario.")
                .build();
    }

    /**
     * <b>O caso que define o bloco inteiro.</b> Sandra pede e Sandra tenta concordar: 403 com
     * código próprio, e o pedido continua pendente.
     */
    @Test
    @DisplayName("quem pede nao concorda consigo, e o pedido continua esperando")
    void quemPedeNaoConcordaConsigo() {
        var pedido = groupApprovalService.pedir(pedidoDeAdocao());

        assertThatThrownBy(() -> groupApprovalService.concordar(pedido.getGroupApprovalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.CANNOT_APPROVE_OWN_REQUEST.getCode());

        assertThat(groupApprovalService.pendentes())
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.getStatus()).isEqualTo(GroupApprovalStatus.PENDENTE);
                    // quem pediu VE o proprio pedido, e ve que nao pode decidi-lo: esconde-lo
                    // pareceria limpo e faria o pedido parado sumir de quem mais precisa cobra-lo
                    assertThat(p.isCanDecide()).isFalse();
                });
    }

    @Test
    @DisplayName("a segunda pessoa concorda, e o ato acontece na mesma transacao")
    void aSegundaPessoaConcorda() {
        var pedido = groupApprovalService.pedir(pedidoDeAdocao());

        agirComo(marta, grupo);
        assertThat(groupApprovalService.pendentes()).singleElement()
                .satisfies(p -> assertThat(p.isCanDecide()).isTrue());

        var decidido = groupApprovalService.concordar(pedido.getGroupApprovalId());

        assertThat(decidido.getStatus()).isEqualTo(GroupApprovalStatus.CONCORDADO);
        assertThat(decidido.getDecidedByName()).isEqualTo("Marta Nogueira");
        assertThat(groupApprovalService.pendentes()).isEmpty();

        /*
         * A ADOCAO VIRA CONVITE, e não transferência direta — a decisão menos óbvia do bloco.
         *
         * O desenho diz "Paula passa a responder por ela", mas o produto inteiro exige que quem
         * recebe um animal consinta. A custódia continua com o grupo até Paula aceitar: passá-la
         * sem o aceite colocaria um animal sob a responsabilidade de quem ainda não disse sim.
         */
        assertThat(custodyRepository.findEmCurso(branquinha.getAnimalId()))
                .isPresent()
                .get()
                .satisfies(c -> assertThat(c.getHolderOrganization().getOrganizationId())
                        .isEqualTo(grupo.getOrganizationId()));
    }

    @Test
    @DisplayName("quem nao e do grupo nao decide, mesmo alcancando o animal")
    void deForaDoGrupoNaoDecide() {
        var pedido = groupApprovalService.pedir(pedidoDeAdocao());

        agirComo(paula, grupo);

        assertThatThrownBy(() -> groupApprovalService.concordar(pedido.getGroupApprovalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.NOT_A_GROUP_MEMBER.getCode());
    }

    @Test
    @DisplayName("decidir duas vezes e recusado")
    void naoDecideDuasVezes() {
        var pedido = groupApprovalService.pedir(pedidoDeAdocao());

        agirComo(marta, grupo);
        groupApprovalService.concordar(pedido.getGroupApprovalId());

        assertThatThrownBy(() -> groupApprovalService.recusar(pedido.getGroupApprovalId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.GROUP_APPROVAL_ALREADY_DECIDED.getCode());
    }

    @Test
    @DisplayName("o mesmo pedido duas vezes e recusado pelo indice parcial")
    void naoPedeDuasVezes() {
        groupApprovalService.pedir(pedidoDeAdocao());

        assertThatThrownBy(() -> groupApprovalService.pedir(pedidoDeAdocao()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.GROUP_APPROVAL_ALREADY_PENDING.getCode());
    }

    /**
     * O CHECK de coerência do alvo mora no banco além do serviço, e este caso guarda o serviço:
     * um pedido de adoção sem destinatário seria gravado e só explodiria na hora de executar —
     * depois de a segunda pessoa já ter concordado com um pedido que não dizia sobre o quê era.
     */
    @Test
    @DisplayName("pedido de adocao sem para-quem e recusado antes de gravar")
    void adocaoPrecisaDeDestino() {
        var incompleto = GroupApprovalRequestDTO.builder()
                .kind(GroupApprovalKind.ADOCAO)
                .animalId(branquinha.getAnimalId())
                .build();

        assertThatThrownBy(() -> groupApprovalService.pedir(incompleto))
                .isInstanceOf(PetfyHealthcareException.class);

        assertThat(groupApprovalService.pendentes()).isEmpty();
    }

    @Test
    @DisplayName("o obito da colonia encerra pelo fluxo da Tela 33, com a data de quem viu")
    void obitoUsaOFluxoDaTela33() {
        var pedido = groupApprovalService.pedir(GroupApprovalRequestDTO.builder()
                .kind(GroupApprovalKind.OBITO)
                .animalId(branquinha.getAnimalId())
                .deceasedOn(LocalDate.now().minusDays(2))
                .reason("Encontrada sem vida perto da banca.")
                .build());

        agirComo(marta, grupo);
        groupApprovalService.concordar(pedido.getGroupApprovalId());

        // a custodia do GRUPO encerra sem sucessor, exatamente como a de um tutor
        assertThat(custodyRepository.findEmCurso(branquinha.getAnimalId())).isEmpty();
    }

    /**
     * O gesto mais frequente da tela, e o único que NÃO passa pelo acordo de duas: "o que qualquer
     * um pode fazer — marcar que viu o gato".
     */
    @Test
    @DisplayName("marcar que viu e idempotente por pessoa, animal e dia")
    void avistamentoIdempotente() {
        var primeiro = animalSightingService.registrar(branquinha.getAnimalId(),
                new AnimalSightingRequestDTO());
        var segundo = animalSightingService.registrar(branquinha.getAnimalId(),
                new AnimalSightingRequestDTO());

        // Sandra passa na praca de manha e a noite: o segundo toque devolve o mesmo registro. Um
        // 409 aqui a ensinaria a nao tocar, e sem o toque a tela nao sabe quem sumiu.
        assertThat(segundo.getAnimalSightingId()).isEqualTo(primeiro.getAnimalSightingId());
        assertThat(segundo.getDaysSince()).isZero();

        // Marta vendo o MESMO gato no MESMO dia e registro novo: confirmacao e informacao
        agirComo(marta, grupo);
        var deOutraPessoa = animalSightingService.registrar(branquinha.getAnimalId(),
                new AnimalSightingRequestDTO());

        assertThat(deOutraPessoa.getAnimalSightingId()).isNotEqualTo(primeiro.getAnimalSightingId());
        assertThat(deOutraPessoa.getRecordedByName()).isEqualTo("Marta Nogueira");
    }

    /**
     * <b>Este caso nasceu de um defeito que ele mesmo encontrou.</b>
     *
     * O {@code requireEscrita} só olhava custódia de PESSOA e concessão — a custódia de
     * organização contava apenas no {@code requireCustodia}, desde a Tela 13. O efeito era mudo e
     * grande: um animal cuja custódia é de uma organização, sem tutor humano, não podia receber
     * registro nenhum de quem cuida dele. O abrigo não lançava peso no animal que resgatou, e a
     * colônia inteira seria inconstruível — "marcar que viu, registrar ferida, foto" é escrita.
     *
     * Sem a correção, todos os casos de avistamento acima respondem 404.
     */
    @Test
    @DisplayName("quem cuida escreve no animal do grupo, sem concessao nenhuma")
    void custodiaDeOrganizacaoDaEscrita() {
        // Sandra nao tem concessao no animal: ela alcanca pela custodia do GRUPO
        assertThat(animalSightingService.registrar(branquinha.getAnimalId(),
                new AnimalSightingRequestDTO()).getRecordedByName())
                .isEqualTo("Sandra Prado");
    }

    @Test
    @DisplayName("ver o animal amanha e recusado")
    void avistamentoNoFuturo() {
        var amanha = AnimalSightingRequestDTO.builder().seenOn(LocalDate.now().plusDays(1)).build();

        assertThatThrownBy(() -> animalSightingService.registrar(branquinha.getAnimalId(), amanha))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SIGHTING_DATE_IN_FUTURE.getCode());
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

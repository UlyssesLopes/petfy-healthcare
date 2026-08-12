package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.ReferralRequestDTO;
import br.com.petfy.healthcare.domain.dto.ReferralScopeOptionDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.ReferralService;
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

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O caso que passa adiante, contra Postgres real (Tela 45).
 *
 * <b>"Encaminhar é você indicando o caminho; conceder acesso continua sendo dele, como sempre
 * foi."</b> É essa frase que estes casos guardam, e ela se quebra de dois jeitos opostos: se o
 * encaminhamento concedesse algo por si, a clínica passaria a dar acesso a terceiros sem o tutor; se
 * o aceite não concedesse, a tela prometeria ao especialista um caso que ele abre em 404.
 *
 * <b>Container, e não unidade, por três coisas que mock não tem.</b> O CHECK de "só o autorizado tem
 * concessão" mora no banco além do serviço. O índice parcial de um pendente por animal e
 * destinatário é o que recusa a segunda pergunta ao mesmo tutor. E a união de escopos com a
 * concessão que já existia — o caso que mais importa aqui — depende de uma linha real em
 * {@code grants}, porque o defeito que ela evita é justamente o de gravar por cima.
 */
@SpringBootTest
@DisplayName("o encaminhamento, contra Postgres real")
class OCasoQuePassaAdianteContainerTest extends PostgresContainerTest {

    @Autowired private ReferralService referralService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ProfessionalCredentialRepository credentialRepository;

    /**
     * A concessão é lida pelo banco, e não pela entidade.
     *
     * <b>Não é preferência de estilo: {@code Grant.scopes} é uma {@code @ElementCollection}
     * preguiçosa</b>, e o {@code open-in-view=false} deste projeto fecha a sessão ao sair do
     * repositório — tocar a coleção aqui estoura {@code LazyInitializationException}, que é o mesmo
     * defeito que o serviço evita copiando o conjunto. E ler pelo SQL afirma o que ficou GRAVADO, que
     * é exatamente o que um teste de container deveria afirmar.
     */
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Person marcelo;
    private Person ana;
    private Person roberto;
    private Organization vetNorte;
    private Organization anhangabau;
    private Animal code;

    /**
     * O CRMV do Roberto, sorteado.
     *
     * O número do registro é único no schema inteiro, e o container é o mesmo para a classe: um
     * número fixo passaria no primeiro caso e violaria a chave em todos os seguintes. É a mesma razão
     * pela qual o e-mail de cada pessoa leva um UUID.
     */
    private String crmvDoRoberto;

    @BeforeEach
    void montarOCaso() {
        marcelo = pessoa("Marcelo Dias");
        ana = pessoa("Ana Ferreira");
        roberto = pessoa("Roberto Lins");

        vetNorte = clinica("Clinica Vet Norte");
        anhangabau = clinica("Clinica Anhangabau");

        membro(ana, vetNorte);
        membro(roberto, anhangabau);

        credencial(ana, null);
        crmvDoRoberto = credencial(roberto, "ortopedia");

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        // Marcelo RESPONDE pelo animal: e ele que autoriza, e ninguem mais
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(marcelo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(7))
                .build());

        // Ana alcanca o animal por CONCESSAO, com escrita: e o que ela precisa para atender e o que
        // NAO basta para autorizar nada
        concessao(ana, Set.of(GrantScope.PRONTUARIO, GrantScope.CARTEIRA), null);

        agirComo(ana, vetNorte);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * <b>O caso que define o bloco inteiro.</b> Ana encaminha, e o Roberto continua sem alcançar o
     * animal — porque encaminhar não concede.
     */
    @Test
    @DisplayName("encaminhar nao concede nada, e o especialista continua fora")
    void encaminharNaoConcede() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        assertThat(pedido.getStatus()).isEqualTo(ReferralStatus.PENDENTE);
        assertThat(pedido.getAccessDays()).isEqualTo(90);
        assertThat(pedido.getAccessExpiresAt()).isNull();

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now()))
                .as("o encaminhamento criou concessao antes de o tutor decidir")
                .isEmpty();
    }

    /**
     * <b>Ana tem escrita no animal e não pode autorizar o próprio encaminhamento.</b>
     *
     * Sem esta recusa, a clínica com acesso concedido autorizaria o que ela mesma pediu — o que é o
     * mesmo que lhe dar o poder de conceder acesso a terceiros, o cenário que o
     * {@code requireCustodia} existe para impedir desde o P2b.
     */
    @Test
    @DisplayName("quem encaminha nao autoriza, mesmo tendo escrita no animal")
    void quemEncaminhaNaoAutoriza() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        assertThatThrownBy(() -> referralService.autorizar(pedido.getReferralId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.INSUFFICIENT_ANIMAL_ROLE.getCode());

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now())).isEmpty();
    }

    @Test
    @DisplayName("o tutor autoriza, e a concessao de 90 dias nasce concedida por ele")
    void oTutorAutorizaEAConcessaoNasce() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        agirComo(marcelo, null);

        assertThat(referralService.pendentesParaDecisao()).singleElement()
                .satisfies(p -> assertThat(p.isCanDecide()).isTrue());

        var decidido = referralService.autorizar(pedido.getReferralId());

        assertThat(decidido.getStatus()).isEqualTo(ReferralStatus.AUTORIZADO);
        assertThat(decidido.getDecidedByName()).isEqualTo("Marcelo Dias");
        assertThat(decidido.getAccessExpiresAt())
                .isAfter(LocalDateTime.now().plusDays(89))
                .isBefore(LocalDateTime.now().plusDays(91));

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now()))
                .isPresent()
                .get()
                .satisfies(g -> {
                    // QUEM CONCEDEU E O TUTOR, e e isso que diferencia esta linha da excecao que a
                    // Tela 33 recusou: la nao havia quem concedesse nem quem revogasse
                    assertThat(g.getGrantedBy().getPersonId()).isEqualTo(marcelo.getPersonId());
                    // EDITOR, e nao VIEWER: "o especialista que registra ali passa a devolver o
                    // retorno para a clinica que encaminhou"
                    assertThat(g.getLevel()).isEqualTo(GrantLevel.EDITOR);
                });

        assertThat(escoposDoRoberto()).containsExactlyInAnyOrder(
                "PRONTUARIO", "ANEXOS", "PESO", "OBSERVACOES");
    }

    /**
     * <b>O caso mais importante desta classe, e o menos óbvio.</b>
     *
     * Roberto já é co-tutor com acesso permanente à carteira. Autorizar o encaminhamento tem de
     * SOMAR: gravar prazo e escopos por cima encolheria o acesso dele — o tutor autorizaria a ver
     * mais e o efeito seria ver menos, e ninguém descobriria antes de o co-tutor abrir a carteira e
     * não achar a vacina.
     */
    @Test
    @DisplayName("a concessao que ja existia nao encolhe: os escopos somam e o prazo nao aparece")
    void aConcessaoQueJaExistiaNaoEncolhe() {
        concessao(roberto, Set.of(GrantScope.CARTEIRA), null);

        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        agirComo(marcelo, null);
        referralService.autorizar(pedido.getReferralId());

        assertThat(escoposDoRoberto())
                .as("os escopos foram gravados por cima em vez de somados")
                .contains("CARTEIRA", "PRONTUARIO");

        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now()))
                .isPresent()
                .get()
                .satisfies(g -> assertThat(g.getExpiresAt())
                        .as("um acesso sem prazo ganhou data de fim porque alguem encaminhou")
                        .isNull());
    }

    @Test
    @DisplayName("o tutor recusa, e nenhuma concessao nasce")
    void oTutorRecusa() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        agirComo(marcelo, null);
        var decidido = referralService.recusar(pedido.getReferralId());

        assertThat(decidido.getStatus()).isEqualTo(ReferralStatus.RECUSADO);
        assertThat(decidido.getAccessExpiresAt()).isNull();
        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now())).isEmpty();

        // nao apaga: quem encaminhou precisa ver que foi recusado, e nao que o pedido sumiu
        agirComo(ana, vetNorte);
        assertThat(referralService.doAnimal(code.getAnimalId())).singleElement()
                .satisfies(r -> assertThat(r.getStatus()).isEqualTo(ReferralStatus.RECUSADO));
    }

    @Test
    @DisplayName("decidir duas vezes e recusado")
    void naoDecideDuasVezes() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        agirComo(marcelo, null);
        referralService.autorizar(pedido.getReferralId());

        assertThatThrownBy(() -> referralService.recusar(pedido.getReferralId()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.REFERRAL_ALREADY_DECIDED.getCode());
    }

    @Test
    @DisplayName("o mesmo animal para o mesmo especialista, duas vezes, e recusado")
    void naoEncaminhaDuasVezes() {
        referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        assertThatThrownBy(() -> referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto()))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.REFERRAL_ALREADY_PENDING.getCode());
    }

    @Test
    @DisplayName("encaminhar para si mesmo e recusado")
    void naoEncaminhaParaSiMesmo() {
        var paraMim = ReferralRequestDTO.builder()
                .toPersonId(ana.getPersonId())
                .reason("Piora da claudicacao.")
                .scopes(Set.of(GrantScope.PRONTUARIO))
                .build();

        assertThatThrownBy(() -> referralService.encaminhar(code.getAnimalId(), paraMim))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.CANNOT_REFER_TO_SELF.getCode());
    }

    /**
     * Sem esta recusa, encaminhar viraria o caminho mais curto para dar acesso de ESCRITA a qualquer
     * pessoa sem passar pela tela de conceder — com o tutor autorizando na crença de que aquilo era
     * um especialista.
     */
    @Test
    @DisplayName("so se encaminha a quem tem credencial profissional ativa")
    void soParaProfissional() {
        Person vizinho = pessoa("Joao Sem Crmv");

        var paraOVizinho = ReferralRequestDTO.builder()
                .toPersonId(vizinho.getPersonId())
                .reason("Piora da claudicacao.")
                .scopes(Set.of(GrantScope.PRONTUARIO))
                .build();

        assertThatThrownBy(() -> referralService.encaminhar(code.getAnimalId(), paraOVizinho))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.NOT_A_PROFESSIONAL.getCode());
    }

    @Test
    @DisplayName("encaminhar a quem ja responde pelo animal e recusado")
    void naoEncaminhaParaQuemResponde() {
        credencial(marcelo, "clinica geral");

        var paraOTutor = ReferralRequestDTO.builder()
                .toPersonId(marcelo.getPersonId())
                .reason("Piora da claudicacao.")
                .scopes(Set.of(GrantScope.PRONTUARIO))
                .build();

        assertThatThrownBy(() -> referralService.encaminhar(code.getAnimalId(), paraOTutor))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.CANNOT_REFER_TO_HOLDER.getCode());
    }

    /**
     * Quem responde pelo animal encaminhando não pede autorização a si mesmo: o pedido nasce
     * autorizado e a concessão sai na hora. Apertar dois botões para o mesmo efeito seria teatro.
     */
    @Test
    @DisplayName("quem responde pelo animal encaminha e o pedido nasce autorizado")
    void oTutorEncaminhaEJaAutoriza() {
        credencial(marcelo, null);
        agirComo(marcelo, null);

        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        assertThat(pedido.getStatus()).isEqualTo(ReferralStatus.AUTORIZADO);
        assertThat(pedido.getDecidedByName()).isEqualTo("Marcelo Dias");
        assertThat(grantRepository.findVigenteDaPessoaNoAnimal(
                code.getAnimalId(), roberto.getPersonId(), LocalDateTime.now())).isPresent();
    }

    /**
     * O especialista vê que há um caso esperando decisão — pode ligar para a clínica que encaminhou —
     * e não vê de que animal se trata. O tutor não autorizou nada ainda.
     */
    @Test
    @DisplayName("a caixa do especialista mostra o pendente sem o animal, e o autorizado com ele")
    void aCaixaDoEspecialista() {
        var pedido = referralService.encaminhar(code.getAnimalId(), pedidoParaRoberto());

        agirComo(roberto, anhangabau);
        assertThat(referralService.recebidos()).singleElement()
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ReferralStatus.PENDENTE);
                    assertThat(r.getAnimalName()).isNull();
                    assertThat(r.getAnimalId()).isNull();
                    // o motivo VIAJA: e o que diz a ele o que esta sendo perguntado
                    assertThat(r.getReason()).contains("claudicacao");
                });

        agirComo(marcelo, null);
        referralService.autorizar(pedido.getReferralId());

        agirComo(roberto, anhangabau);
        assertThat(referralService.recebidos()).singleElement()
                .satisfies(r -> assertThat(r.getAnimalName()).isEqualTo("Code"));
    }

    @Test
    @DisplayName("as caixas do que vai junto vem com os sete escopos, quatro sugeridas")
    void asCaixasDoQueVaiJunto() {
        var opcoes = referralService.opcoes(code.getAnimalId());

        assertThat(opcoes.getDefaultAccessDays()).isEqualTo(90);
        assertThat(opcoes.getScopes()).hasSize(GrantScope.values().length);

        assertThat(opcoes.getScopes())
                .filteredOn(ReferralScopeOptionDTO::isSuggested)
                .extracting(ReferralScopeOptionDTO::getScope)
                .containsExactlyInAnyOrder(GrantScope.PRONTUARIO, GrantScope.ANEXOS,
                        GrantScope.PESO, GrantScope.OBSERVACOES);

        // CONTATO nao e um pedaco do prontuario, e o telefone do tutor: a tela precisa saber disso
        // para avisar antes do gesto
        assertThat(opcoes.getScopes())
                .filteredOn(c -> c.getScope() == GrantScope.CONTATO)
                .singleElement()
                .satisfies(c -> assertThat(c.isPersonalData()).isTrue());
    }

    /**
     * Só quem tem credencial ativa aparece, a parcial casa a especialidade, e eu não me acho.
     *
     * <b>Recorta pelo id do Roberto desta rodada, e não pelo tamanho da lista.</b> O container é o
     * mesmo para a classe e o banco não é limpo entre casos: cada {@code @BeforeEach} cria um
     * ortopedista novo, e uma asserção de "um único resultado" passaria no primeiro caso e falharia
     * nos catorze seguintes por uma razão que nada tem a ver com a regra sob teste.
     */
    @Test
    @DisplayName("a busca acha o ortopedista pela especialidade e nao devolve quem esta lendo")
    void aBuscaDeProfissional() {
        assertThat(referralService.candidatos(code.getAnimalId(), "orto"))
                .filteredOn(c -> c.getPersonId().equals(roberto.getPersonId()))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.getName()).isEqualTo("Roberto Lins");
                    assertThat(c.getSpecialty()).isEqualTo("ortopedia");
                    assertThat(c.getCredential()).isEqualTo("CRMV-SP " + crmvDoRoberto);
                    assertThat(c.getOrganizations()).containsExactly("Clinica Anhangabau");
                });

        // "Ana Ferreira" acharia a propria Ana, e oferecer o proprio nome apresentaria uma opcao que
        // o servidor rejeita
        assertThat(referralService.candidatos(code.getAnimalId(), "Ana Ferreira"))
                .extracting(c -> c.getPersonId())
                .doesNotContain(ana.getPersonId());

        // duas letras nao e buscar, e listar todo mundo
        assertThat(referralService.candidatos(code.getAnimalId(), "or")).isEmpty();
    }

    @Test
    @DisplayName("o suspenso nao aparece na busca")
    void oSuspensoNaoAparece() {
        credentialRepository.findByPersonPersonId(roberto.getPersonId()).forEach(c -> {
            c.setStatus(CredentialStatus.SUSPENSO);
            credentialRepository.saveAndFlush(c);
        });

        assertThat(referralService.candidatos(code.getAnimalId(), "orto"))
                .extracting(c -> c.getPersonId())
                .doesNotContain(roberto.getPersonId());
    }

    /** Os escopos que ficaram GRAVADOS na concessão vigente do Roberto neste animal. */
    private List<String> escoposDoRoberto() {
        return jdbcTemplate.queryForList(
                "select gs.scope from grant_scopes gs "
                        + "join grants g on g.grant_id = gs.grant_id "
                        + "where g.animal_id = ? and g.grantee_person_id = ? "
                        + "and g.revoked_at is null",
                String.class, code.getAnimalId(), roberto.getPersonId());
    }

    private ReferralRequestDTO pedidoParaRoberto() {
        return ReferralRequestDTO.builder()
                .toPersonId(roberto.getPersonId())
                .reason("Piora da claudicacao posterior esquerda nos ultimos 3 meses. "
                        + "Displasia leve diagnosticada em 2023. Peco avaliacao ortopedica.")
                .scopes(new LinkedHashSet<>(List.of(GrantScope.PRONTUARIO, GrantScope.ANEXOS,
                        GrantScope.PESO, GrantScope.OBSERVACOES)))
                .build();
    }

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome)
                .email(nome.split(" ")[0].toLowerCase() + "-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());
    }

    private Organization clinica(String nome) {
        return organizationRepository.saveAndFlush(Organization.builder()
                .name(nome)
                .capabilities(Set.of(OrganizationCapability.REGISTRAR_ATO_CLINICO))
                .creationDate(LocalDateTime.now())
                .build());
    }

    private void membro(Person quem, Organization onde) {
        membershipRepository.saveAndFlush(Membership.builder()
                .organization(onde).person(quem)
                .role(MembershipRole.VETERINARIO)
                .joinedAt(LocalDateTime.now())
                .build());
    }

    private String credencial(Person quem, String especialidade) {
        String numero = String.valueOf(
                Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000L));

        credentialRepository.saveAndFlush(ProfessionalCredential.builder()
                .person(quem)
                .council("CRMV").uf("SP").number(numero)
                .specialty(especialidade)
                .status(CredentialStatus.INFORMADO)
                .creationDate(LocalDateTime.now())
                .build());

        return numero;
    }

    private void concessao(Person paraQuem, Set<GrantScope> escopos, LocalDateTime ate) {
        grantRepository.saveAndFlush(Grant.builder()
                .animal(code)
                .granteePerson(paraQuem)
                .level(GrantLevel.EDITOR)
                .scopes(new LinkedHashSet<>(escopos))
                .grantedBy(marcelo)
                .grantedAt(LocalDateTime.now())
                .expiresAt(ate)
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

package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Membership;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.PetTutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A adocao: o abrigo responde pelo animal, convida quem adota, e o registro inteiro muda de mao.
 *
 * <b>Esta classe existe porque o domínio da adocao estava pronto e inalcancavel.</b> O
 * {@code Custody} aceita {@code holderOrganization} desde o P2 e o {@code CustodyEndReason} traz
 * {@code ADOCAO} com a documentacao do caso — "o abrigo cadastra o adotante, e e isso que encerra
 * a custodia dele". Nada disso tinha caminho HTTP: o {@code AnimalAccessGuard.requireCustodia} so
 * sabia perguntar por PESSOA, e o efeito era que nenhum membro do abrigo conseguia agir sobre o
 * animal do proprio abrigo.
 *
 * <b>E um teste de container, e nao de unidade, por dois motivos.</b> O primeiro e o indice unico
 * parcial de "uma custodia em curso por animal": a troca de mao encerra uma e abre outra na ordem
 * certa, e mock nao tem indice — foi assim que a transferencia quebrou no 8b. O segundo e que a
 * custodia de organizacao e uma coluna exclusiva com a de pessoa, e o comportamento das duas so
 * aparece contra o banco de verdade.
 */
@SpringBootTest
@DisplayName("adocao: do abrigo para quem adota, contra Postgres real")
class AdocaoContainerTest extends PostgresContainerTest {

    @Autowired private PetTutorService petTutorService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private GrantRepository grantRepository;

    private Organization abrigo;
    private Person claudia;
    private Person paula;
    private Animal teco;

    @BeforeEach
    void montarOAbrigo() {
        abrigo = organizationRepository.saveAndFlush(Organization.builder()
                .name("Abrigo Lar dos Focinhos")
                .creationDate(LocalDateTime.now())
                .build());

        claudia = personRepository.saveAndFlush(Person.builder()
                .name("Claudia Menezes")
                .email("claudia-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        paula = personRepository.saveAndFlush(Person.builder()
                .name("Paula Rezende")
                .email("paula-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(abrigo)
                .person(claudia)
                .role(MembershipRole.ADMINISTRADOR)
                .joinedAt(LocalDateTime.now())
                .build());

        teco = animalRepository.saveAndFlush(Animal.builder()
                .name("Teco").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        // O ANIMAL SEM TUTOR HUMANO: quem responde e o abrigo, e e este o estado que a
        // guarda nao sabia ler.
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(teco)
                .holderOrganization(abrigo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusDays(8))
                .build());

        agirComo(claudia, abrigo);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * <b>Conferido ao contrario:</b> sem a custodia de organizacao na guarda, este teste falha em
     * {@code invite} com 404 — o abrigo nao conseguia nem comecar a adocao do proprio animal.
     */
    @Test
    @DisplayName("o abrigo convida quem adota, e a custodia encerra como ADOCAO")
    void adocaoTrocaAMaoEGravaOMotivo() {
        var convite = petTutorService.invite(teco.getAnimalId(), PetTutorInviteRequestDTO.builder()
                .email(paula.getEmail())
                .role(PetTutorRole.HOLDER)
                .build());

        assertThat(convite.getToken()).isNotBlank();

        // Quem aceita e Paula, e nao o abrigo.
        agirComo(paula, null);
        petTutorService.accept(convite.getToken());

        List<Custody> historia = custodyRepository.findByAnimalAnimalIdOrderByStartedAtAsc(teco.getAnimalId());

        Custody doAbrigo = historia.get(0);
        assertThat(doAbrigo.getEndedAt()).isNotNull();
        assertThat(doAbrigo.getEndReason())
                .as("o enum documenta este caso, e gravar TRANSFERENCIA apagaria o que aconteceu")
                .isEqualTo(CustodyEndReason.ADOCAO);

        Custody daPaula = historia.get(historia.size() - 1);
        assertThat(daPaula.getEndedAt()).isNull();
        assertThat(daPaula.getHolderPerson().getPersonId()).isEqualTo(paula.getPersonId());
        assertThat(daPaula.getHolderOrganization()).isNull();
    }

    /**
     * "A partir do aceite, o abrigo passa a LER o que registrou, e nao decide mais nada sobre o
     * Teco" — a frase e da Tela 13, e este teste e o que a torna verdade.
     */
    @Test
    @DisplayName("depois da adocao o abrigo fica com leitura, e nao com escrita")
    void oAbrigoFicaLendo() {
        var convite = petTutorService.invite(teco.getAnimalId(), PetTutorInviteRequestDTO.builder()
                .email(paula.getEmail())
                .role(PetTutorRole.HOLDER)
                .build());

        agirComo(paula, null);
        petTutorService.accept(convite.getToken());

        List<Grant> concessoes = grantRepository.findByAnimalAnimalIdOrderByGrantedAtDesc(teco.getAnimalId());

        Grant doAbrigo = concessoes.stream()
                .filter(g -> g.getGranteeOrganization() != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o abrigo deveria continuar vendo o que registrou"));

        assertThat(doAbrigo.getLevel())
                .as("EDITOR deixaria o abrigo mandando no animal de quem adotou")
                .isEqualTo(GrantLevel.VIEWER);
        assertThat(doAbrigo.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("a lista de sob-custodia devolve o animal do abrigo, e nao os de concessao")
    void aListaDoAbrigo() {
        var pagina = custodyRepository.buscarEmCursoDaOrganizacao(
                abrigo.getOrganizationId(), "%", PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getAnimal().getName()).isEqualTo("Teco");
    }

    @Test
    @DisplayName("a busca da lista acha por nome e ignora quem nao casa")
    void aBuscaDaLista() {
        assertThat(custodyRepository
                .buscarEmCursoDaOrganizacao(abrigo.getOrganizationId(), "%tec%", PageRequest.of(0, 20))
                .getTotalElements()).isEqualTo(1);

        assertThat(custodyRepository
                .buscarEmCursoDaOrganizacao(abrigo.getOrganizationId(), "%nina%", PageRequest.of(0, 20))
                .getTotalElements()).isZero();
    }

    /**
     * O contexto ativo vem do cabecalho, e nao de inferencia — entao o teste precisa por o
     * cabecalho como o filtro poria. Sem isso, agir "pelo abrigo" seria o servidor adivinhando.
     */
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

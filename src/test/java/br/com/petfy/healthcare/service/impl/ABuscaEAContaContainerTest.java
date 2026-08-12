package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.ProfessionalCredentialRequestDTO;
import br.com.petfy.healthcare.domain.entity.*;
import br.com.petfy.healthcare.domain.repository.*;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentProfessionalProvider;
import br.com.petfy.healthcare.service.AnimalSearchService;
import br.com.petfy.healthcare.service.PersonService;
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
 * A busca e a conta, contra Postgres real (Telas 35 e 36).
 *
 * <b>Duas telas que quase não pediram schema</b> — a busca é uma consulta sobre o que já existe, e a
 * conta reúne rotas do P1 e do P4. O que elas mudaram de verdade foi uma regra: **ninguém sai do Petfy
 * deixando um animal sem quem responda por ele.**
 *
 * <b>Container, e não unidade, porque o que importa aqui é o RECORTE.</b> Quem aparece na busca e
 * quem não aparece depende de custódia e concessão reais; um mock diria que o serviço chamou o
 * repositório, e a pergunta é se um estranho aparece na lista de alguém.
 */
@SpringBootTest
@DisplayName("a busca e a conta, contra Postgres real")
class ABuscaEAContaContainerTest extends PostgresContainerTest {

    @Autowired private AnimalSearchService buscaService;
    @Autowired private PersonService personService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private MembershipRepository membershipRepository;

    private Person marcelo;
    private Person ana;
    private Organization clinica;
    private Animal code;
    private Animal amora;
    private Animal deNinguem;

    private String microchipDoCode;

    @BeforeEach
    void montar() {
        marcelo = pessoa("Marcelo Dias");
        ana = pessoa("Ana Ferreira");

        clinica = organizationRepository.saveAndFlush(Organization.builder()
                .name("Clinica Vet Norte")
                .capabilities(Set.of(OrganizationCapability.REGISTRAR_ATO_CLINICO))
                .creationDate(LocalDateTime.now())
                .build());

        membershipRepository.saveAndFlush(Membership.builder()
                .organization(clinica).person(ana)
                .role(MembershipRole.VETERINARIO)
                .joinedAt(LocalDateTime.now())
                .build());

        // o microchip e sorteado: o banco de teste nao e limpo entre casos, e um numero fixo faria
        // a busca do caso seguinte achar o animal do caso anterior
        microchipDoCode = "9810" + Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000L);

        code = animal("Code", microchipDoCode);
        amora = animal("Amora", microchipDoCode.replace("9810", "9811"));
        deNinguem = animal("Bartolomeu", microchipDoCode.replace("9810", "9812"));

        // o Code e do Marcelo; a Amora e de outra pessoa e a clinica alcanca
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(marcelo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(7))
                .build());

        Person ricardo = pessoa("Ricardo Alves");

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(amora).holderPerson(ricardo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now().minusYears(2))
                .build());

        grantRepository.saveAndFlush(Grant.builder()
                .animal(amora)
                .granteeOrganization(clinica)
                .level(GrantLevel.EDITOR)
                .scopes(new LinkedHashSet<>(List.of(GrantScope.PRONTUARIO)))
                .grantedBy(ricardo)
                .grantedAt(LocalDateTime.now())
                .build());

        // e o Bartolomeu nao e alcancado por ninguem deste teste
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(deNinguem).holderPerson(pessoa("Estranho Qualquer"))
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now())
                .build());

        agirComo(marcelo, null);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    /** "Seus animais" e "Animais que você alcança pela Clínica Vet Norte" são grupos diferentes. */
    @Test
    @DisplayName("a busca separa o que voce responde do que voce alcanca pela organizacao")
    void osDoisGrupos() {
        agirComo(ana, clinica);

        var resultado = buscaService.buscar("Amora");

        assertThat(resultado.getMine())
                .as("um animal que a clinica alcanca apareceu como se fosse da veterinaria")
                .noneSatisfy(a -> assertThat(a.getAnimalId()).isEqualTo(amora.getAnimalId()));

        assertThat(resultado.getThroughOrganization())
                .filteredOn(a -> a.getAnimalId().equals(amora.getAnimalId()))
                .singleElement()
                .satisfies(a -> {
                    // "Amora · tutor Ricardo Alves"
                    assertThat(a.getHolderName()).isEqualTo("Ricardo Alves");
                    assertThat(a.getName()).isEqualTo("Amora");
                });

        assertThat(resultado.getOrganizationName()).isEqualTo("Clinica Vet Norte");
    }

    /** Os três campos numa consulta só, porque quem digita não sabe em qual está digitando. */
    @Test
    @DisplayName("acha por nome e por microchip parcial")
    void achaPorNomeEPorMicrochip() {
        assertThat(buscaService.buscar("Code").getMine())
                .filteredOn(a -> a.getAnimalId().equals(code.getAnimalId()))
                .hasSize(1);

        assertThat(buscaService.buscar(microchipDoCode.substring(0, 8)).getMine())
                .filteredOn(a -> a.getAnimalId().equals(code.getAnimalId()))
                .hasSize(1);
    }

    /**
     * <b>A frase mais incomum da tela.</b> "Existem outros animais com microchip começando em 9810 no
     * Petfy. Você não tem acesso a eles, e por isso não aparecem aqui."
     *
     * Calar sobre o resto faria a pessoa concluir que o animal que ela procura não está no produto.
     */
    @Test
    @DisplayName("diz que existem outros que voce nao alcanca, sem mostra-los")
    void oQueVoceNaoAlcanca() {
        var resultado = buscaService.buscar("Bartolomeu");

        assertThat(resultado.getMine()).isEmpty();
        assertThat(resultado.getThroughOrganization()).isEmpty();
        assertThat(resultado.isOthersExist())
                .as("a busca calou sobre um animal que existe e que a pessoa nao alcanca")
                .isTrue();
    }

    /** E quando não há mais nada, ela também diz a verdade: não há. */
    @Test
    @DisplayName("sem nada que case, nao inventa que existe outro")
    void quandoNaoHaNada() {
        var resultado = buscaService.buscar("zzzznaoexistezzzz");

        assertThat(resultado.getMine()).isEmpty();
        assertThat(resultado.isOthersExist()).isFalse();
    }

    @Test
    @DisplayName("buscar com duas letras e recusado")
    void buscaCurta() {
        assertThatThrownBy(() -> buscaService.buscar("Co"))
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.SEARCH_TERM_TOO_SHORT.getCode());
    }

    /**
     * <b>A mudança de contrato do bloco.</b> "O Code e o Bartolomeu precisam de alguém antes que você
     * saia." Até aqui, encerrar a conta matava o animal sem outro tutor — em silêncio.
     */
    @Test
    @DisplayName("nao encerra a conta com animal sob a responsabilidade")
    void naoEncerraComAnimalSobResponsabilidade() {
        var conta = personService.conta();

        assertThat(conta.isCanDeleteAccount()).isFalse();
        assertThat(conta.getAnimalsUnderMyResponsibility())
                .extracting(a -> a.getName())
                .contains("Code");

        assertThatThrownBy(() -> personService.deleteCurrentPerson())
                .isInstanceOf(PetfyHealthcareException.class)
                .extracting(e -> ((PetfyHealthcareException) e).getCode())
                .isEqualTo(ErrorMessageEnum.ANIMALS_STILL_UNDER_YOUR_RESPONSIBILITY.getCode());

        // e o animal continua la, inteiro
        assertThat(animalRepository.findById(code.getAnimalId())).isPresent();
    }

    /** Dado destino ao animal, a conta encerra — o caminho de saída existe e não é novo. */
    @Test
    @DisplayName("sem animal sob responsabilidade, a conta encerra")
    void encerraQuandoNaoHaAnimal() {
        Person semAnimais = pessoa("Sozinha Silva");
        agirComo(semAnimais, null);

        assertThat(personService.conta().isCanDeleteAccount()).isTrue();

        personService.deleteCurrentPerson();

        assertThat(personRepository.findById(semAnimais.getPersonId())).isEmpty();
    }

    /**
     * "Registro profissional · Você não declarou nenhum. Sem ele, você não registra diagnóstico nem
     * prescrição."
     */
    @Test
    @DisplayName("declara o registro profissional depois do cadastro")
    void declaraCredencialDepois() {
        assertThat(personService.conta().getProfessionalCredential()).isNull();

        String numero = String.valueOf(
                Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000L));

        var conta = personService.declararCredencial(ProfessionalCredentialRequestDTO.builder()
                .crmv(numero).uf("sp").specialty("ortopedia")
                .build());

        // a UF sobe para maiuscula: o CRMV e estadual, e "sp" e "SP" sao o mesmo registro
        assertThat(conta.getProfessionalCredential()).isEqualTo("CRMV-SP " + numero);
    }

    /** "Senha · alterada em 02/2024" — e nulo significa "nunca desde que a conta existe". */
    @Test
    @DisplayName("a conta nasce sem data de troca de senha")
    void semTrocaDeSenhaAinda() {
        assertThat(personService.conta().getPasswordChangedAt()).isNull();
    }

    private Animal animal(String nome, String microchip) {
        return animalRepository.saveAndFlush(Animal.builder()
                .name(nome).species(Species.CANINA)
                .microchipNumber(microchip)
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

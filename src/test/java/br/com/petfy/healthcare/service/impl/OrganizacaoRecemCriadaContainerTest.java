package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.ActiveContextResponseDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.MembershipRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.service.ActiveContextService;
import br.com.petfy.healthcare.service.OrganizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criar uma organizacao e sair de la conseguindo agir em nome dela, contra Postgres real.
 *
 * <b>Este teste existe por causa de uma organizacao que nascia inalcancavel.</b> O
 * {@code createOrganization} salvava a {@link br.com.petfy.healthcare.domain.entity.Organization}
 * e nao gravava {@code Membership} nenhum. Como o contexto ativo lista organizacoes por
 * {@code findAtivosDaPessoa}, quem criava nao encontrava a propria organizacao no "Agindo como" —
 * e o produto levava a pessoa direto para a equipe dela, que abria com "esta acao e da
 * organizacao, escolha em nome de qual esta agindo" sobre uma lista onde ela nao estava. Nao
 * havia conserto pela interface.
 *
 * <b>Por que nenhum teste via:</b> os de unidade montam o repositorio com mock e afirmam sobre o
 * que o servico chamou, e ninguem tinha escrito que ele deveria chamar o {@code MembershipRepository} —
 * um teste de mock nao acusa a ausencia de uma chamada que ninguem esperava. E a unica tela que
 * criava organizacao nasceu depois do servico. E a mesma familia do {@code viewSharedCard}: o
 * defeito mora no caminho que nenhuma tela percorria.
 *
 * <b>Foi conferido que este teste PEGA:</b> removendo o {@code membershipRepository.save(...)} do
 * servico, os tres casos abaixo falham — o contexto volta a nao listar a organizacao.
 */
@SpringBootTest
@DisplayName("a organizacao recem-criada, contra Postgres real")
class OrganizacaoRecemCriadaContainerTest extends PostgresContainerTest {

    @Autowired private OrganizationService organizationService;
    @Autowired private ActiveContextService activeContextService;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private PersonRepository personRepository;

    private Person quemCria;

    @BeforeEach
    void montar() {
        /*
         * Sem CRMV de proposito: quem cria a creche nao e veterinario, e o servico nao pode
         * exigir credencial para deixar criar. E o `CurrentProfessionalProvider.require()`
         * responderia 403 aqui — e por isso que o servico usa o provider de pessoa.
         */
        quemCria = personRepository.saveAndFlush(Person.builder()
                .name("Dona da creche")
                .email("creche-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .phone("11999998888")
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(quemCria.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private UUID criar(String nome) {
        return organizationService.createOrganization(
                OrganizationRequestDTO.builder().name(nome).build()).getOrganizationId();
    }

    /** O caso que quebrava a tela: criar e nao se achar dentro. */
    @Test
    @DisplayName("quem cria sai com vinculo ativo de ADMINISTRADOR")
    void quemCriaSaiComVinculo() {
        UUID organizationId = criar("Creche Quintal");

        var vinculos = membershipRepository.findAtivosDaPessoa(quemCria.getPersonId());

        assertThat(vinculos)
                .filteredOn(vinculo -> vinculo.getOrganization().getOrganizationId().equals(organizationId))
                .singleElement()
                .satisfies(vinculo -> {
                    assertThat(vinculo.getRole()).isEqualTo(MembershipRole.ADMINISTRADOR);
                    assertThat(vinculo.estaAtivo()).isTrue();
                });
    }

    /**
     * A pergunta que a tela faz, e que respondia errado.
     *
     * <b>Contagem nao serve aqui:</b> o banco do teste nao e limpo entre casos, entao a assercao
     * e sobre ESTA organizacao dentro da lista, e o nome dela e sorteado para nao colidir com o
     * de outro caso.
     */
    @Test
    @DisplayName("o contexto ativo passa a oferecer a organizacao no 'Agindo como'")
    void oContextoOferece() {
        String nome = "Creche " + UUID.randomUUID();
        UUID organizationId = criar(nome);

        ActiveContextResponseDTO contexto = activeContextService.contextoAtivo(null);

        assertThat(contexto.getAvailable())
                .anySatisfy(opcao -> {
                    assertThat(opcao.getOrganizationId()).isEqualTo(organizationId);
                    assertThat(opcao.getOrganizationName()).isEqualTo(nome);
                });
    }

    /**
     * Declarar a organizacao no header e agir em nome dela — o que a tela de equipe faz.
     *
     * Sem o vinculo, esta chamada caia no erro que apareceu no navegador: "esta acao e da
     * organizacao. Escolha em nome de qual esta agindo."
     */
    @Test
    @DisplayName("declarar a organizacao recem-criada resolve o contexto para ela")
    void declararResolve() {
        UUID organizationId = criar("Clinica Vet Norte");

        ActiveContextResponseDTO contexto = activeContextService.contextoAtivo(organizationId.toString());

        assertThat(contexto.getActive()).isNotNull();
        assertThat(contexto.getActive().getOrganizationId()).isEqualTo(organizationId);
        assertThat(contexto.getActive().getRole()).isEqualTo(MembershipRole.ADMINISTRADOR);
    }
}

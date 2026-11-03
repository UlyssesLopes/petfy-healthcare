package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.CareNetworkMemberDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.service.CareNetworkService;
import br.com.petfy.healthcare.service.OrganizationAccessService;
import br.com.petfy.healthcare.service.OrganizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rede de quem cuida, com uma organizacao que recebeu acesso. O controller serializa a
 * resposta depois que a transacao fecha; antes do conserto, os escopos da concessao ainda eram
 * a colecao preguicosa do Hibernate, e a tela do animal respondia 500.
 */
@SpringBootTest
@DisplayName("a rede de cuidado, contra Postgres real")
class RedeDeCuidadoContainerTest extends PostgresContainerTest {

    @Autowired private CareNetworkService careNetworkService;
    @Autowired private OrganizationAccessService organizationAccessService;
    @Autowired private OrganizationService organizationService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private ObjectMapper objectMapper;

    private Animal pipoca;

    private Person pessoa(String nome) {
        return personRepository.saveAndFlush(Person.builder()
                .name(nome)
                .email("rede-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .phone("11999998888")
                .build());
    }

    private void comoQuem(Person pessoa) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));
    }

    @BeforeEach
    void montar() {
        Person joana = pessoa("Joana Lima");
        comoQuem(joana);
        UUID creche = organizationService.createOrganization(
                OrganizationRequestDTO.builder().name("Creche Quintal").build()).getOrganizationId();

        Person ana = pessoa("Ana Ribeiro");
        pipoca = animalRepository.saveAndFlush(Animal.builder()
                .name("Pipoca").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
        custodyRepository.saveAndFlush(Custody.builder()
                .animal(pipoca).holderPerson(ana).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        comoQuem(ana);
        organizationAccessService.grant(pipoca.getAnimalId(), OrganizationAccessRequestDTO.builder()
                .organizationId(creche)
                .scopes(new LinkedHashSet<>(List.of(GrantScope.CARTEIRA, GrantScope.OBSERVACOES)))
                .expiresAt(LocalDateTime.now().plusMonths(3))
                .build());
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a rede vira JSON depois da transacao, com os escopos da creche")
    void viraJson() throws Exception {
        List<CareNetworkMemberDTO> rede = careNetworkService.doAnimal(pipoca.getAnimalId());

        String json = objectMapper.writeValueAsString(rede);

        assertThat(json).contains("Creche Quintal").contains("CARTEIRA").contains("OBSERVACOES");
    }
}

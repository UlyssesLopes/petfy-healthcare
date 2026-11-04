package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.OrganizationRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationResponseDTO;
import br.com.petfy.healthcare.domain.entity.CredentialStatus;
import br.com.petfy.healthcare.domain.entity.OrganizationCapability;
import br.com.petfy.healthcare.domain.entity.ProfessionalCredential;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.service.OrganizationService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O contexto profissional e montado fora de transacao, como numa requisicao de verdade, e
 * precisa responder "esta organizacao pode?" sem sessao aberta. Antes do conserto, a pergunta
 * estourava LazyInitializationException e toda vacina registrada em nome de clinica dava 500.
 */
@SpringBootTest
@DisplayName("o contexto profissional, contra Postgres real")
class ContextoProfissionalContainerTest extends PostgresContainerTest {

    @Autowired private CurrentProfessionalProvider currentProfessionalProvider;
    @Autowired private OrganizationService organizationService;
    @Autowired private PersonRepository personRepository;
    @Autowired private ProfessionalCredentialRepository credentialRepository;

    private UUID clinica;

    @BeforeEach
    void montar() {
        Person luiza = personRepository.saveAndFlush(Person.builder()
                .name("Luiza Prado")
                .email("contexto-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .phone("11999998888")
                .build());

        credentialRepository.saveAndFlush(ProfessionalCredential.builder()
                .person(luiza).council("CRMV").uf("SP").number(String.valueOf(10000 + new java.util.Random().nextInt(89999)))
                .status(CredentialStatus.INFORMADO).creationDate(java.time.LocalDateTime.now())
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(luiza.getEmail(), "n/a", List.of()));

        OrganizationResponseDTO criada = organizationService.createOrganization(
                OrganizationRequestDTO.builder().name("Clinica Bom Pasto").build());
        clinica = criada.getOrganizationId();
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void comHeader(String organizacao) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        if (organizacao != null) {
            requisicao.addHeader("X-Petfy-Organization", organizacao);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicao));
    }

    @Test
    @DisplayName("com o header da organizacao, a capacidade e lida sem sessao aberta")
    void comHeader() {
        comHeader(clinica.toString());

        ProfessionalContext contexto = currentProfessionalProvider.requireContext();

        assertThat(contexto.permite(OrganizationCapability.REGISTRAR_ATO_CLINICO)).isTrue();
    }

    @Test
    @DisplayName("sem header e com um vinculo so, tambem")
    void semHeader() {
        comHeader(null);

        ProfessionalContext contexto = currentProfessionalProvider.requireContext();

        assertThat(contexto.permite(OrganizationCapability.REGISTRAR_ATO_CLINICO)).isTrue();
    }
}

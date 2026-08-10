package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Grant;
import br.com.petfy.healthcare.domain.entity.GrantLevel;
import br.com.petfy.healthcare.domain.entity.GrantScope;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.GrantRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.service.DueItemService;
import br.com.petfy.healthcare.service.OrganizationAccessService;
import br.com.petfy.healthcare.service.SensitiveAccessLogService;
import br.com.petfy.healthcare.service.VaccineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * As leituras que montam DTO atravessando associacao preguicosa, chamadas SEM transacao
 * em volta — exatamente como o controller as chama.
 *
 * <b>Esta classe existe porque o mesmo defeito apareceu tres vezes.</b> O projeto roda com
 * {@code spring.jpa.open-in-view=false}, que e decisao e nao descuido: sessao aberta na
 * view esconde consulta N+1 no controller. O preco e que todo metodo de leitura que
 * atravessa uma associacao precisa abrir a propria transacao — e esquecer disso nao quebra
 * teste de unidade, nao quebra compilacao e nao aparece no contrato. Aparece como 500 na
 * cara de quem usa, e so quando existe dado.
 *
 * Os tres casos ja pagos:
 * <ul>
 *   <li>o feed ({@code DueItemServiceImpl}) — corrigido antes desta classe existir;</li>
 *   <li>a agenda de vacinas — 500 para todo tutor com uma vacina registrada;</li>
 *   <li>a lista de acessos — 500 para todo animal com acesso concedido.</li>
 * </ul>
 *
 * <b>Por que esta classe NAO e {@code @Transactional}, ao contrario das vizinhas.</b> A
 * anotacao manteria a sessao aberta durante a chamada e todos os casos passariam com os
 * bugs de volta. O cenario e montado e comitado antes; as chamadas acontecem fora.
 *
 * <b>Como estender:</b> ao criar uma leitura que monte DTO a partir de entidade, chame-a
 * aqui. Uma linha por leitura — e a diferenca entre descobrir no teste e descobrir na
 * tela.
 */
@SpringBootTest
@DisplayName("leituras fora de transacao, contra Postgres real")
class LeituraForaDeTransacaoContainerTest extends PostgresContainerTest {

    @Autowired private VaccineService vaccineService;
    @Autowired private OrganizationAccessService organizationAccessService;
    @Autowired private SensitiveAccessLogService sensitiveAccessLogService;
    @Autowired private DueItemService dueItemService;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private GrantRepository grantRepository;
    @Autowired private VaccineRepository vaccineRepository;

    private Animal code;

    @BeforeEach
    void montarOCenario() {
        Person tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("leitura-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(tutor).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        Organization clinica = organizationRepository.saveAndFlush(Organization.builder()
                .name("Clinica Vet Norte")
                .creationDate(LocalDateTime.now())
                .build());

        // O acesso concedido e o que fazia a lista responder 500: sem ele a rota devolvia
        // lista vazia e o defeito ficava escondido.
        grantRepository.saveAndFlush(Grant.builder()
                .animal(code)
                .granteeOrganization(clinica)
                .level(GrantLevel.EDITOR)
                .grantedBy(tutor)
                .grantedAt(LocalDateTime.now())
                .scopes(Set.of(GrantScope.CARTEIRA, GrantScope.CONDICOES))
                .build());

        // Idem para a agenda: sem uma vacina registrada ela devolvia zero itens.
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code)
                .vaccineName("Antirrabica canina")
                .applicationDate(LocalDate.now().minusYears(2))
                .nextDoseDate(LocalDate.now().minusDays(23))
                .creationDate(LocalDateTime.now())
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tutor.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    /**
     * <b>Ler nao basta: e preciso SERIALIZAR.</b>
     *
     * Este metodo existe por causa de um defeito que a primeira versao desta classe deixou
     * passar. O {@code scopes} do Grant e uma {@code @ElementCollection} preguicosa, e o
     * DTO recebia a referencia dela em vez de uma copia — a leitura funcionava, e a falha
     * so aparecia quando o Jackson escrevia a resposta, ja fora da transacao, como
     * {@code HttpMessageNotWritableException}.
     *
     * Chamar o servico e verificar que nao lanca cobre metade do caminho. A outra metade e
     * o que o controller faz depois, e e aqui.
     */
    private void leSemQuebrar(ThrowingSupplier<Object> leitura) {
        assertThatCode(() -> objectMapper.writeValueAsString(leitura.get()))
                .doesNotThrowAnyException();
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @Test
    @DisplayName("a agenda de vacinas atravessa leitura e serializacao")
    void agendaDeVacinas() {
        leSemQuebrar(() -> vaccineService.getAgenda(30));
    }

    @Test
    @DisplayName("a lista de acessos atravessa leitura e serializacao")
    void listaDeAcessos() {
        leSemQuebrar(() -> organizationAccessService.list(code.getAnimalId()));
    }

    @Test
    @DisplayName("o registro de leituras sensiveis atravessa leitura e serializacao")
    void registroDeLeituras() {
        leSemQuebrar(() -> sensitiveAccessLogService.listByAnimal(code.getAnimalId(), PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("o feed de pendencias atravessa leitura e serializacao")
    void feedDePendencias() {
        leSemQuebrar(() -> dueItemService.doAutenticado(30, false));
    }
}

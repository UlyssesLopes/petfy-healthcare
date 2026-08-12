package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AnimalDeathRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.ClassGroup;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyEndReason;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Enrollment;
import br.com.petfy.healthcare.domain.entity.EnrollmentStatus;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.TimelineEventType;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.ClassGroupRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.EnrollmentRepository;
import br.com.petfy.healthcare.domain.repository.OrganizationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.TimelineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.AnimalDeathService;
import br.com.petfy.healthcare.service.AnimalService;
import br.com.petfy.healthcare.service.TimelineService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O fim da linha do tempo, contra Postgres real (Tela 33).
 *
 * <b>Container, e nao unidade, por tres coisas que mock nao tem.</b>
 *
 * A primeira e o <b>indice unico parcial</b> de "uma custodia em curso por animal": encerrar por
 * obito nao abre sucessora, e e o unico fluxo do produto que deixa o animal SEM custodia em curso.
 * Mock nao tem indice, e foi assim que a troca de titularidade quebrou no 8b.
 *
 * A segunda e a <b>view {@code animal_timeline}</b>: o evento de obito nao e uma tabela, e a linha
 * do obito lida pela view. Um erro no SQL da V40 nao aparece em teste de servico — aparece como um
 * evento que simplesmente nao existe.
 *
 * A terceira e a <b>leitura postuma</b>: o {@code AnimalAccessGuard} passou a deixar passar quem
 * NAO tem custodia em curso nem concessao nenhuma, e a promessa que isso sustenta — "os sete anos
 * de vida dele continuam aqui" — so se confirma contra o banco.
 */
@SpringBootTest
@DisplayName("o fim da linha do tempo, contra Postgres real")
class FimDaLinhaContainerTest extends PostgresContainerTest {

    @Autowired private AnimalDeathService animalDeathService;
    @Autowired private AnimalService animalService;
    @Autowired private TimelineService timelineService;

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ClassGroupRepository classGroupRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private TimelineRepository timelineRepository;

    private Person marcelo;
    private Animal code;
    private Enrollment matricula;

    @BeforeEach
    void montar() {
        marcelo = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("marcelo-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code")
                .species(Species.CANINA)
                .bornDate(LocalDate.of(2019, 3, 14))
                .creationDate(LocalDateTime.now())
                .build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code)
                .holderPerson(marcelo)
                .nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.of(2019, 4, 1, 10, 0))
                .build());

        Organization creche = organizationRepository.saveAndFlush(Organization.builder()
                .name("Creche Quintal")
                .email("contato-" + UUID.randomUUID() + "@quintal.com.br")
                .creationDate(LocalDateTime.now())
                .build());

        ClassGroup turma = classGroupRepository.saveAndFlush(ClassGroup.builder()
                .organization(creche)
                .name("Turma da manha")
                .creationDate(LocalDateTime.now())
                .build());

        matricula = enrollmentRepository.saveAndFlush(Enrollment.builder()
                .animal(code)
                .classGroup(turma)
                .status(EnrollmentStatus.ATIVA)
                .requestedAt(LocalDateTime.now().minusMonths(3))
                .activatedAt(LocalDateTime.now().minusMonths(3))
                .build());

        agirComo(marcelo);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private AnimalDeathRequestDTO pedido() {
        return AnimalDeathRequestDTO.builder()
                .deceasedOn(LocalDate.now().minusDays(3))
                .place("Em casa")
                .farewellNote("Obrigado por sete anos, amigo.")
                .build();
    }

    /**
     * <b>O caso que so o banco prova:</b> o animal fica sem NENHUMA custodia em curso, e o indice
     * unico parcial admite isso. Nenhum outro fluxo do produto chega a este estado — todos os
     * outros encerram abrindo a sucessora na mesma transacao.
     */
    @Test
    @DisplayName("encerra a custodia sem sucessor, e o animal fica sem quem responda")
    void encerraSemSucessor() {
        animalDeathService.registrar(code.getAnimalId(), pedido());

        List<Custody> todas = custodyRepository.findByAnimalAnimalIdOrderByStartedAtAsc(code.getAnimalId());

        assertThat(todas).hasSize(1);
        assertThat(todas.get(0).getEndReason()).isEqualTo(CustodyEndReason.OBITO);
        assertThat(todas.get(0).getSuccessor()).isNull();
        assertThat(custodyRepository.findEmCurso(code.getAnimalId())).isEmpty();
    }

    @Test
    @DisplayName("a matricula viva e encerrada: a creche nao espera o animal na segunda")
    void encerraAMatricula() {
        animalDeathService.registrar(code.getAnimalId(), pedido());

        Enrollment depois = enrollmentRepository.findById(matricula.getEnrollmentId()).orElseThrow();

        assertThat(depois.getStatus()).isEqualTo(EnrollmentStatus.ENCERRADA);
        assertThat(depois.getEndedAt()).isNotNull();
        assertThat(enrollmentRepository.findVivasDoAnimal(code.getAnimalId())).isEmpty();
    }

    /**
     * O evento sai da VIEW, e nao de uma tabela de eventos. Um erro no bloco da V40 nao levanta
     * excecao nenhuma: ele faz o obito simplesmente nao aparecer na vida do animal.
     */
    @Test
    @DisplayName("o obito entra na linha do tempo, com a data informada e nao a de hoje")
    void obitoNaLinhaDoTempo() {
        animalDeathService.registrar(code.getAnimalId(), pedido());

        var pagina = timelineService.doAnimal(code.getAnimalId(), PageRequest.of(0, 20));

        var obito = pagina.getContent().stream()
                .filter(entrada -> entrada.getEventType() == TimelineEventType.OBITO)
                .findFirst()
                .orElseThrow(() -> new AssertionError("o obito nao apareceu na linha do tempo"));

        // `occurredAt` e a data que o tutor informou; `recordedAt` e quando ele conseguiu vir
        // preencher. A distancia entre os dois e o assunto da tela.
        assertThat(obito.getOccurredAt().toLocalDate()).isEqualTo(LocalDate.now().minusDays(3));
        assertThat(obito.getRecordedAt().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(obito.getSummary()).isEqualTo("Obrigado por sete anos, amigo.");
        // ato civil, e nao dado de saude — quem tem escopo restrito ve que a vida terminou
        assertThat(obito.isHealthData()).isFalse();
        // o tutor nao age por organizacao nenhuma ao fechar a linha do tempo
        assertThat(obito.getOrganizationName()).isNull();
        assertThat(obito.getRecordedByName()).isEqualTo("Marcelo Dias");
    }

    /**
     * A PROMESSA CENTRAL DA TELA, e a que mais facilmente se quebraria sem ninguem notar: "os sete
     * anos de vida dele continuam aqui, inteiros, para voce abrir quando quiser".
     *
     * Sem a excecao no guard, esta leitura responde 404 — a custodia acabou, obito nao tem
     * sucessor que conceda nada, e nao ha concessao nenhuma para consultar.
     */
    @Test
    @DisplayName("quem respondia continua lendo a vida do animal depois do fim")
    void continuaLendoDepoisDoFim() {
        animalDeathService.registrar(code.getAnimalId(), pedido());

        assertThat(animalService.getAnimalById(code.getAnimalId()).getName()).isEqualTo("Code");
        assertThat(timelineService.doAnimal(code.getAnimalId(), PageRequest.of(0, 20)).getContent())
                .isNotEmpty();

        var ficha = animalDeathService.daFichaFechada(code.getAnimalId());
        assertThat(ficha.getPlace()).isEqualTo("Em casa");
        assertThat(ficha.getHolderSince()).isEqualTo(LocalDateTime.of(2019, 4, 1, 10, 0));
        assertThat(ficha.getHolderUntil()).isNotNull();
    }

    @Test
    @DisplayName("o animal sai da lista de agora e entra em quem ja esteve com voce")
    void trocaDeLista() {
        assertThat(animalService.listAllAnimals(PageRequest.of(0, 20)).getContent())
                .extracting("name")
                .contains("Code");

        animalDeathService.registrar(code.getAnimalId(), pedido());

        assertThat(animalService.listAllAnimals(PageRequest.of(0, 20)).getContent())
                .extracting("name")
                .doesNotContain("Code");

        var anteriores = animalService.queJaEstiveramComigo(PageRequest.of(0, 20)).getContent();

        assertThat(anteriores).extracting("name").contains("Code");
        assertThat(anteriores).extracting("deceasedOn").contains(LocalDate.now().minusDays(3));
    }

    /**
     * A chave primaria e o animal, entao a segunda tentativa bateria em violacao de chave antes de
     * qualquer regra. O servico recusa antes, com 409 — e o que este caso protege e que a data da
     * PRIMEIRA vez continua de pe.
     */
    @Test
    @DisplayName("encerrar duas vezes e recusado, e a primeira data continua valendo")
    void naoEncerraDuasVezes() {
        animalDeathService.registrar(code.getAnimalId(), pedido());

        assertThatThrownBy(() -> animalDeathService.registrar(code.getAnimalId(),
                AnimalDeathRequestDTO.builder().deceasedOn(LocalDate.now()).build()))
                .isInstanceOf(PetfyHealthcareException.class);

        assertThat(animalDeathService.daFichaFechada(code.getAnimalId()).getDeceasedOn())
                .isEqualTo(LocalDate.now().minusDays(3));
    }

    /**
     * A contagem sai da view, e a view inclui o proprio obito. Sem o {@code saveAndFlush} no
     * servico, este numero voltaria zerado na resposta da propria criacao — e a ficha abriria
     * dizendo um evento a menos que a verdade.
     */
    @Test
    @DisplayName("a ficha ja conta o proprio obito na resposta da criacao")
    void contaOProprioObito() {
        var ficha = animalDeathService.registrar(code.getAnimalId(), pedido());

        assertThat(ficha.getEventCount()).isEqualTo(1L);
        assertThat(timelineRepository.tamanhoDe(code.getAnimalId()).getEventos()).isEqualTo(1L);
    }

    private void agirComo(Person pessoa) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(pessoa.getEmail(), "n/a", List.of()));

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
    }
}

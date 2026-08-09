package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.service.VaccineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A agenda de vacinas respondia <b>500 para todo tutor que tinha uma vacina</b>.
 *
 * O {@code toAgendaItem} le {@code vaccine.getAnimal().getName()}, e este projeto roda com
 * {@code spring.jpa.open-in-view=false} — de proposito: sessao aberta na view esconde
 * consulta N+1 no controller. Sem transacao no servico, a sessao fecha ao sair do
 * repositorio e o proxy do animal estoura {@code LazyInitializationException}.
 *
 * <b>Por que esta classe nao e {@code @Transactional}, ao contrario das vizinhas.</b> A
 * anotacao no teste manteria a sessao aberta durante a chamada e o defeito sumiria — o
 * teste passaria com o bug de volta. Aqui o servico e chamado como o controller chama:
 * sem transacao em volta, e cabendo a ele abrir a sua.
 *
 * <b>Por que mock nao serve.</b> Um {@code VaccineRepository} mockado devolve entidades
 * comuns, sem proxy e sem sessao — {@code getName()} funcionaria e a classe inteira
 * afirmaria que esta tudo bem.
 */
@SpringBootTest
@DisplayName("agenda de vacinas fora de transacao, contra Postgres real")
class VaccineAgendaContainerTest extends PostgresContainerTest {

    @Autowired private VaccineService vaccineService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private VaccineRepository vaccineRepository;

    private Animal code;

    @BeforeEach
    void setUp() {
        Person tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("agenda-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        code = animalRepository.saveAndFlush(Animal.builder()
                .name("Code").species(Species.CANINA).creationDate(LocalDateTime.now()).build());

        custodyRepository.saveAndFlush(Custody.builder()
                .animal(code).holderPerson(tutor).nature(CustodyNature.DEFINITIVA)
                .startedAt(LocalDateTime.now()).build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tutor.getEmail(), "n/a", List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a agenda carrega o nome do animal em vez de estourar o proxy")
    void agendaCarregaONomeDoAnimal() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code)
                .vaccineName("Antirrabica canina")
                .applicationDate(LocalDate.now().minusYears(2))
                .nextDoseDate(LocalDate.now().minusDays(23))
                .creationDate(LocalDateTime.now())
                .build());

        var agenda = vaccineService.getAgenda(30);

        assertThat(agenda.getItems()).hasSize(1);
        assertThat(agenda.getItems().get(0).getAnimalName()).isEqualTo("Code");
        assertThat(agenda.getItems().get(0).getStatus()).isEqualTo(VaccineStatus.OVERDUE);
        assertThat(agenda.getOverdueCount()).isEqualTo(1);
    }

    /**
     * A dose em dia nao entra em {@code items} — a lista e o que pede acao —, mas ela conta.
     * Este caso existe para o {@code toAgendaItem} rodar tambem sobre quem NAO e acionavel:
     * o proxy e lido para todos, e nao so para os que sobram no fim.
     */
    @Test
    @DisplayName("dose em dia entra na contagem sem entrar na lista de acionaveis")
    void doseEmDiaContaSemAcionar() {
        vaccineRepository.saveAndFlush(Vaccine.builder()
                .animal(code)
                .vaccineName("V10 (Polivalente canina)")
                .applicationDate(LocalDate.now())
                .nextDoseDate(LocalDate.now().plusYears(1))
                .creationDate(LocalDateTime.now())
                .build());

        var agenda = vaccineService.getAgenda(30);

        assertThat(agenda.getItems()).isEmpty();
        assertThat(agenda.getUpToDateCount()).isEqualTo(1);
    }
}

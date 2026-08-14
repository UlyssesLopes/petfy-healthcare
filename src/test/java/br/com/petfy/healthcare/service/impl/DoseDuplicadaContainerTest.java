package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.VaccineService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A recusa de dose duplicada, contra o banco de verdade.
 *
 * <b>Por que nao basta o teste da fabrica.</b> Ele mocka a consulta que procura a dose do dia —
 * ou seja, afirma sobre a regra ja tendo assumido a resposta do banco. O defeito que a Tela 14
 * descreve e "as duas gravacoes passam", e isso e uma afirmacao sobre GRAVAR: so a segunda
 * chamada real, com a primeira ja commitada, prova que a segunda nao entra.
 *
 * A classe nao e {@code @Transactional} pelo mesmo motivo da vizinha
 * {@link VaccineAgendaContainerTest}: o servico e chamado como o controller chama, e a primeira
 * gravacao precisa estar visivel para a segunda consulta.
 */
@SpringBootTest
@DisplayName("a dose duplicada, contra Postgres real")
class DoseDuplicadaContainerTest extends PostgresContainerTest {

    @Autowired private VaccineService vaccineService;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;
    @Autowired private VaccineRepository vaccineRepository;

    private Animal code;

    private static final LocalDate ONTEM = LocalDate.now().minusDays(1);

    @BeforeEach
    void montar() {
        Person tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("dose-" + UUID.randomUUID() + "@petfy.com.br")
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

    private VaccineRequestDTO dose(String nome, LocalDate data) {
        return VaccineRequestDTO.builder()
                .animalId(code.getAnimalId())
                .vaccineName(nome)
                .applicationDate(data)
                .build();
    }

    @Test
    @DisplayName("a segunda gravacao da mesma dose e recusada, e o historico fica com uma")
    void segundaGravacaoRecusada() {
        vaccineService.createVaccine(dose("Antirrabica canina", ONTEM));

        assertThatThrownBy(() -> vaccineService.createVaccine(dose("Antirrabica canina", ONTEM)))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> {
                    var erro = (PetfyHealthcareException) e;
                    assertThat(erro.getCode()).isEqualTo(149);
                    assertThat(erro.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(code.getAnimalId()))
                .hasSize(1);
    }

    /**
     * O reforco e o caso que uma regra por "mesma vacina" sem data barraria — e ele e a vida
     * normal da carteira.
     */
    @Test
    @DisplayName("a mesma vacina em outra data entra, porque e reforco")
    void reforcoEntra() {
        vaccineService.createVaccine(dose("Antirrabica canina", ONTEM));

        assertThatCode(() -> vaccineService.createVaccine(dose("Antirrabica canina", LocalDate.now())))
                .doesNotThrowAnyException();

        assertThat(vaccineRepository.findByAnimalAnimalIdOrderByApplicationDateDesc(code.getAnimalId()))
                .hasSize(2);
    }

    /**
     * A EDICAO PASSOU A PASSAR PELA GUARDA, e ate aqui nao passava.
     *
     * Estava escrito no {@code VaccineFactory} que a omissao era de proposito: mudar a data para
     * bater com outro registro seria raro, e deixaria rastro no log de correcao. <b>Rastro nao e
     * guarda</b> — a duplicata nascida de uma edicao dobra a dose no historico exatamente como a
     * nascida de duas gravacoes, e "da para descobrir depois lendo o log" nao e o que o produto
     * promete a quem le a carteira.
     */
    @Test
    @DisplayName("a edicao que move a data para cima de outra dose e recusada")
    void edicaoNaoCriaDuplicata() {
        var primeira = vaccineService.createVaccine(dose("Antirrabica canina", ONTEM));
        var segunda = vaccineService.createVaccine(dose("Antirrabica canina", LocalDate.now()));

        assertThatThrownBy(() -> vaccineService.updateVaccine(
                segunda.getVaccineId(),
                VaccineRequestDTO.builder().applicationDate(ONTEM).build()))
                .isInstanceOf(PetfyHealthcareException.class)
                .satisfies(e -> assertThat(((PetfyHealthcareException) e).getCode()).isEqualTo(149));

        // as duas continuam nas datas em que estavam
        assertThat(vaccineRepository.findById(segunda.getVaccineId()))
                .get()
                .satisfies(v -> assertThat(v.getApplicationDate()).isEqualTo(LocalDate.now()));

        /*
         * E NAO SOBROU CORRECAO NENHUMA. O `updateVaccine` nao e transacional: o log grava na hora,
         * e recusar depois dele deixaria uma correcao registrada para uma edicao que nunca
         * aconteceu. O rastro passaria a mentir, que e pior do que nao existir.
         */
        assertThat(vaccineService.listCorrections(segunda.getVaccineId())).isEmpty();
        assertThat(vaccineService.listCorrections(primeira.getVaccineId())).isEmpty();
    }

    /**
     * Sem excluir o proprio registro da busca, a guarda se acharia — e toda edicao de descricao
     * seria recusada por conflito com ela mesma.
     */
    @Test
    @DisplayName("a edicao que nao mexe na data passa, porque a guarda nao se acha")
    void edicaoQueNaoMexeNaDataPassa() {
        var vacina = vaccineService.createVaccine(dose("Antirrabica canina", ONTEM));

        assertThatCode(() -> vaccineService.updateVaccine(
                vacina.getVaccineId(),
                VaccineRequestDTO.builder().description("lote 42, pata traseira").build()))
                .doesNotThrowAnyException();

        assertThat(vaccineRepository.findById(vacina.getVaccineId()))
                .get()
                .satisfies(v -> {
                    assertThat(v.getDescription()).isEqualTo("lote 42, pata traseira");
                    assertThat(v.getApplicationDate()).isEqualTo(ONTEM);
                });
    }
}

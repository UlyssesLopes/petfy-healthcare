package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.dto.AnimalCostRequestDTO;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import br.com.petfy.healthcare.domain.entity.AnimalCostKind;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.Custody;
import br.com.petfy.healthcare.domain.entity.CustodyNature;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.repository.AnimalCostRepository;
import br.com.petfy.healthcare.domain.repository.AnimalRepository;
import br.com.petfy.healthcare.domain.repository.CareInstructionRepository;
import br.com.petfy.healthcare.domain.repository.CustodyRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import br.com.petfy.healthcare.service.AnimalCostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O remedio deixou de ser custo e nada mais.
 *
 * <b>A Tela 42 lancava "Remedio, R$ 90" e o produto guardava um numero na categoria SAUDE.</b> Quem
 * cuidasse do animal naquela semana nao tinha como saber que havia um comprimido as 8h, e a linha do
 * tempo nao mostrava tratamento nenhum. A V49 ligou os dois lados do mesmo fato.
 *
 * <b>Nao houve entidade nova</b>, e e o que torna isto barato: a `care_instructions` ja e
 * "prescricao, medicacao e tema de casa" desde o P3, e nao exige credencial — o tutor sempre pode
 * dizer "o Code esta tomando isto".
 */
@SpringBootTest
@DisplayName("o remedio e gasto e e tratamento, contra Postgres real")
class RemedioEhSaudeContainerTest extends PostgresContainerTest {

    @Autowired private AnimalCostService animalCostService;
    @Autowired private AnimalCostRepository animalCostRepository;
    @Autowired private CareInstructionRepository careInstructionRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CustodyRepository custodyRepository;

    private Animal code;
    private Person tutor;

    @BeforeEach
    void montar() {
        tutor = personRepository.saveAndFlush(Person.builder()
                .name("Marcelo Dias")
                .email("remedio-" + UUID.randomUUID() + "@petfy.com.br")
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

    private CareInstruction tratamento() {
        return careInstructionRepository.saveAndFlush(CareInstruction.builder()
                .animal(code)
                .recordedBy(tutor)
                .description("Meio comprimido de manha, com comida")
                .intervalDays(1)
                .startsOn(LocalDate.now())
                .endsOn(LocalDate.now().plusDays(21))
                .creationDate(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("a compra do remedio aponta para o tratamento, e a resposta devolve a ligacao")
    void aCompraApontaParaOTratamento() {
        CareInstruction tratamento = tratamento();

        var custo = animalCostService.lancar(code.getAnimalId(), AnimalCostRequestDTO.builder()
                .description("Remedio")
                .amount(new BigDecimal("90.00"))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.SAUDE)
                .sourceCareInstructionId(tratamento.getCareInstructionId())
                .build());

        assertThat(custo.getSourceCareInstructionId())
                .isEqualTo(tratamento.getCareInstructionId());

        assertThat(animalCostRepository.findById(custo.getAnimalCostId()))
                .get()
                .satisfies(c -> assertThat(c.getSourceCareInstructionId())
                        .isEqualTo(tratamento.getCareInstructionId()));
    }

    /**
     * ON DELETE SET NULL, e nao CASCADE.
     *
     * <b>O dinheiro saiu.</b> Se um dia uma orientacao for removida, o que a pessoa gastou continua
     * tendo acontecido — um custo que some da conta porque alguem mexeu no tratamento faria a soma
     * do ano mentir.
     */
    @Test
    @DisplayName("apagar o tratamento nao apaga o gasto: solta a ligacao e o custo fica")
    void apagarOTratamentoNaoApagaOGasto() {
        CareInstruction tratamento = tratamento();

        var custo = animalCostService.lancar(code.getAnimalId(), AnimalCostRequestDTO.builder()
                .description("Remedio")
                .amount(new BigDecimal("90.00"))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.SAUDE)
                .sourceCareInstructionId(tratamento.getCareInstructionId())
                .build());

        careInstructionRepository.deleteById(tratamento.getCareInstructionId());
        careInstructionRepository.flush();

        assertThat(animalCostRepository.findById(custo.getAnimalCostId()))
                .get()
                .satisfies(c -> {
                    assertThat(c.getAmount()).isEqualByComparingTo("90.00");
                    assertThat(c.getSourceCareInstructionId()).isNull();
                });
    }

    /**
     * Racao nao e tratamento, e o remedio de dose unica que ninguem quer acompanhar tambem nao.
     * Obrigar a ligacao transformaria os tres toques da Tela 42 num formulario — "quanto mais
     * campos, menos gente lanca, e menos verdadeiro fica o custo".
     */
    @Test
    @DisplayName("a compra sem tratamento continua entrando, e a ligacao fica nula")
    void semTratamentoContinuaEntrando() {
        var custo = animalCostService.lancar(code.getAnimalId(), AnimalCostRequestDTO.builder()
                .description("Racao")
                .amount(new BigDecimal("190.00"))
                .kind(AnimalCostKind.COMPRA)
                .category(AnimalCostCategory.ALIMENTACAO)
                .build());

        assertThat(custo.getSourceCareInstructionId()).isNull();
    }
}

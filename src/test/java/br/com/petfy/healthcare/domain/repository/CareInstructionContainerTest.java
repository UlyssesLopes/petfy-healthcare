package br.com.petfy.healthcare.domain.repository;

import br.com.petfy.healthcare.PostgresContainerTest;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.CareInstruction;
import br.com.petfy.healthcare.domain.entity.CareInstructionFulfillment;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.TimelineEntry;
import br.com.petfy.healthcare.domain.entity.TimelineEventType;
import br.com.petfy.healthcare.service.AnimalPurger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Orientacao e cumprimento contra Postgres de verdade.
 *
 * <b>Tres coisas aqui nao existem em codigo Java nenhum</b>, e por isso mock nao serve:
 * os dois CHECK da V28, os dois ramos novos da view {@code animal_timeline}, e a ordem
 * de delete que a chave estrangeira entre cumprimento e orientacao exige. Os tres so
 * aparecem quando o Flyway roda.
 *
 * <b>Sem {@code @Transactional} de proposito.</b> O purge tem de acontecer de verdade -
 * um rollback no fim esconderia justamente a violacao de FK que este teste procura, que
 * foi como as tres versoes anteriores da limpeza passaram verdes e quebraram em
 * producao.
 */
@SpringBootTest
@DisplayName("orientacao e cumprimento contra Postgres real")
class CareInstructionContainerTest extends PostgresContainerTest {

    @Autowired private PersonRepository personRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private CareInstructionRepository careInstructionRepository;
    @Autowired private CareInstructionFulfillmentRepository fulfillmentRepository;
    @Autowired private TimelineRepository timelineRepository;
    @Autowired private AnimalPurger animalPurger;

    private Person ulysses;
    private Animal rex;

    @BeforeEach
    void setUp() {
        ulysses = personRepository.saveAndFlush(Person.builder()
                .name("Ulysses")
                .email("orientacao-" + UUID.randomUUID() + "@petfy.com.br")
                .password("hash")
                .build());

        rex = animalRepository.saveAndFlush(Animal.builder()
                .name("Rex").species(Species.CANINA).creationDate(LocalDateTime.now()).build());
    }

    private CareInstruction.CareInstructionBuilder<?, ?> orientacao() {
        return CareInstruction.builder()
                .animal(rex)
                .recordedBy(ulysses)
                .description("Fenobarbital de 12 em 12 horas")
                .intervalDays(1)
                .startsOn(LocalDate.now().minusDays(5))
                .creationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("o que o banco recusa")
    class OQueOBancoRecusa {

        /**
         * Intervalo zero significaria pendencia que renasce no mesmo instante em que e
         * cumprida - uma orientacao permanentemente atrasada, que o usuario nunca
         * consegue tirar da lista.
         */
        @Test
        @DisplayName("intervalo zero e recusado pelo CHECK")
        void intervaloZeroERecusado() {
            assertThatThrownBy(() -> careInstructionRepository.saveAndFlush(
                    orientacao().intervalDays(0).build()))
                    .hasMessageContaining("ck_care_instructions_intervalo_positivo");
        }

        @Test
        @DisplayName("intervalo negativo e recusado pelo CHECK")
        void intervaloNegativoERecusado() {
            assertThatThrownBy(() -> careInstructionRepository.saveAndFlush(
                    orientacao().intervalDays(-3).build()))
                    .hasMessageContaining("ck_care_instructions_intervalo_positivo");
        }

        /**
         * Prazo que termina antes de comecar nunca apareceria como pendencia: falharia em
         * silencio, que e o pior modo de falhar. O servico tambem recusa, com 400 - as
         * duas guardas nao sao redundantes.
         */
        @Test
        @DisplayName("prazo que termina antes de comecar e recusado pelo CHECK")
        void prazoIncoerenteERecusado() {
            assertThatThrownBy(() -> careInstructionRepository.saveAndFlush(orientacao()
                    .startsOn(LocalDate.now())
                    .endsOn(LocalDate.now().minusDays(1))
                    .build()))
                    .hasMessageContaining("ck_care_instructions_prazo_coerente");
        }

        /**
         * Cumprimento sem quem cumpriu nao serve ao historico de aderencia, que existe
         * para dizer se alguem esta dando o remedio. NOT NULL, ao contrario da autoria dos
         * outros eventos - aqui nao ha linha antiga sem autor para acomodar.
         */
        @Test
        @DisplayName("cumprimento sem quem confirmou e recusado")
        void cumprimentoSemAutorERecusado() {
            CareInstruction instrucao = careInstructionRepository.saveAndFlush(orientacao().build());

            assertThatThrownBy(() -> fulfillmentRepository.saveAndFlush(
                    CareInstructionFulfillment.builder()
                            .careInstruction(instrucao)
                            .fulfilledAt(LocalDateTime.now())
                            .recordedAt(LocalDateTime.now())
                            .build()))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("a linha do tempo")
    class LinhaDoTempo {

        /**
         * <b>Mandar e fazer sao dois fatos, e a view os separa.</b> Juntar os dois numa
         * entrada perderia o historico de aderencia - o dado que o veterinario nunca tem
         * quando o tratamento nao funciona.
         */
        @Test
        @DisplayName("orientacao e cumprimento entram como eventos distintos")
        void orientacaoECumprimentoSaoDistintos() {
            CareInstruction instrucao = careInstructionRepository.saveAndFlush(orientacao().build());

            fulfillmentRepository.saveAndFlush(CareInstructionFulfillment.builder()
                    .careInstruction(instrucao)
                    .confirmedBy(ulysses)
                    .fulfilledAt(LocalDateTime.now().minusDays(1))
                    .recordedAt(LocalDateTime.now())
                    .build());

            var linha = timelineRepository
                    .findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20));

            assertThat(linha).extracting(TimelineEntry::getEventType)
                    .containsExactly(TimelineEventType.CUMPRIMENTO, TimelineEventType.ORIENTACAO);
        }

        /**
         * A orientacao entra na data em que <b>comeca a valer</b>, e nao na de digitacao:
         * o veterinario prescreveu em marco, e a linha do tempo diz marco.
         */
        @Test
        @DisplayName("a orientacao entra na data em que comeca a valer")
        void orientacaoEntraPelaDataDeInicio() {
            careInstructionRepository.saveAndFlush(orientacao()
                    .startsOn(LocalDate.of(2026, 3, 1))
                    .build());

            assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                    .singleElement()
                    .extracting(e -> e.getOccurredAt().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 3, 1));
        }

        /**
         * <b>O cumprimento herda a organizacao de quem emitiu, e nao de quem cumpriu.</b>
         * Quem da o remedio em casa e o tutor, e ele nao age por organizacao nenhuma - o
         * que a linha do tempo precisa dizer e de qual tratamento aquele cumprimento faz
         * parte. Sem organizacao emissora, o contexto e nulo nos dois.
         */
        @Test
        @DisplayName("o cumprimento carrega a descricao da orientacao a que pertence")
        void cumprimentoCarregaADescricaoDaOrientacao() {
            CareInstruction instrucao = careInstructionRepository.saveAndFlush(orientacao()
                    .description("Meia dose de fenobarbital")
                    .build());

            fulfillmentRepository.saveAndFlush(CareInstructionFulfillment.builder()
                    .careInstruction(instrucao)
                    .confirmedBy(ulysses)
                    .fulfilledAt(LocalDateTime.now())
                    .recordedAt(LocalDateTime.now())
                    .build());

            assertThat(timelineRepository.findDoAnimal(rex.getAnimalId(), PageRequest.of(0, 20)))
                    .extracting(TimelineEntry::getSummary)
                    .containsOnly("Meia dose de fenobarbital");
        }
    }

    @Nested
    @DisplayName("consultas e limpeza")
    class ConsultasELimpeza {

        /**
         * A vigencia e do banco, e nao do Java: a query filtra revogada, futura e
         * expirada. Sem prazo, vale para sempre depois de comecar - o tratamento continuo,
         * que e justamente a populacao que mais precisa de lembrete.
         */
        @Test
        @DisplayName("findVigentesNosAnimais deixa fora a revogada, a futura e a expirada")
        void vigentesDeixaForaOQueNaoVale() {
            CareInstruction vigenteIndefinida = careInstructionRepository
                    .saveAndFlush(orientacao().description("Continua").build());

            careInstructionRepository.saveAndFlush(orientacao()
                    .description("Revogada").revokedAt(LocalDateTime.now()).build());

            careInstructionRepository.saveAndFlush(orientacao()
                    .description("Comeca amanha").startsOn(LocalDate.now().plusDays(1)).build());

            careInstructionRepository.saveAndFlush(orientacao()
                    .description("Terminou ontem")
                    .startsOn(LocalDate.now().minusDays(10))
                    .endsOn(LocalDate.now().minusDays(1))
                    .build());

            assertThat(careInstructionRepository.findVigentesNosAnimais(
                    List.of(rex.getAnimalId()), LocalDate.now()))
                    .extracting(CareInstruction::getCareInstructionId)
                    .containsExactly(vigenteIndefinida.getCareInstructionId());
        }

        /**
         * O ultimo cumprimento e pelo instante do <b>fato</b>, e nao pelo da digitacao: o
         * remedio de ontem digitado hoje nao e o mais recente. Sem isso, a pendencia
         * contaria o intervalo da hora errada.
         */
        @Test
        @DisplayName("o ultimo cumprimento e o do fato mais recente, nao o digitado por ultimo")
        void ultimoCumprimentoEPeloFato() {
            CareInstruction instrucao = careInstructionRepository.saveAndFlush(orientacao().build());

            fulfillmentRepository.saveAndFlush(CareInstructionFulfillment.builder()
                    .careInstruction(instrucao).confirmedBy(ulysses)
                    .fulfilledAt(LocalDateTime.now().minusHours(2))
                    .recordedAt(LocalDateTime.now().minusHours(2))
                    .note("de hoje")
                    .build());

            // digitado depois, mas aconteceu antes
            fulfillmentRepository.saveAndFlush(CareInstructionFulfillment.builder()
                    .careInstruction(instrucao).confirmedBy(ulysses)
                    .fulfilledAt(LocalDateTime.now().minusDays(2))
                    .recordedAt(LocalDateTime.now())
                    .note("de anteontem")
                    .build());

            assertThat(fulfillmentRepository
                    .findFirstByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(
                            instrucao.getCareInstructionId()))
                    .get()
                    .extracting(CareInstructionFulfillment::getNote)
                    .isEqualTo("de hoje");
        }

        /**
         * <b>A quarta vez que uma tabela nova pendurada no animal poderia derrubar a
         * exclusao.</b> O cumprimento e neta - aponta para a orientacao, que aponta para o
         * animal -, entao sair na ordem errada e violacao de FK, e nao um teste falhando
         * por detalhe.
         */
        @Test
        @DisplayName("apagar o animal apaga orientacao e cumprimento, nessa ordem")
        void purgeApagaOsDois() {
            CareInstruction instrucao = careInstructionRepository.saveAndFlush(orientacao().build());

            fulfillmentRepository.saveAndFlush(CareInstructionFulfillment.builder()
                    .careInstruction(instrucao).confirmedBy(ulysses)
                    .fulfilledAt(LocalDateTime.now()).recordedAt(LocalDateTime.now())
                    .build());

            animalPurger.purge(List.of(rex.getAnimalId()));

            assertThat(careInstructionRepository.findByAnimalAnimalIdOrderByStartsOnDesc(
                    rex.getAnimalId())).isEmpty();
            assertThat(fulfillmentRepository
                    .findByCareInstructionCareInstructionIdOrderByFulfilledAtDesc(
                            instrucao.getCareInstructionId())).isEmpty();
            assertThat(animalRepository.findById(rex.getAnimalId())).isEmpty();
        }
    }

}

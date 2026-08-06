package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.security.CurrentPersonProvider;
import br.com.petfy.healthcare.service.VaccineStatusCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A classificacao da agenda e o nucleo da feature, entao os testes ficam nas
 * fronteiras: ontem, hoje, o ultimo dia da janela e o primeiro dia fora dela.
 * As datas sao relativas a LocalDate.now() para o teste nao apodrecer com o
 * passar do tempo.
 */
@ExtendWith(MockitoExtension.class)
class VaccineAgendaTest {

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private CurrentPersonProvider currentPersonProvider;

    // calculator real: a classificacao e justamente o que este teste verifica,
    // entao mocka-la esvaziaria o teste
    @Spy
    private VaccineStatusCalculator vaccineStatusCalculator = new VaccineStatusCalculator();

    @InjectMocks
    private VaccineServiceImpl vaccineService;

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate HOJE = LocalDate.now();

    private Vaccine vacinaComProximaDose(String nome, LocalDate proximaDose) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .vaccineName(nome)
                .applicationDate(HOJE.minusYears(1))
                .nextDoseDate(proximaDose)
                .animal(Animal.builder().animalId(UUID.randomUUID()).name("Rex").build())
                .build();
    }

    private void tutorTem(Vaccine... vacinas) {
        when(currentPersonProvider.require()).thenReturn(Person.builder().personId(OWNER_ID).build());
        when(vaccineRepository.findByAnimalTutorsPersonPersonId(OWNER_ID)).thenReturn(List.of(vacinas));
    }

    @Nested
    @DisplayName("classificacao")
    class Classificacao {

        @Test
        @DisplayName("dose de ontem deve contar como vencida")
        void doseDeOntemDeveContarComoVencida() {
            tutorTem(vacinaComProximaDose("V10", HOJE.minusDays(1)));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getOverdueCount()).isEqualTo(1);
            assertThat(agenda.getItems()).singleElement()
                    .satisfies(item -> {
                        assertThat(item.getStatus()).isEqualTo(VaccineStatus.OVERDUE);
                        assertThat(item.getDaysUntilNextDose()).isEqualTo(-1L);
                    });
        }

        @Test
        @DisplayName("dose de hoje deve contar como vencendo, nao como vencida")
        void doseDeHojeDeveContarComoVencendo() {
            tutorTem(vacinaComProximaDose("V10", HOJE));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getOverdueCount()).isZero();
            assertThat(agenda.getDueSoonCount()).isEqualTo(1);
            assertThat(agenda.getItems()).singleElement()
                    .satisfies(item -> assertThat(item.getDaysUntilNextDose()).isZero());
        }

        @Test
        @DisplayName("dose no ultimo dia da janela ainda deve entrar na agenda")
        void doseNoUltimoDiaDaJanelaDeveEntrar() {
            tutorTem(vacinaComProximaDose("V10", HOJE.plusDays(30)));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getDueSoonCount()).isEqualTo(1);
            assertThat(agenda.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("dose um dia depois da janela deve ficar de fora da agenda")
        void doseAlemDaJanelaDeveFicarDeFora() {
            tutorTem(vacinaComProximaDose("V10", HOJE.plusDays(31)));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getUpToDateCount()).isEqualTo(1);
            assertThat(agenda.getDueSoonCount()).isZero();
            assertThat(agenda.getItems()).isEmpty();
        }

        @Test
        @DisplayName("vacina sem proxima dose deve ser contada a parte, sem virar pendencia")
        void vacinaSemProximaDoseNaoDeveVirarPendencia() {
            tutorTem(vacinaComProximaDose("Dose unica", null));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getWithoutNextDoseCount()).isEqualTo(1);
            assertThat(agenda.getOverdueCount()).isZero();
            assertThat(agenda.getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("montagem da resposta")
    class MontagemDaResposta {

        @Test
        @DisplayName("deve listar da mais atrasada para a menos urgente")
        void deveListarDaMaisAtrasadaParaAMenosUrgente() {
            tutorTem(
                    vacinaComProximaDose("Vencendo", HOJE.plusDays(10)),
                    vacinaComProximaDose("Muito atrasada", HOJE.minusDays(60)),
                    vacinaComProximaDose("Pouco atrasada", HOJE.minusDays(2)));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getItems())
                    .extracting("vaccineName")
                    .containsExactly("Muito atrasada", "Pouco atrasada", "Vencendo");
        }

        @Test
        @DisplayName("deve levar o nome do animal junto, para a tela nao precisar de outra chamada")
        void deveLevarNomeDoAnimal() {
            tutorTem(vacinaComProximaDose("V10", HOJE.minusDays(1)));

            assertThat(vaccineService.getAgenda(30).getItems()).singleElement()
                    .satisfies(item -> assertThat(item.getAnimalName()).isEqualTo("Rex"));
        }

        @Test
        @DisplayName("os contadores devem cobrir tudo, mesmo o que fica fora da lista")
        void contadoresDevemCobrirTudo() {
            tutorTem(
                    vacinaComProximaDose("Vencida", HOJE.minusDays(5)),
                    vacinaComProximaDose("Vencendo", HOJE.plusDays(5)),
                    vacinaComProximaDose("Em dia", HOJE.plusDays(200)),
                    vacinaComProximaDose("Sem proxima", null));

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getOverdueCount()).isEqualTo(1);
            assertThat(agenda.getDueSoonCount()).isEqualTo(1);
            assertThat(agenda.getUpToDateCount()).isEqualTo(1);
            assertThat(agenda.getWithoutNextDoseCount()).isEqualTo(1);
            assertThat(agenda.getItems()).hasSize(2);
            assertThat(agenda.getReferenceDate()).isEqualTo(HOJE);
            assertThat(agenda.getWindowDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("janela maior deve puxar para a agenda o que estava em dia")
        void janelaMaiorDevePuxarOQueEstavaEmDia() {
            tutorTem(vacinaComProximaDose("Daqui a 90 dias", HOJE.plusDays(90)));

            assertThat(vaccineService.getAgenda(30).getItems()).isEmpty();
            assertThat(vaccineService.getAgenda(120).getItems()).hasSize(1);
        }

        @Test
        @DisplayName("tutor sem vacinas deve receber agenda vazia, e nao erro")
        void tutorSemVacinasDeveReceberAgendaVazia() {
            tutorTem();

            var agenda = vaccineService.getAgenda(30);

            assertThat(agenda.getItems()).isEmpty();
            assertThat(agenda.getOverdueCount()).isZero();
        }
    }
}

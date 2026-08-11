package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AnimalHealthConditionRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A coluna "situacao" da Tela 03, que o produto nao sabia responder.
 *
 * O `VetPetDTO` trazia nome, tutor, raca, sexo, nascimento e peso — <b>zero sobre saude</b> —, e
 * a pendencia era sempre da pessoa logada. A tela dizia isso em vez de fingir a coluna.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("a situacao dos pacientes, em lote")
class PatientSituationReaderTest {

    @Mock private VaccineRepository vaccineRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private AnimalHealthConditionRepository animalHealthConditionRepository;

    private PatientSituationReader reader;

    private static final UUID CODE = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reader = new PatientSituationReader(vaccineRepository, healthRecordRepository,
                animalHealthConditionRepository, new VaccineStatusCalculator());

        lenient().when(healthRecordRepository.ultimaVisitaPorAnimal(any())).thenReturn(List.of());
        lenient().when(animalHealthConditionRepository.idsComCondicaoAberta(any())).thenReturn(List.of());
    }

    private Vaccine dose(UUID animalId, LocalDate proximaDose) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .animal(Animal.builder().animalId(animalId).build())
                .nextDoseDate(proximaDose)
                .build();
    }

    @Nested
    @DisplayName("qual dose decide a linha")
    class OPior {

        /**
         * A tabela existe para decidir a quem ligar hoje. Um animal com dez doses em dia e uma
         * vencida nao esta em dia — e mostrar "em dia" ali seria o produto afirmando saude que
         * ele mesmo sabe que nao ha.
         */
        @Test
        @DisplayName("uma vencida vence dez em dia")
        void vencidaVence() {
            when(vaccineRepository.findByAnimalAnimalIdIn(any())).thenReturn(List.of(
                    dose(CODE, LocalDate.now().plusYears(1)),
                    dose(CODE, LocalDate.now().minusDays(3)),
                    dose(CODE, LocalDate.now().plusMonths(6))));

            assertThat(reader.de(CODE).status()).isEqualTo(VaccineStatus.OVERDUE);
        }

        @Test
        @DisplayName("vencendo dentro da janela vence em dia")
        void vencendoVenceEmDia() {
            when(vaccineRepository.findByAnimalAnimalIdIn(any())).thenReturn(List.of(
                    dose(CODE, LocalDate.now().plusYears(1)),
                    dose(CODE, LocalDate.now().plusDays(10))));

            assertThat(reader.de(CODE).status()).isEqualTo(VaccineStatus.DUE_SOON);
        }

        /**
         * <b>Sem prazo nao e "em dia".</b> Pode ser dose unica e pode ser carteira nao registrada,
         * e afirmar saude a partir de ausencia de dado e a mentira que o produto existe para nao
         * contar. Por isso ela fica embaixo de tudo e nao mascara o vencimento ao lado.
         */
        @Test
        @DisplayName("dose sem proxima nao mascara a que esta vencendo")
        void semPrazoNaoMascara() {
            when(vaccineRepository.findByAnimalAnimalIdIn(any())).thenReturn(List.of(
                    dose(CODE, null),
                    dose(CODE, LocalDate.now().plusDays(5))));

            assertThat(reader.de(CODE).status()).isEqualTo(VaccineStatus.DUE_SOON);
        }

        @Test
        @DisplayName("animal sem vacina nenhuma fica em NO_NEXT_DOSE, e nao em dia")
        void semVacina() {
            when(vaccineRepository.findByAnimalAnimalIdIn(any())).thenReturn(List.of());

            assertThat(reader.de(CODE).status()).isEqualTo(VaccineStatus.NO_NEXT_DOSE);
        }
    }

    @Nested
    @DisplayName("o conjunto vazio")
    class Vazio {

        /** {@code in ()} nao e SQL valido, e a pergunta tambem nao faz sentido. */
        @Test
        @DisplayName("sem animal nenhum, nao consulta o banco")
        void naoConsulta() {
            assertThat(reader.de(List.of())).isEmpty();

            verify(vaccineRepository, never()).findByAnimalAnimalIdIn(any());
            verify(healthRecordRepository, never()).ultimaVisitaPorAnimal(any());
        }

        @Test
        @DisplayName("o resumo de um conjunto vazio e zero, e nao um erro")
        void resumoZerado() {
            var resumo = reader.resumo(List.of());

            assertThat(resumo.getTotal()).isZero();
            assertThat(resumo.getDueIn30Days()).isZero();
        }
    }

    @Nested
    @DisplayName("o resumo do cabecalho")
    class Resumo {

        /**
         * Vencidas entram no "vencendo em 30 dias": separar as duas daria ao cabecalho um numero
         * que esconde o pior caso.
         */
        @Test
        @DisplayName("vencida e vencendo contam juntas")
        void vencidaEVencendoContamJuntas() {
            UUID nina = UUID.randomUUID();
            UUID rex = UUID.randomUUID();

            when(vaccineRepository.findByAnimalAnimalIdIn(any())).thenReturn(List.of(
                    dose(CODE, LocalDate.now().minusDays(2)),
                    dose(nina, LocalDate.now().plusDays(10)),
                    dose(rex, LocalDate.now().plusYears(1))));
            when(animalHealthConditionRepository.idsComCondicaoAberta(any())).thenReturn(List.of(rex));
            when(healthRecordRepository.contarAnimaisAtendidosDesde(any(), any())).thenReturn(2L);

            var resumo = reader.resumo(List.of(CODE, nina, rex));

            assertThat(resumo.getDueIn30Days()).isEqualTo(2);
            assertThat(resumo.getUnderTreatment()).isEqualTo(1);
            assertThat(resumo.getSeenThisMonth()).isEqualTo(2);
            assertThat(resumo.getTotal()).isEqualTo(3);
        }
    }
}

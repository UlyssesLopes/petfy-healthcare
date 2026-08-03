package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaccineReminderServiceTest {

    @Mock
    private VaccineRepository vaccineRepository;

    @Mock
    private ReminderNotifier notifier;

    @InjectMocks
    private VaccineReminderService service;

    private static final LocalDate HOJE = LocalDate.now();
    private static final LocalDateTime AGORA = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "windowDays", 30);
        ReflectionTestUtils.setField(service, "cooldownDays", 7);
    }

    private Owner owner(String nome, String email) {
        return Owner.builder().ownerId(UUID.randomUUID()).name(nome).email(email).build();
    }

    private Vaccine vacina(Owner owner, String petName, String vaccineName,
                           LocalDate proximaDose, LocalDateTime ultimoEnvio) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .vaccineName(vaccineName)
                .nextDoseDate(proximaDose)
                .lastReminderSentAt(ultimoEnvio)
                .pet(Pet.builder().petId(UUID.randomUUID()).name(petName).owner(owner).build())
                .build();
    }

    private void baseTem(Vaccine... vacinas) {
        when(vaccineRepository.findByNextDoseDateLessThanEqual(any())).thenReturn(List.of(vacinas));
    }

    @Nested
    @DisplayName("regra de reenvio")
    class RegraDeReenvio {

        @Test
        @DisplayName("deve avisar quando a dose nunca teve lembrete")
        void deveAvisarQuandoNuncaTeveLembrete() {
            baseTem(vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10", HOJE.minusDays(3), null));

            assertThat(service.enviarLembretes()).isEqualTo(1);
            verify(notifier).notify(any());
        }

        @Test
        @DisplayName("nao deve reavisar a mesma dose dentro do cooldown")
        void naoDeveReavisarDentroDoCooldown() {
            baseTem(vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10",
                    HOJE.minusDays(3), AGORA.minusDays(2)));

            assertThat(service.enviarLembretes()).isZero();
            verifyNoInteractions(notifier);
        }

        @Test
        @DisplayName("deve reavisar quando o cooldown ja passou - a dose segue vencida")
        void deveReavisarDepoisDoCooldown() {
            baseTem(vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10",
                    HOJE.minusDays(30), AGORA.minusDays(8)));

            assertThat(service.enviarLembretes()).isEqualTo(1);
            verify(notifier).notify(any());
        }

        @Test
        @DisplayName("deve ignorar dose sem data de proxima aplicacao")
        void deveIgnorarDoseSemProximaData() {
            baseTem(vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "Dose unica", null, null));

            assertThat(service.enviarLembretes()).isZero();
            verifyNoInteractions(notifier);
        }

        @Test
        @DisplayName("deve marcar o envio para o cooldown valer na proxima execucao")
        void deveMarcarOEnvio() {
            var vacina = vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10", HOJE.minusDays(3), null);
            baseTem(vacina);

            service.enviarLembretes();

            verify(vaccineRepository).saveAll(any());
            assertThat(vacina.getLastReminderSentAt()).isNotNull();
        }

        @Test
        @DisplayName("nao deve marcar como avisada uma dose cujo envio falhou")
        void naoDeveMarcarQuandoEnvioFalha() {
            var vacina = vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10", HOJE.minusDays(3), null);
            baseTem(vacina);
            doThrow(new RuntimeException("SMTP fora do ar")).when(notifier).notify(any());

            assertThatThrownBy(() -> service.enviarLembretes())
                    .isInstanceOf(RuntimeException.class);

            assertThat(vacina.getLastReminderSentAt()).isNull();
            verify(vaccineRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("agrupamento")
    class Agrupamento {

        @Test
        @DisplayName("deve mandar um lembrete por tutor, nao um por dose")
        void deveMandarUmLembretePorTutor() {
            var ulysses = owner("Ulysses", "u@petfy.com.br");
            baseTem(
                    vacina(ulysses, "Rex", "V10", HOJE.minusDays(3), null),
                    vacina(ulysses, "Mia", "Antirrabica", HOJE.plusDays(5), null),
                    vacina(ulysses, "Rex", "Giardia", HOJE.minusDays(10), null));

            assertThat(service.enviarLembretes()).isEqualTo(1);

            var captor = ArgumentCaptor.forClass(VaccineReminder.class);
            verify(notifier, times(1)).notify(captor.capture());
            assertThat(captor.getValue().getItems()).hasSize(3);
        }

        @Test
        @DisplayName("deve separar tutores diferentes em lembretes diferentes")
        void deveSepararTutoresDiferentes() {
            baseTem(
                    vacina(owner("Ulysses", "u@petfy.com.br"), "Rex", "V10", HOJE.minusDays(3), null),
                    vacina(owner("Maria", "m@petfy.com.br"), "Nina", "V8", HOJE.minusDays(1), null));

            assertThat(service.enviarLembretes()).isEqualTo(2);
            verify(notifier, times(2)).notify(any());
        }

        @Test
        @DisplayName("deve ordenar as doses do lembrete da mais atrasada para a menos urgente")
        void deveOrdenarDaMaisAtrasada() {
            var ulysses = owner("Ulysses", "u@petfy.com.br");
            baseTem(
                    vacina(ulysses, "Mia", "Vencendo", HOJE.plusDays(5), null),
                    vacina(ulysses, "Rex", "Muito atrasada", HOJE.minusDays(40), null));

            service.enviarLembretes();

            var captor = ArgumentCaptor.forClass(VaccineReminder.class);
            verify(notifier).notify(captor.capture());
            assertThat(captor.getValue().getItems())
                    .extracting(VaccineReminder.Item::getVaccineName)
                    .containsExactly("Muito atrasada", "Vencendo");
        }

        @Test
        @DisplayName("o item deve saber se esta vencido e ha quantos dias")
        void itemDeveSaberSeEstaVencido() {
            var ulysses = owner("Ulysses", "u@petfy.com.br");
            baseTem(
                    vacina(ulysses, "Rex", "Atrasada", HOJE.minusDays(4), null),
                    vacina(ulysses, "Mia", "Chegando", HOJE.plusDays(6), null));

            service.enviarLembretes();

            var captor = ArgumentCaptor.forClass(VaccineReminder.class);
            verify(notifier).notify(captor.capture());

            assertThat(captor.getValue().getItems().get(0).isOverdue()).isTrue();
            assertThat(captor.getValue().getItems().get(0).getDaysUntilNextDose()).isEqualTo(-4L);
            assertThat(captor.getValue().getItems().get(1).isOverdue()).isFalse();
            assertThat(captor.getValue().getItems().get(1).getDaysUntilNextDose()).isEqualTo(6L);
        }
    }

    @Nested
    @DisplayName("execucao vazia")
    class ExecucaoVazia {

        @Test
        @DisplayName("nao deve notificar ninguem quando nao ha dose pendente")
        void naoDeveNotificarSemDosePendente() {
            baseTem();

            assertThat(service.enviarLembretes()).isZero();
            verifyNoInteractions(notifier);
            verify(vaccineRepository, never()).saveAll(any());
        }
    }
}

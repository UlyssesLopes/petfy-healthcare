package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VaccineRecordedNotifierTest {

    @Mock
    private Notifier notifier;

    @InjectMocks
    private VaccineRecordedNotifier vaccineRecordedNotifier;

    private Vaccine vacina(LocalDate proximaDose, Clinic clinic) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .vaccineName("V10")
                .applicationDate(LocalDate.of(2026, 8, 1))
                .nextDoseDate(proximaDose)
                .clinic(clinic)
                .pet(Pet.builder()
                        .petId(UUID.randomUUID())
                        .name("Rex")
                        .owner(Owner.builder().ownerId(UUID.randomUUID())
                                .name("Ulysses").email("u@petfy.com.br").build())
                        .build())
                .build();
    }

    private Clinic clinic() {
        return Clinic.builder().clinicId(UUID.randomUUID()).name("Clinica Bicho Feliz").build();
    }

    private Notification capturarEnvio() {
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifier).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("deve avisar o tutor do pet, e nao outra pessoa")
    void deveAvisarOTutorDoPet() {
        vaccineRecordedNotifier.notifyOwner(vacina(LocalDate.of(2027, 8, 1), clinic()));

        var notificacao = capturarEnvio();
        assertThat(notificacao.getToEmail()).isEqualTo("u@petfy.com.br");
        assertThat(notificacao.getToName()).isEqualTo("Ulysses");
        assertThat(notificacao.getSubject()).isEqualTo("Nova vacina registrada em Rex");
    }

    @Test
    @DisplayName("deve dizer qual clinica registrou, que e o que o tutor precisa reconhecer")
    void deveDizerQualClinicaRegistrou() {
        vaccineRecordedNotifier.notifyOwner(vacina(LocalDate.of(2027, 8, 1), clinic()));

        assertThat(capturarEnvio().getLines())
                .anySatisfy(l -> assertThat(l).contains("Clinica Bicho Feliz", "Rex"))
                .anySatisfy(l -> assertThat(l).contains("V10", "2026-08-01"));
    }

    @Test
    @DisplayName("deve informar a proxima dose quando ha uma")
    void deveInformarProximaDose() {
        vaccineRecordedNotifier.notifyOwner(vacina(LocalDate.of(2027, 8, 1), clinic()));

        assertThat(capturarEnvio().getLines())
                .anySatisfy(l -> assertThat(l).contains("proxima dose", "2027-08-01"));
    }

    @Test
    @DisplayName("nao deve inventar proxima dose quando a vacina nao tem")
    void naoDeveInventarProximaDose() {
        vaccineRecordedNotifier.notifyOwner(vacina(null, clinic()));

        assertThat(capturarEnvio().getLines())
                .noneSatisfy(l -> assertThat(l).contains("proxima dose"));
    }

    @Test
    @DisplayName("deve orientar a revogar o acesso caso o tutor nao reconheca o registro")
    void deveOrientarARevogar() {
        vaccineRecordedNotifier.notifyOwner(vacina(null, clinic()));

        assertThat(capturarEnvio().getLines())
                .anySatisfy(l -> assertThat(l).contains("revogue o acesso"));
    }

    @Test
    @DisplayName("deve funcionar mesmo sem clinica na vacina")
    void deveFuncionarSemClinica() {
        vaccineRecordedNotifier.notifyOwner(vacina(null, null));

        assertThat(capturarEnvio().getLines()).isNotEmpty();
    }

    @Test
    @DisplayName("falha de envio nao deve derrubar o registro da vacina")
    void falhaDeEnvioNaoDeveDerrubarORegistro() {
        doThrow(new RuntimeException("SMTP fora do ar")).when(notifier).send(any());

        assertThatCode(() -> vaccineRecordedNotifier.notifyOwner(vacina(null, clinic())))
                .doesNotThrowAnyException();
    }
}

package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClinicActivityNotifierTest {

    @Mock
    private AsyncNotificationDispatcher dispatcher;

    @InjectMocks
    private ClinicActivityNotifier notifier;

    private Owner tutor(boolean emailConfirmado) {
        return Owner.builder()
                .ownerId(UUID.randomUUID())
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .emailVerifiedAt(emailConfirmado ? LocalDateTime.now().minusDays(1) : null)
                .build();
    }

    private Vaccine vacina(Owner dono) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .vaccineName("V10")
                .applicationDate(LocalDate.of(2026, 8, 1))
                .nextDoseDate(LocalDate.of(2027, 8, 1))
                .clinic(Clinic.builder().name("Clinica Pet Feliz").build())
                .pet(Pet.builder().name("Rex").tutors(br.com.petfy.healthcare.PetTutores.titular(dono)).build())
                .build();
    }

    private HealthRecord atendimento(Owner dono) {
        return HealthRecord.builder()
                .eventType("Consulta")
                .eventDate(LocalDate.of(2026, 8, 1))
                .description("Checkup anual")
                .clinic(Clinic.builder().name("Clinica Pet Feliz").build())
                .pet(Pet.builder().name("Rex").tutors(br.com.petfy.healthcare.PetTutores.titular(dono)).build())
                .build();
    }

    private Notification capturar() {
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher).dispatch(captor.capture(), anyString());
        return captor.getValue();
    }

    @Test
    @DisplayName("vacina registrada deve virar aviso com pet, clinica e proxima dose")
    void vacinaRegistradaDeveVirarAviso() {
        var dono = tutor(true);

        notifier.vaccineRecorded(vacina(dono));

        var notificacao = capturar();
        assertThat(notificacao.getToEmail()).isEqualTo("ulysses@petfy.com.br");
        assertThat(String.join(" ", notificacao.getLines()))
                .contains("Clinica Pet Feliz")
                .contains("Rex")
                .contains("V10")
                .contains("2027-08-01");
    }

    /**
     * A orientacao final e o que torna o aviso util: sem ela o tutor le que uma
     * clinica escreveu no pet dele e nao sabe o que fazer a respeito.
     */
    @Test
    @DisplayName("todo aviso deve terminar com a orientacao de revogar o acesso")
    void todoAvisoDeveTerminarComOrientacao() {
        notifier.healthRecordRecorded(atendimento(tutor(true)));

        assertThat(capturar().getLines())
                .last().asString().contains("revogue o acesso da clinica");
    }

    /**
     * O risco de notificar endereco nao confirmado nao e incomodo: e o nome do
     * pet e do tutor chegando na caixa de um estranho.
     */
    @Test
    @DisplayName("nao deve notificar tutor que ainda nao confirmou o e-mail")
    void naoDeveNotificarTutorSemEmailConfirmado() {
        notifier.vaccineRecorded(vacina(tutor(false)));

        verify(dispatcher, never()).dispatch(any(), anyString());
    }

    @Test
    @DisplayName("a supressao deve valer para os quatro avisos, nao so para a vacina")
    void supressaoDeveValerParaOsQuatroAvisos() {
        var dono = tutor(false);

        notifier.vaccineRecorded(vacina(dono));
        notifier.vaccineCorrected(vacina(dono));
        notifier.healthRecordRecorded(atendimento(dono));
        notifier.healthRecordCorrected(atendimento(dono));

        verify(dispatcher, never()).dispatch(any(), anyString());
    }

    /**
     * O efeito principal e o registro no historico do pet, que ja foi gravado
     * quando este metodo roda. Deixar a excecao subir desfaria o registro por
     * causa de um aviso - trocar um problema pequeno por um grande.
     *
     * O caso concreto e o pool de envio em shutdown durante um deploy: enfileirar
     * lanca RejectedExecutionException, e sem o try isso chegaria ao veterinario
     * como falha ao registrar a vacina.
     */
    @Test
    @DisplayName("falha ao despachar o aviso nao pode subir para quem registrou")
    void falhaAoDespacharNaoPodeSubir() {
        doThrow(new RejectedExecutionException("pool em shutdown"))
                .when(dispatcher).dispatch(any(), anyString());

        assertThatCode(() -> notifier.vaccineRecorded(vacina(tutor(true))))
                .doesNotThrowAnyException();
    }

}

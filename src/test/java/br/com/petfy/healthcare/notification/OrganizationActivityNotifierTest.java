package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationActivityNotifierTest {

    @Mock
    private AsyncNotificationDispatcher dispatcher;

    /**
     * Os destinatarios vem do AnimalReach desde o P2b: alcancar um animal passou a ter
     * duas origens - custodia e concessao -, e a colecao do animal nao responde mais
     * isso sozinha.
     */
    @Mock
    private br.com.petfy.healthcare.service.AnimalReach animalReach;

    @InjectMocks
    private OrganizationActivityNotifier notifier;

    private Person tutor(boolean emailConfirmado) {
        return Person.builder()
                .personId(UUID.randomUUID())
                .name("Ulysses")
                .email("ulysses@petfy.com.br")
                .emailVerifiedAt(emailConfirmado ? LocalDateTime.now().minusDays(1) : null)
                .build();
    }

    private Vaccine vacina(Person dono) {
        return Vaccine.builder()
                .vaccineId(UUID.randomUUID())
                .vaccineName("V10")
                .applicationDate(LocalDate.of(2026, 8, 1))
                .nextDoseDate(LocalDate.of(2027, 8, 1))
                .organization(Organization.builder().name("Clinica Animal Feliz").build())
                .animal(Animal.builder().name("Rex").custodies(br.com.petfy.healthcare.Custodias.titular(dono)).build())
                .build();
    }

    private HealthRecord atendimento(Person dono) {
        return HealthRecord.builder()
                .eventType("Consulta")
                .eventDate(LocalDate.of(2026, 8, 1))
                .description("Checkup anual")
                .organization(Organization.builder().name("Clinica Animal Feliz").build())
                .animal(Animal.builder().name("Rex").custodies(br.com.petfy.healthcare.Custodias.titular(dono)).build())
                .build();
    }

    /**
     * Diz ao AnimalReach quem alcanca o animal.
     *
     * Antes o notifier lia a colecao do animal, e montar o animal bastava. Agora ele
     * pergunta - de proposito, porque a colecao e lazy e pode nao refletir o que foi
     * gravado -, entao o teste responde.
     */
    private void quemAlcanca(Person... pessoas) {
        when(animalReach.pessoas(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.List.of(pessoas));
    }

    private Notification capturar() {
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher).dispatch(captor.capture(), anyString());
        return captor.getValue();
    }

    @Test
    @DisplayName("vacina registrada deve virar aviso com animal, clinica e proxima dose")
    void vacinaRegistradaDeveVirarAviso() {
        var dono = tutor(true);
        quemAlcanca(dono);

        notifier.vaccineRecorded(vacina(dono));

        var notificacao = capturar();
        assertThat(notificacao.getToEmail()).isEqualTo("ulysses@petfy.com.br");
        assertThat(String.join(" ", notificacao.getLines()))
                .contains("Clinica Animal Feliz")
                .contains("Rex")
                .contains("V10")
                .contains("2027-08-01");
    }

    /**
     * A orientacao final e o que torna o aviso util: sem ela o tutor le que uma
     * clinica escreveu no animal dele e nao sabe o que fazer a respeito.
     */
    @Test
    @DisplayName("todo aviso deve terminar com a orientacao de revogar o acesso")
    void todoAvisoDeveTerminarComOrientacao() {
        var comEmail = tutor(true);
        quemAlcanca(comEmail);
        notifier.healthRecordRecorded(atendimento(comEmail));

        assertThat(capturar().getLines())
                .last().asString().contains("revogue o acesso da clinica");
    }

    /*
     * ESTES DOIS AFIRMAVAM O CONTRARIO ATE A V48, E MUDARAM DE LADO DE PROPOSITO.
     *
     * Eles diziam "nao deve notificar tutor que ainda nao confirmou o e-mail", e o `verify(never())`
     * era sobre o DISPATCHER. Estava certo enquanto o e-mail era o unico canal: o risco de escrever
     * para endereco nao confirmado nao e incomodo, e o nome do animal e do tutor chegando na caixa
     * de um estranho.
     *
     * <b>O risco continua sendo esse, e por isso o e-mail continua nao saindo.</b> O que mudou e que
     * suprimir no DOMINIO passou a calar tambem o aviso in-app — que so aparece para quem ja entrou
     * na conta, e portanto nao vaza nada. Quem nao confirmou o e-mail era justamente quem ficava sem
     * canal nenhum.
     *
     * A assercao nova e mais forte que a antiga: prova que o aviso EXISTE e que o e-mail NAO sai.
     */
    @Test
    @DisplayName("o tutor sem e-mail confirmado recebe o aviso no app, e nao por e-mail")
    void semEmailConfirmadoRecebeSoNoApp() {
        var semEmail = tutor(false);
        quemAlcanca(semEmail);
        notifier.vaccineRecorded(vacina(semEmail));

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher).dispatch(captor.capture(), anyString());

        assertThat(captor.getValue().isPorEmail()).isFalse();
    }

    @Test
    @DisplayName("os quatro avisos chegam ao app, e nenhum dos quatro sai por e-mail")
    void osQuatroAvisosValemNoApp() {
        var dono = tutor(false);
        quemAlcanca(dono);

        notifier.vaccineRecorded(vacina(dono));
        notifier.vaccineCorrected(vacina(dono));
        notifier.healthRecordRecorded(atendimento(dono));
        notifier.healthRecordCorrected(atendimento(dono));

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher, times(4)).dispatch(captor.capture(), anyString());

        assertThat(captor.getAllValues())
                .hasSize(4)
                .allSatisfy(aviso -> assertThat(aviso.isPorEmail()).isFalse());
    }

    /**
     * O efeito principal e o registro no historico do animal, que ja foi gravado
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

        quemAlcanca(tutor(true));
        assertThatCode(() -> notifier.vaccineRecorded(vacina(tutor(true))))
                .doesNotThrowAnyException();
    }

}

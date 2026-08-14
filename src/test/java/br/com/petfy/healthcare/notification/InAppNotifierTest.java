package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PersonNotification;
import br.com.petfy.healthcare.domain.repository.PersonNotificationRepository;
import br.com.petfy.healthcare.domain.repository.PersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * O canal que nao sai do produto.
 *
 * <b>Ele fecha o buraco que o e-mail nao fechava:</b> a Tela 03 lista quem esta vencendo e nao
 * avisava ninguem, e a 47 promete "voce e avisado na hora". O e-mail nao chega a quem ainda nao
 * confirmou o endereco — politica correta la — nem a quem nao abre a caixa.
 */
@ExtendWith(MockitoExtension.class)
class InAppNotifierTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonNotificationRepository personNotificationRepository;

    /* O canal de tempo real, que o notificador aciona depois de gravar. */
    @Mock
    private AvisoStream avisoStream;

    @InjectMocks
    private InAppNotifier notifier;

    private Person ana() {
        return Person.builder()
                .personId(UUID.randomUUID())
                .name("Ana Prado")
                .email("ana@petfy.com.br")
                .build();
    }

    private Notification aviso(String assunto, List<String> linhas) {
        return Notification.builder()
                .toEmail("ana@petfy.com.br")
                .toName("Ana Prado")
                .subject(assunto)
                .lines(linhas)
                .build();
    }

    private PersonNotification gravado() {
        var captor = ArgumentCaptor.forClass(PersonNotification.class);
        verify(personNotificationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("guarda o mesmo texto que foi por e-mail, e nao um resumo")
    void guardaOTextoInteiro() {
        when(personRepository.findByEmail("ana@petfy.com.br")).thenReturn(Optional.of(ana()));

        notifier.registrar(aviso("Marcelo entrou no Code",
                List.of("Marcelo passou a cuidar do Code com voce.", "- papel: editor")),
                "tutor que entrou no animal");

        PersonNotification linha = gravado();

        assertThat(linha.getSubject()).isEqualTo("Marcelo entrou no Code");
        assertThat(linha.getBody())
                .contains("Marcelo passou a cuidar do Code com voce.")
                .contains("- papel: editor");
        assertThat(linha.getEvent()).isEqualTo("tutor que entrou no animal");
        assertThat(linha.getReadAt()).isNull();
    }

    /*
     * Quem recebe um convite pode nao ter conta — e o caso comum, e a razao de o convite existir.
     * Nao ha caixa de entrada onde guardar, e o e-mail ja e o canal que alcanca essa pessoa.
     */
    @Test
    @DisplayName("sai calado quando o endereco nao e de ninguem")
    void semContaNaoGuarda() {
        when(personRepository.findByEmail("desconhecido@exemplo.com")).thenReturn(Optional.empty());

        notifier.registrar(Notification.builder()
                .toEmail("desconhecido@exemplo.com")
                .subject("Marcelo convidou voce a cuidar do Code")
                .lines(List.of("Codigo: TOKEN"))
                .build(), "convite de animal");

        verifyNoInteractions(personNotificationRepository);
    }

    @Test
    @DisplayName("sem endereco nao procura ninguem")
    void semEnderecoNaoProcura() {
        notifier.registrar(Notification.builder()
                .subject("x").lines(List.of("y")).build(), "evento");

        verifyNoInteractions(personRepository, personNotificationRepository);
    }

    /*
     * Um assunto comprido nao pode ser a razao de a pessoa nao ser avisada de que alguem entrou no
     * animal dela. O corpo, que e onde mora o que aconteceu, fica inteiro.
     */
    @Test
    @DisplayName("recorta o assunto comprido em vez de recusar o aviso")
    void recortaOAssunto() {
        when(personRepository.findByEmail("ana@petfy.com.br")).thenReturn(Optional.of(ana()));

        String comprido = "A".repeat(400);
        String corpoInteiro = "B".repeat(5000);

        notifier.registrar(aviso(comprido, List.of(corpoInteiro)), "evento");

        PersonNotification linha = gravado();

        assertThat(linha.getSubject()).hasSize(200).endsWith("...");
        assertThat(linha.getBody()).hasSize(5000);
    }
}

package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.MembershipRole;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import br.com.petfy.healthcare.domain.entity.Species;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * O convite passou a chegar a quem foi convidado, e este teste guarda o que ele pode dizer.
 *
 * <b>Ele existe porque o convite nao chegava a ninguem.</b> O `invite` gravava e devolvia o token, e
 * nao havia envio em lugar nenhum: o convite ficava de pe esperando alguem que nunca soube dele.
 */
@ExtendWith(MockitoExtension.class)
class InviteNotifierTest {

    @Mock
    private AsyncNotificationDispatcher dispatcher;

    @InjectMocks
    private InviteNotifier notifier;

    private Person marcelo() {
        return Person.builder()
                .personId(UUID.randomUUID())
                .name("Marcelo Lopes")
                .email("marcelo@petfy.com.br")
                .build();
    }

    private Animal code() {
        return Animal.builder().animalId(UUID.randomUUID()).name("Code").species(Species.CANINA).build();
    }

    private Organization vilaNova() {
        return Organization.builder().organizationId(UUID.randomUUID()).name("Clinica Vila Nova").build();
    }

    private Notification enviada() {
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(dispatcher).dispatch(captor.capture(), anyString());
        return captor.getValue();
    }

    private String corpo(Notification notificacao) {
        return String.join("\n", notificacao.getLines());
    }

    @Nested
    @DisplayName("conviteDeAnimal")
    class ConviteDeAnimal {

        @Test
        @DisplayName("nomeia quem convidou e o animal, porque sem os dois o e-mail e indistinguivel de um golpe")
        void nomeiaOsDois() {
            notifier.conviteDeAnimal(code(), marcelo(), "ana@exemplo.com", PetTutorRole.EDITOR,
                    "TOKEN-123", 7);

            Notification notificacao = enviada();

            assertThat(notificacao.getToEmail()).isEqualTo("ana@exemplo.com");
            assertThat(notificacao.getSubject()).contains("Marcelo Lopes").contains("Code");
            assertThat(corpo(notificacao)).contains("Marcelo Lopes").contains("Code");
        }

        /*
         * O token E o codigo, e nao um segundo segredo: quem o tem aceita. O teste afirma que ele
         * viaja inteiro — um codigo truncado por um formatador deixaria o e-mail bonito e inutil.
         */
        @Test
        @DisplayName("manda o codigo, e ele e o token do convite")
        void mandaOCodigo() {
            notifier.conviteDeAnimal(code(), marcelo(), "ana@exemplo.com", PetTutorRole.EDITOR,
                    "TOKEN-123", 7);

            assertThat(corpo(enviada())).contains("Codigo: TOKEN-123").contains("7 dias");
        }

        /*
         * HOLDER transfere a titularidade do animal, e aceitar sem saber disso seria aceitar outra
         * coisa. A palavra "responde" e o que separa os dois convites, e ela nao pode sumir.
         */
        @Test
        @DisplayName("diz que a titularidade muda quando o papel e HOLDER")
        void titularidadeEDita() {
            notifier.conviteDeAnimal(code(), marcelo(), "ana@exemplo.com", PetTutorRole.HOLDER,
                    "TOKEN-123", 7);

            assertThat(corpo(enviada())).contains("responsabilidade").contains("responde pelo Code");
        }

        @Test
        @DisplayName("nao promete titularidade a quem foi convidado para dividir o cuidado")
        void coTutoriaNaoPrometeTitularidade() {
            notifier.conviteDeAnimal(code(), marcelo(), "ana@exemplo.com", PetTutorRole.EDITOR,
                    "TOKEN-123", 7);

            assertThat(corpo(enviada())).doesNotContain("responde pelo Code");
        }

        /*
         * "Nada acontece sem voce aceitar" precisa estar no e-mail que carrega um codigo: quem
         * recebe nao pediu nada, e um codigo sem essa linha parece uma cobranca.
         */
        @Test
        @DisplayName("diz que nada acontece sem o aceite")
        void dizQueNadaAconteceSozinho() {
            notifier.conviteDeAnimal(code(), marcelo(), "ana@exemplo.com", PetTutorRole.EDITOR,
                    "TOKEN-123", 7);

            assertThat(corpo(enviada())).contains("Nada acontece sem voce aceitar");
        }

        @Test
        @DisplayName("sai calado quando nao ha endereco a que escrever")
        void semEnderecoNaoEscreve() {
            notifier.conviteDeAnimal(code(), marcelo(), null, PetTutorRole.EDITOR, "TOKEN-123", 7);
            notifier.conviteDeAnimal(code(), marcelo(), "   ", PetTutorRole.EDITOR, "TOKEN-123", 7);

            verifyNoInteractions(dispatcher);
        }
    }

    @Nested
    @DisplayName("conviteDeOrganizacao")
    class ConviteDeOrganizacao {

        /*
         * Entrar como veterinaria e entrar como voluntaria dao poderes diferentes sobre o prontuario
         * de animais que nao sao seus, e quem aceita tem de saber qual dos dois esta aceitando.
         */
        @Test
        @DisplayName("diz a funcao, e nao so o nome da organizacao")
        void dizAFuncao() {
            notifier.conviteDeOrganizacao(vilaNova(), marcelo(), "ana@exemplo.com",
                    MembershipRole.VETERINARIO, "TOKEN-456", 7);

            Notification notificacao = enviada();

            assertThat(notificacao.getSubject()).contains("Clinica Vila Nova");
            assertThat(corpo(notificacao)).contains("veterinaria").contains("Codigo: TOKEN-456");
        }

        @Test
        @DisplayName("a autoria e da pessoa, e o e-mail diz isso antes do aceite")
        void autoriaEDaPessoa() {
            notifier.conviteDeOrganizacao(vilaNova(), marcelo(), "ana@exemplo.com",
                    MembershipRole.MONITOR, "TOKEN-456", 7);

            assertThat(corpo(enviada())).contains("leva o seu nome");
        }

        /*
         * O convite aberto — sem endereco — existe para ser entregue por outro caminho. O
         * notificador nao tem a quem escrever, e sair calado e o comportamento certo.
         */
        @Test
        @DisplayName("o convite sem endereco nao gera e-mail nenhum")
        void conviteAbertoNaoEscreve() {
            notifier.conviteDeOrganizacao(vilaNova(), marcelo(), null, MembershipRole.MONITOR,
                    "TOKEN-456", 7);

            verifyNoInteractions(dispatcher);
        }

        @Test
        @DisplayName("a funcao nula nao quebra o e-mail")
        void funcaoNulaNaoQuebra() {
            notifier.conviteDeOrganizacao(vilaNova(), marcelo(), "ana@exemplo.com", null,
                    "TOKEN-456", 7);

            assertThat(corpo(enviada())).contains("membro da equipe");
        }
    }
}

package br.com.petfy.healthcare.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * Um aviso, dois canais — e a ordem entre eles nao e detalhe.
 */
@ExtendWith(MockitoExtension.class)
class CanaisTest {

    @Mock
    private Notifier externo;

    @Mock
    private InAppNotifier inApp;

    private Notification aviso() {
        return Notification.builder()
                .toEmail("ana@petfy.com.br")
                .subject("Marcelo entrou no Code")
                .lines(List.of("Marcelo passou a cuidar do Code com voce."))
                .build();
    }

    /*
     * O in-app e uma escrita no mesmo banco; o externo e uma chamada HTTP a um terceiro. Invertida,
     * a ordem faria a pessoa perder os DOIS avisos por causa do canal mais fragil.
     */
    @Test
    @DisplayName("guarda o aviso antes de tentar o canal externo")
    void inAppPrimeiro() {
        new Canais(externo, inApp).send(aviso(), "tutor que entrou no animal");

        var ordem = inOrder(inApp, externo);
        ordem.verify(inApp).registrar(any(), anyString());
        ordem.verify(externo).send(any());
    }

    @Test
    @DisplayName("a falha do in-app nao impede o e-mail")
    void falhaDoInAppNaoCalaOExterno() {
        doThrow(new RuntimeException("banco fora")).when(inApp).registrar(any(), anyString());

        assertThatCode(() -> new Canais(externo, inApp).send(aviso(), "evento"))
                .doesNotThrowAnyException();

        verify(externo).send(any());
    }

    /*
     * A falha do externo SOBE, e nao e esquecimento: o dispatcher assincrono ja a captura e loga, e
     * o lembrete de vacina depende dela subindo para o rollback desmarcar a dose — senao ficaria
     * registrada como avisada uma dose que ninguem recebeu.
     */
    @Test
    @DisplayName("a falha do canal externo continua subindo, e o aviso in-app ja esta gravado")
    void falhaDoExternoSobe() {
        doThrow(new RuntimeException("resend fora")).when(externo).send(any());

        assertThatCode(() -> new Canais(externo, inApp).send(aviso(), "evento"))
                .isInstanceOf(RuntimeException.class);

        verify(inApp).registrar(any(), anyString());
    }
}

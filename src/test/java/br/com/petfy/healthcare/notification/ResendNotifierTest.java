package br.com.petfy.healthcare.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendNotifierTest {

    private ResendNotifier notifier;
    private MockRestServiceServer servidor;

    @BeforeEach
    void setUp() {
        notifier = new ResendNotifier(new RestTemplateBuilder(), "chave-de-teste", "onboarding@resend.dev");

        // o RestTemplate e montado no construtor, entao o mock e ligado a ele
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(notifier, "restTemplate");
        servidor = MockRestServiceServer.createServer(restTemplate);
    }

    private Notification notificacao() {
        return Notification.builder()
                .toEmail("tutor@petfy.com.br")
                .toName("Ulysses")
                .subject("Confirme seu e-mail - Petfy")
                .lines(List.of("Codigo: abc123", "Ele vale por 24 horas."))
                .build();
    }

    @Test
    @DisplayName("deve postar no endpoint do Resend com a chave no header e o destinatario em lista")
    void devePostarNoEndpointDoResend() {
        servidor.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-de-teste"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.from").value("onboarding@resend.dev"))
                .andExpect(jsonPath("$.to[0]").value("tutor@petfy.com.br"))
                .andExpect(jsonPath("$.subject").value("Confirme seu e-mail - Petfy"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        notifier.send(notificacao());

        servidor.verify();
    }

    @Test
    @DisplayName("o corpo deve levar a saudacao e todas as linhas da mensagem")
    void corpoDeveLevarSaudacaoELinhas() {
        servidor.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.text").value(
                        "Ola, Ulysses!\n\nCodigo: abc123\nEle vale por 24 horas."))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        notifier.send(notificacao());

        servidor.verify();
    }

    /**
     * A excecao precisa subir, e nao ser engolida aqui: quem chama e que decide.
     * O aviso ao tutor captura e segue, mas o lembrete depende dela subir para o
     * rollback manter a dose elegivel na proxima varredura.
     */
    @Test
    @DisplayName("falha do Resend deve subir para quem chamou")
    void falhaDoResendDeveSubir() {
        servidor.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> notifier.send(notificacao()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tutor@petfy.com.br");
    }

    /**
     * A mensagem carrega token de recuperacao de senha e de confirmacao de
     * e-mail. Ele nao pode vazar para o log por causa de uma falha de envio.
     */
    @Test
    @DisplayName("a mensagem de erro nao deve conter o corpo do e-mail")
    void mensagemDeErroNaoDeveConterCorpo() {
        servidor.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> notifier.send(notificacao()))
                .hasMessageNotContaining("abc123");
    }

    @Test
    @DisplayName("resposta de sucesso nao deve gerar excecao")
    void respostaDeSucessoNaoDeveGerarExcecao() {
        servidor.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> notifier.send(notificacao())).doesNotThrowAnyException();
    }

}

package br.com.petfy.healthcare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Envio por API HTTP, ativado com petfy.notifications.channel=resend.
 *
 * Existe porque o provedor de hospedagem bloqueia saida em porta de SMTP: a
 * conexao com smtp.gmail.com:587 nunca se estabelece, e nenhuma configuracao
 * vence bloqueio de porta. HTTPS passa.
 *
 * O canal continua sem conhecer o dominio - recebe uma Notification pronta,
 * como o log e o SMTP. Trocar o transporte nao encostou em nenhuma regra de
 * negocio, que era a razao de essa abstracao existir.
 */
@Slf4j
@Component
@org.springframework.beans.factory.annotation.Qualifier("canalExterno")
@ConditionalOnProperty(name = "petfy.notifications.channel", havingValue = "resend")
public class ResendNotifier implements Notifier {

    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String remetente;

    public ResendNotifier(RestTemplateBuilder builder,
                          @Value("${petfy.notifications.resend.api-key:}") String apiKey,
                          @Value("${petfy.notifications.from:onboarding@resend.dev}") String remetente) {
        // timeouts explicitos pela mesma razao do SMTP: sem eles uma chamada
        // pendurada prenderia uma thread do pool de envio ate a aplicacao
        // reiniciar, e o pool tem quatro
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.remetente = remetente;
    }

    /**
     * Deixa a excecao subir em caso de falha, e nao apenas loga. Quem chama e que
     * decide o que fazer: o aviso ao tutor captura e segue, enquanto o lembrete
     * precisa que ela suba para o rollback manter a dose elegivel.
     */
    @Override
    public void send(Notification notification) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> corpo = Map.of(
                "from", remetente,
                "to", List.of(notification.getToEmail()),
                "subject", notification.getSubject(),
                "text", notification.saudacao() + "\n\n" + String.join("\n", notification.getLines()));

        try {
            restTemplate.postForEntity(ENDPOINT, new HttpEntity<>(corpo, headers), String.class);
        } catch (RestClientException e) {
            // o endereco entra no log, o corpo nao: a mensagem carrega token de
            // recuperacao de senha e de confirmacao de e-mail
            throw new IllegalStateException(
                    "Falha ao enviar e-mail para " + notification.getToEmail() + " pelo Resend", e);
        }
    }

}

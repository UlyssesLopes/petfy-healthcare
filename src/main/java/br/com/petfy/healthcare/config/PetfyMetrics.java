package br.com.petfy.healthcare.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Metricas custom do Petfy.
 *
 * Registradas aqui de forma centralizada para que o nome e as tags
 * nao fiquem espalhados em varios services — uma alteracao de nome fica
 * num so lugar.
 *
 * Contadores escolhidos:
 *   petfy.vaccines.reminders.sent   - lembretes enviados com sucesso (tag: channel)
 *   petfy.vaccines.reminders.failed - lembretes que falharam (tag: channel)
 *   petfy.auth.login.attempts       - tentativas de login (tag: outcome=success|failure)
 *
 * Timers HTTP nao sao criados aqui: o Actuator ja os registra automaticamente
 * via WebMvcMetricsFilter sob a metrica http.server.requests.
 */
@Component
public class PetfyMetrics {

    private final MeterRegistry registry;

    public PetfyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void reminderSent(String channel) {
        Counter.builder("petfy.vaccines.reminders.sent")
                .description("Lembretes de vacina enviados com sucesso")
                .tag("channel", channel)
                .register(registry)
                .increment();
    }

    public void reminderFailed(String channel) {
        Counter.builder("petfy.vaccines.reminders.failed")
                .description("Lembretes de vacina que falharam ao ser enviados")
                .tag("channel", channel)
                .register(registry)
                .increment();
    }

    public void loginAttempt(String outcome) {
        Counter.builder("petfy.auth.login.attempts")
                .description("Tentativas de autenticacao")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }
}

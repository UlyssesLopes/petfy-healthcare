package br.com.petfy.healthcare.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Condicionado a mesma propriedade do scheduler: sem lembretes ligados nao ha
 * nada agendado, entao nao faz sentido subir o pool de agendamento.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "petfy.reminders.enabled", havingValue = "true")
public class SchedulingConfig {
}

package br.com.petfy.healthcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Habilita o envio de notificacao fora da requisicao.
 *
 * Nao e condicionado a petfy.reminders.enabled como o SchedulingConfig: o aviso
 * de vacina registrada sai no momento do registro e nao depende do agendador,
 * entao ele existe mesmo com os lembretes desligados.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool proprio e limitado, em vez do executor default do Spring, que cria uma
     * thread por chamada sem teto. Uma clinica registrando vacina em lote nao
     * pode virar uma thread por registro.
     *
     * A politica de rejeicao e CallerRuns de proposito: quando a fila enche, o
     * envio volta a acontecer na thread de quem chamou. Isso devolve a latencia
     * ao veterinario nos momentos de pico, o que e ruim, mas melhor do que
     * descartar em silencio um aviso ao tutor.
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("petfy-notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // espera o envio em andamento terminar antes de derrubar a aplicacao, para
        // um deploy nao cortar um aviso no meio
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        executor.initialize();
        return executor;
    }

}

package br.com.petfy.healthcare.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * As conexoes abertas por pessoa, para o aviso chegar na hora.
 *
 * <p>Uma lista por pessoa, e nao uma conexao: a mesma conta pode estar aberta em varias abas.
 */
@Slf4j
@Component
public class AvisoStream {

    /** 30 min. Depois disso o navegador reconecta — conexao eterna vaza quando o cliente some. */
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<UUID, List<SseEmitter>> porPessoa = new ConcurrentHashMap<>();

    public SseEmitter abrir(UUID personId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        porPessoa.computeIfAbsent(personId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remover(personId, emitter));
        emitter.onTimeout(() -> remover(personId, emitter));
        emitter.onError(erro -> remover(personId, emitter));

        return emitter;
    }

    /** Avisa que ha algo novo. O cliente recarrega a contagem — o payload nao carrega dado. */
    public void publicar(UUID personId) {
        for (SseEmitter emitter : porPessoa.getOrDefault(personId, List.of())) {
            try {
                emitter.send(SseEmitter.event().name("aviso").data("novo"));
            } catch (IOException | IllegalStateException e) {
                // aba fechada sem avisar: o `remover` do callback pode nao ter rodado ainda
                remover(personId, emitter);
            }
        }
    }

    private void remover(UUID personId, SseEmitter emitter) {
        List<SseEmitter> abertos = porPessoa.get(personId);

        if (abertos == null) {
            return;
        }

        abertos.remove(emitter);

        if (abertos.isEmpty()) {
            porPessoa.remove(personId);
        }
    }

    /** Quantas conexoes abertas — para teste e diagnostico. */
    public int abertas(UUID personId) {
        return porPessoa.getOrDefault(personId, List.of()).size();
    }

}

package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roda antes da cadeia do Spring Security — incluindo o JwtAuthenticationFilter.
 * Isso evita gastar validacao de token num ataque de flood que seria bloqueado
 * aqui de qualquer jeito.
 *
 * <b>Estende OncePerRequestFilter, e nao Filter, e isso nao e estilo.</b> Um
 * Filter anotado com @Component e registrado duas vezes: uma pelo Boot na cadeia
 * do servlet container, outra aqui na SecurityFilterChain. Rodando duas vezes
 * por requisicao, cada chamada consumiria dois tokens do balde e o limite real
 * seria metade do configurado - 5 tentativas de login por minuto onde a
 * property diz 10. O JwtAuthenticationFilter vizinho ja estende
 * OncePerRequestFilter pelo mesmo motivo.
 *
 * Cache em memoria: a aplicacao roda em instancia unica (ROADMAP). Sem Redis
 * ou Hazelcast. Se a instancia reiniciar os baldes sao zerados, o que e
 * aceitavel dado o contexto. <b>Escalar para duas instancias divide o limite
 * entre elas</b>, do mesmo jeito que reintroduz o lembrete duplicado.
 *
 * Dois grupos de limite, configurados por properties com defaults sensiveis:
 *   - login: 10 tentativas / minuto / IP
 *   - cadastro/reset/verificacao: 5 requests / minuto / IP
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /* endpoints que entram no grupo "login" */
    private static final Set<String> ENDPOINTS_LOGIN = Set.of("/auth/login");

    /* endpoints que entram no grupo "cadastro/recuperacao" */
    private static final Set<String> ENDPOINTS_RESTRITO = Set.of(
            "/persons",
            "/vets",
            "/auth/password-reset",
            "/auth/email-verification/resend"
    );

    private final int loginCapacity;
    private final int restritoCapacity;

    /* um balde por (IP + grupo). A chave e "grupo:ip" */
    private final Map<String, Bucket> baldes = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            @Value("${petfy.rate-limit.login-por-minuto:10}") int loginCapacity,
            @Value("${petfy.rate-limit.restrito-por-minuto:5}") int restritoCapacity) {
        this.loginCapacity = loginCapacity;
        this.restritoCapacity = restritoCapacity;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Bucket balde = resolverBalde(request.getRequestURI(), resolverIp(request));

        if (balde == null) {
            // rota nao limitada
            chain.doFilter(request, response);
            return;
        }

        if (balde.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            responder429(response);
        }
    }

    private Bucket resolverBalde(String path, String ip) {
        if (ENDPOINTS_LOGIN.contains(path)) {
            return baldes.computeIfAbsent("login:" + ip, k -> criarBalde(loginCapacity));
        }
        if (ENDPOINTS_RESTRITO.contains(path)) {
            return baldes.computeIfAbsent("restrito:" + ip, k -> criarBalde(restritoCapacity));
        }
        return null;
    }

    private Bucket criarBalde(int capacidade) {
        Bandwidth limite = Bandwidth.builder()
                .capacity(capacidade)
                .refillIntervally(capacidade, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    private String resolverIp(HttpServletRequest request) {
        // X-Forwarded-For vem do proxy/load balancer; fallback para remoteAddr
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void responder429(HttpServletResponse response) throws IOException {
        ErrorResponse body = new ErrorResponse(
                "Muitas requisicoes. Tente novamente em breve.",
                429,
                429,
                LocalDateTime.now());

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}

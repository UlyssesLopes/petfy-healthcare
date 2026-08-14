package br.com.petfy.healthcare.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * O cookie que carrega o refresh, e as tres decisoes que ele obriga a tomar.
 *
 * <b>httpOnly, e e o ponto inteiro.</b> O JWT continua vivendo em memoria no cliente porque
 * `localStorage` e exposicao a XSS — a decisao esta no `ROADMAP.md` e no `sessao.ts`. Um cookie
 * httpOnly nao e lido por JavaScript nenhum, entao o refresh sobrevive a recarga sem ficar ao
 * alcance de um script injetado. E o unico jeito de a sessao durar sem afrouxar aquela regra.
 *
 * ------------------------------------------------------------------------- por que Path=/auth
 *
 * <b>O cookie so viaja para as rotas que precisam dele.</b> Toda requisicao do produto leva o JWT no
 * header; nenhuma delas tem o que fazer com o refresh. Mandar o cookie em todas seria dar a cada
 * endpoint a chance de vaza-lo num log.
 *
 * ------------------------------------------------------------------ por que SameSite e config
 *
 * Localmente o front esta em `localhost:5173` e a API em `localhost:8080`: origens diferentes, mas
 * <b>o mesmo site</b> — a porta nao entra na conta de same-site. Entao `Lax` funciona, e e o default.
 *
 * Em producao, se o front e a API ficarem em sites diferentes, so `None` funciona — e `None` exige
 * `Secure`, que exige HTTPS. <b>Isso e configuracao de deploy, e nao decisao de codigo</b>, e e
 * exatamente o gatilho que o `sessao.ts` cita: "o dominio proprio pondo front e API sob o mesmo
 * site". Com o mesmo site, `Lax` continua bastando e o cookie fica mais protegido.
 */
@Component
public class RefreshCookie {

    public static final String NOME = "petfy_refresh";

    private static final String CAMINHO = "/auth";

    private final Duration validade;
    private final String sameSite;
    private final boolean secure;

    public RefreshCookie(@Value("${petfy.refresh.expiration-days:30}") long validadeEmDias,
                         @Value("${petfy.refresh.cookie-same-site:Lax}") String sameSite,
                         @Value("${petfy.refresh.cookie-secure:false}") boolean secure) {
        this.validade = Duration.ofDays(validadeEmDias);
        this.sameSite = sameSite;
        this.secure = secure;
    }

    public Duration getValidade() {
        return validade;
    }

    /** O cookie que leva o refresh novo. */
    public String paraDefinir(String token) {
        return ResponseCookie.from(NOME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(CAMINHO)
                .maxAge(validade)
                .build()
                .toString();
    }

    /**
     * O cookie que apaga o anterior.
     *
     * <b>Mesmo nome, mesmo caminho e maxAge zero</b> — sem os tres iguais o navegador guarda um
     * segundo cookie em vez de substituir o primeiro, e a pessoa sai da conta com o refresh ainda
     * no disco.
     */
    public String paraApagar() {
        return ResponseCookie.from(NOME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(CAMINHO)
                .maxAge(0)
                .build()
                .toString();
    }

    public Optional<String> ler(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> NOME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(valor -> valor != null && !valor.isBlank())
                .findFirst();
    }

    /** O nome do header, para quem monta a resposta. */
    public String header() {
        return HttpHeaders.SET_COOKIE;
    }

}

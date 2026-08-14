package br.com.petfy.healthcare.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O cookie do refresh, e a unica diferenca que "continuar conectado" produz.
 *
 * A escolha nao muda o prazo do servidor — muda se o navegador guarda o cookie no disco ou o
 * descarta ao fechar.
 */
class RefreshCookieTest {

    private final RefreshCookie cookie = new RefreshCookie(30, "Lax", false);

    @Test
    @DisplayName("marcado, o cookie leva Max-Age e sobrevive a fechar o navegador")
    void persistenteLevaMaxAge() {
        assertThat(cookie.paraDefinir("token-abc", true))
                .contains("Max-Age=" + (30 * 24 * 60 * 60))
                .contains("Expires=");
    }

    /**
     * Sem {@code Max-Age} e sem {@code Expires} o navegador trata como cookie de sessao. E o
     * unico jeito de a caixa desmarcada ter efeito num computador emprestado.
     */
    @Test
    @DisplayName("desmarcado, o cookie morre com o navegador")
    void naoPersistenteNaoLevaPrazo() {
        assertThat(cookie.paraDefinir("token-abc", false))
                .doesNotContain("Max-Age")
                .doesNotContain("Expires");
    }

    /** O que protege o refresh nao depende da escolha, e vale nos dois casos. */
    @Test
    @DisplayName("httpOnly e Path=/auth valem nos dois casos")
    void protecoesValemNosDoisCasos() {
        assertThat(cookie.paraDefinir("token-abc", true))
                .contains("HttpOnly").contains("Path=/auth").contains("SameSite=Lax");
        assertThat(cookie.paraDefinir("token-abc", false))
                .contains("HttpOnly").contains("Path=/auth").contains("SameSite=Lax");
    }

    @Test
    @DisplayName("apagar usa o mesmo nome e caminho, para substituir em vez de acumular")
    void apagarCasaComOQueFoiDefinido() {
        assertThat(cookie.paraApagar())
                .contains(RefreshCookie.NOME + "=")
                .contains("Path=/auth")
                .contains("Max-Age=0");
    }

}

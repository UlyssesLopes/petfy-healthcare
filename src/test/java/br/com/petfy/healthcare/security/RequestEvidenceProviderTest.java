package br.com.petfy.healthcare.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class RequestEvidenceProviderTest {

    private final RequestEvidenceProvider provider = new RequestEvidenceProvider();

    private MockHttpServletRequest emRequisicao() {
        var request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("sem requisicao devolve nulo em vez de lancar")
    void semRequisicaoDevolveNulo() {
        // a rotina de lembrete roda em scheduler: um aceite gravado por caminho sem
        // HTTP nao pode falhar por nao ter de onde tirar IP
        assertThat(provider.ip()).isNull();
        assertThat(provider.userAgent()).isNull();
    }

    @Test
    @DisplayName("sem X-Forwarded-For usa o endereco remoto")
    void semCabecalhoUsaRemoteAddr() {
        emRequisicao().setRemoteAddr("192.168.0.10");

        assertThat(provider.ip()).isEqualTo("192.168.0.10");
    }

    /**
     * O provedor de hospedagem termina o TLS e encaminha, entao {@code getRemoteAddr}
     * sozinho devolveria o IP do proxy - a mesma evidencia para todo mundo, o que e o
     * mesmo que nao ter evidencia.
     */
    @Test
    @DisplayName("com X-Forwarded-For usa o primeiro da cadeia, que e o cliente")
    void usaOPrimeiroDaCadeia() {
        var request = emRequisicao();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 70.41.3.18, 10.0.0.1");

        assertThat(provider.ip()).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("cabecalho vazio nao mascara o endereco remoto")
    void cabecalhoVazioCaiNoRemoteAddr() {
        var request = emRequisicao();
        request.setRemoteAddr("192.168.0.10");
        request.addHeader("X-Forwarded-For", "   ");

        assertThat(provider.ip()).isEqualTo("192.168.0.10");
    }

    @Test
    @DisplayName("IPv6 completo cabe no limite da coluna")
    void ipv6Cabe() {
        emRequisicao().setRemoteAddr("2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        assertThat(provider.ip()).hasSizeLessThanOrEqualTo(45);
    }

    @Test
    @DisplayName("devolve o user agent quando presente")
    void devolveUserAgent() {
        emRequisicao().addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0)");

        assertThat(provider.userAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0)");
    }

    /**
     * Trunca em vez de deixar o banco recusar: user agent nao tem limite pratico, e
     * coluna estourada faria a criacao de conta falhar por causa de um navegador
     * verborragico.
     */
    @Test
    @DisplayName("user agent longo e truncado no limite da coluna")
    void userAgentLongoETruncado() {
        emRequisicao().addHeader("User-Agent", "x".repeat(900));

        assertThat(provider.userAgent()).hasSize(512);
    }

    @Test
    @DisplayName("cabecalho ausente devolve nulo, e nao string vazia")
    void ausenteDevolveNulo() {
        emRequisicao();

        assertThat(provider.userAgent()).isNull();
    }

}

package br.com.petfy.healthcare.security;

import br.com.petfy.healthcare.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa o rate limiting em memoria sem subir o contexto Spring.
 *
 * Cada teste cria um novo filtro para garantir baldes zerados,
 * independente da ordem de execucao.
 */
class RateLimitFilterTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        // capacidade minima para os testes passarem rapido
        filtro = new RateLimitFilter(2, 2);
    }

    @Test
    @DisplayName("requisicoes dentro do limite passam com 200 para /auth/login")
    void dentroDoLimitePassam() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = postPara("/auth/login", "10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("apos esgotar o limite, /auth/login responde 429")
    void aposLimiteRecebe429() throws Exception {
        for (int i = 0; i < 2; i++) {
            filtro.doFilter(postPara("/auth/login", "10.0.0.2"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(postPara("/auth/login", "10.0.0.2"), res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);

        ErrorResponse body = mapper.readValue(res.getContentAsString(), ErrorResponse.class);
        assertThat(body.getStatus()).isEqualTo(429);
        assertThat(body.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("apos esgotar o limite, /owners responde 429")
    void ownersResponde429() throws Exception {
        for (int i = 0; i < 2; i++) {
            filtro.doFilter(postPara("/owners", "10.0.0.3"), new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(postPara("/owners", "10.0.0.3"), res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("IPs diferentes tem baldes independentes")
    void ipsDiferentesNaoCompartilhamBalde() throws Exception {
        // ip A esgota o balde
        for (int i = 0; i < 2; i++) {
            filtro.doFilter(postPara("/auth/login", "1.1.1.1"), new MockHttpServletResponse(), new MockFilterChain());
        }

        // ip B ainda tem balde cheio
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(postPara("/auth/login", "2.2.2.2"), res, new MockFilterChain());

        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    @DisplayName("GET nao e limitado mesmo no path de login")
    void getNaoELimitado() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/auth/login");
            req.setRemoteAddr("10.0.0.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    @Test
    @DisplayName("endpoint autenticado (ex: /pets) nao e limitado")
    void endpointAutenticadoNaoELimitado() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filtro.doFilter(postPara("/pets", "10.0.0.5"), res, new MockFilterChain());
            assertThat(res.getStatus()).isNotEqualTo(429);
        }
    }

    private MockHttpServletRequest postPara(String path, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", path);
        req.setRemoteAddr(ip);
        return req;
    }
}

package br.com.petfy.healthcare.config;

import br.com.petfy.healthcare.controller.AnimalController;
import br.com.petfy.healthcare.domain.repository.ProfessionalCredentialRepository;
import br.com.petfy.healthcare.security.JwtAuthenticationFilter;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.ProfessionalAccessManager;
import br.com.petfy.healthcare.security.TokenFreshness;
import br.com.petfy.healthcare.service.AnimalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O CORS e a unica configuracao do backend que so falha no navegador: teste de
 * API por curl passa com ele quebrado, e o sintoma aparece na primeira tela do
 * front. Por isso ele tem teste proprio, e por isso o teste checa o preflight e
 * nao so a requisicao simples.
 */
@WebMvcTest(controllers = AnimalController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, JwtService.class,
        ProfessionalAccessManager.class})
@TestPropertySource(properties = {
        "petfy.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "petfy.jwt.expiration-minutes=120",
        "petfy.cors.allowed-origins=http://localhost:5173"
})
class CorsConfigTest {

    private static final String ORIGEM_DO_FRONT = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProfessionalCredentialRepository professionalCredentialRepository;

    @MockBean
    private AnimalService animalService;

    @MockBean
    private TokenFreshness tokenFreshness;

    private String tokenValido() {
        return jwtService.generateToken("ulysses@petfy.com.br", UUID.randomUUID());
    }

    /**
     * O preflight nao carrega credencial nenhuma - o navegador nao a envia. Se ele
     * chegasse ate a autorizacao tomaria 401, e a requisicao de verdade nem
     * chegaria a sair: a tela ficaria em branco com erro de CORS no console,
     * enquanto o token estava correto o tempo todo.
     */
    @Test
    @DisplayName("o preflight de rota protegida nao pode tomar 401 - ele nunca carrega token")
    void preflightDeRotaProtegidaNaoTomaUnauthorized() throws Exception {
        mockMvc.perform(options("/animals")
                        .header("Origin", ORIGEM_DO_FRONT)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_DO_FRONT));
    }

    /**
     * O header do contexto ativo e customizado, entao e ele que obriga o preflight a
     * existir. Fora da lista de aceitos, a area de organizacao inteira para de
     * funcionar no navegador.
     */
    @Test
    @DisplayName("o preflight deve aceitar o header de contexto X-Petfy-Organization")
    void preflightDeveAceitarOHeaderDeContexto() throws Exception {
        mockMvc.perform(options("/animals")
                        .header("Origin", ORIGEM_DO_FRONT)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization, X-Petfy-Organization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_DO_FRONT));
    }

    @Test
    @DisplayName("origem desconhecida nao recebe permissao no preflight")
    void origemDesconhecidaNaoRecebePermissao() throws Exception {
        mockMvc.perform(options("/animals")
                        .header("Origin", "https://site-de-terceiro.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /**
     * O download de anexo passa pela API autenticada, e o nome do arquivo chega em
     * Content-Disposition. Ele nao esta entre os headers que o navegador entrega ao
     * JS por padrao: sem esta exposicao, todo anexo baixado salva com nome errado.
     */
    @Test
    @DisplayName("Content-Disposition deve ser exposto, senao o anexo baixa com nome errado")
    void contentDispositionDeveSerExposto() throws Exception {
        when(animalService.listAllAnimals(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/animals")
                        .header("Origin", ORIGEM_DO_FRONT)
                        .header("Authorization", "Bearer " + tokenValido()))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGEM_DO_FRONT))
                .andExpect(header().string("Access-Control-Expose-Headers", "Content-Disposition"));
    }

    /**
     * A API responde com credencial: origem coringa deixaria qualquer site aberto no
     * mesmo navegador ler a resposta. Recusar na subida segue o criterio do
     * JWT_SECRET - nao subir e melhor do que subir errado -, e transforma a regra
     * escrita no ROADMAP em algo que o codigo cobra.
     */
    @Test
    @DisplayName("'*' deve derrubar a subida, e nao virar configuracao aceita em silencio")
    void coringaDeveDerrubarASubida() {
        assertThatThrownBy(() -> new CorsConfig(List.of("*")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credencial");
    }

    /**
     * Fecha por omissao: ambiente que esquecer de declarar a origem nao libera
     * ninguem. O oposto - abrir por omissao - seria o tipo de default que so se
     * descobre depois.
     */
    @Test
    @DisplayName("sem origem declarada, nenhuma origem e liberada")
    void semOrigemDeclaradaNinguemEhLiberado() {
        var configuracao = configuracaoDe(new CorsConfig(List.of("", "  ")));

        assertThat(configuracao.getAllowedOrigins()).isEmpty();
        assertThat(configuracao.checkOrigin(ORIGEM_DO_FRONT)).isNull();
        assertThat(configuracao.checkOrigin("https://site-de-terceiro.example")).isNull();
    }

    @Test
    @DisplayName("credencial de cookie continua desligada enquanto o token viaja no header")
    void allowCredentialsSegueDesligado() {
        var configuracao = configuracaoDe(new CorsConfig(List.of(ORIGEM_DO_FRONT)));

        assertThat(configuracao.getAllowCredentials()).isFalse();
        assertThat(configuracao.getAllowedOrigins()).containsExactly(ORIGEM_DO_FRONT);
    }

    private CorsConfiguration configuracaoDe(CorsConfig config) {
        return ((UrlBasedCorsConfigurationSource) config.corsConfigurationSource())
                .getCorsConfigurations()
                .get("/**");
    }

}

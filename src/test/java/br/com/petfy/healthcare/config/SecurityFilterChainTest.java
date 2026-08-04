package br.com.petfy.healthcare.config;

import br.com.petfy.healthcare.controller.AuthController;
import br.com.petfy.healthcare.controller.OwnerController;
import br.com.petfy.healthcare.controller.PetController;
import br.com.petfy.healthcare.controller.PetShareController;
import br.com.petfy.healthcare.controller.SharedCardController;
import br.com.petfy.healthcare.domain.dto.SharedVaccineCardDTO;
import br.com.petfy.healthcare.service.PetShareService;
import br.com.petfy.healthcare.security.JwtAuthenticationFilter;
import br.com.petfy.healthcare.security.JwtService;
import br.com.petfy.healthcare.security.TokenFreshness;
import br.com.petfy.healthcare.security.UserRole;
import br.com.petfy.healthcare.service.AuthService;
import br.com.petfy.healthcare.service.OwnerService;
import br.com.petfy.healthcare.service.PasswordResetService;
import br.com.petfy.healthcare.service.PetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os demais testes de controller usam standaloneSetup, que nao monta a cadeia de
 * filtros - eles passariam mesmo se a seguranca estivesse desligada. Este sobe a
 * SecurityFilterChain de verdade para conferir o que e publico e o que exige
 * token.
 */
@WebMvcTest(controllers = {AuthController.class, PetController.class, OwnerController.class,
        SharedCardController.class, PetShareController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "petfy.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "petfy.jwt.expiration-minutes=120"
})
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PetService petService;

    @MockBean
    private OwnerService ownerService;

    @MockBean
    private PetShareService petShareService;

    @MockBean
    private PasswordResetService passwordResetService;

    /**
     * Mockado porque a checagem de token anterior a troca de senha consulta o
     * banco, que nao existe neste slice. O default do Mockito - false - e o
     * comportamento normal: token nao ficou para tras.
     */
    @MockBean
    private TokenFreshness tokenFreshness;

    private String tokenValido() {
        return jwtService.generateToken("ulysses@petfy.com.br", UserRole.OWNER, UUID.randomUUID());
    }

    @Test
    @DisplayName("deve responder 401 e nao chamar o service em rota protegida sem token")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(get("/pets/all"))
                .andExpect(status().isUnauthorized());

        verify(petService, never()).listAllPets();
    }

    @Test
    @DisplayName("deve responder 401 quando o token e invalido")
    void deveResponder401ComTokenInvalido() throws Exception {
        mockMvc.perform(get("/pets/all").header("Authorization", "Bearer token-falsificado"))
                .andExpect(status().isUnauthorized());

        verify(petService, never()).listAllPets();
    }

    @Test
    @DisplayName("deve responder 401 quando o header nao usa o esquema Bearer")
    void deveResponder401SemEsquemaBearer() throws Exception {
        mockMvc.perform(get("/pets/all").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve liberar a rota protegida quando o token e valido")
    void deveLiberarComTokenValido() throws Exception {
        when(petService.listAllPets()).thenReturn(List.of());

        mockMvc.perform(get("/pets/all").header("Authorization", "Bearer " + tokenValido()))
                .andExpect(status().isOk());

        verify(petService).listAllPets();
    }

    @Test
    @DisplayName("o login deve ser publico")
    void loginDeveSerPublico() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ulysses@petfy.com.br\",\"password\":\"s3nhaForte\"}"))
                .andExpect(status().isOk());

        verify(authService).login(any());
    }

    @Test
    @DisplayName("o cadastro de owner deve ser publico, senao nao existe primeiro usuario")
    void cadastroDeOwnerDeveSerPublico() throws Exception {
        mockMvc.perform(post("/owners/include")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ulysses\",\"email\":\"ulysses@petfy.com.br\",\"password\":\"s3nhaForte\"}"))
                .andExpect(status().isCreated());

        verify(ownerService).createOwner(any());
    }

    @Test
    @DisplayName("a carteira compartilhada deve ser publica - quem recebe o link nao tem conta")
    void carteiraCompartilhadaDeveSerPublica() throws Exception {
        when(petShareService.viewSharedCard("um-token")).thenReturn(SharedVaccineCardDTO.builder().petName("Rex").build());

        mockMvc.perform(get("/share/{token}", "um-token"))
                .andExpect(status().isOk());

        verify(petShareService).viewSharedCard("um-token");
    }

    @Test
    @DisplayName("criar link de compartilhamento deve exigir token - so o tutor compartilha")
    void criarLinkDeveExigirToken() throws Exception {
        mockMvc.perform(post("/pets/{petId}/shares", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());

        verify(petShareService, never()).createShare(any(), any());
    }

    @Test
    @DisplayName("as demais rotas de owner devem continuar exigindo token")
    void demaisRotasDeOwnerDevemExigirToken() throws Exception {
        mockMvc.perform(get("/owners/me"))
                .andExpect(status().isUnauthorized());

        verify(ownerService, never()).getCurrentOwner();
    }

    /**
     * O health check e chamado pelo provedor de hospedagem, sem credencial: se a
     * cadeia exigir token, o deploy e considerado morto e reiniciado em loop.
     * <p>
     * O actuator nao entra neste slice de @WebMvcTest, entao o endpoint nao
     * existe aqui e a resposta e 404. E exatamente essa a distincao que importa:
     * 404 significa que a cadeia deixou passar e nao havia controller atras,
     * enquanto 401 significaria que a cadeia barrou antes de chegar la.
     */
    /**
     * Quem esqueceu a senha nao tem como se autenticar para pedir a troca: se
     * estas rotas exigissem token, a recuperacao so serviria a quem ja consegue
     * entrar, ou seja, a quem nao precisa dela.
     */
    @Test
    @DisplayName("pedir recuperacao de senha deve ser publico")
    void pedirRecuperacaoDeSenhaDeveSerPublico() throws Exception {
        mockMvc.perform(post("/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ulysses@petfy.com.br\"}"))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset(any());
    }

    @Test
    @DisplayName("confirmar recuperacao de senha deve ser publico")
    void confirmarRecuperacaoDeSenhaDeveSerPublico() throws Exception {
        mockMvc.perform(post("/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"um-token\",\"newPassword\":\"s3nhaNovaForte\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).confirmReset(any());
    }

    /**
     * Token intacto - assinado, dentro da validade - mas emitido antes de uma
     * troca de senha. Se ele continuasse autenticando, trocar a senha nao
     * expulsaria quem ja estava dentro, que e o principal motivo de trocar.
     */
    @Test
    @DisplayName("deve responder 401 com token anterior a troca de senha")
    void deveResponder401ComTokenAnteriorATrocaDeSenha() throws Exception {
        when(tokenFreshness.isStale(any())).thenReturn(true);

        mockMvc.perform(get("/pets/all").header("Authorization", "Bearer " + tokenValido()))
                .andExpect(status().isUnauthorized());

        verify(petService, never()).listAllPets();
    }

    @Test
    @DisplayName("o health check nao deve exigir token - quem chama e o provedor de hospedagem")
    void healthCheckNaoDeveExigirToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("os demais endpoints do actuator devem exigir token - so o health e publico")
    void demaisEndpointsDoActuatorDevemExigirToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }
}

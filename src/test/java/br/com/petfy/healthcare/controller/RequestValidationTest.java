package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PersonResponseDTO;
import br.com.petfy.healthcare.exception.GlobalExceptionHandler;
import br.com.petfy.healthcare.service.PersonExportService;
import br.com.petfy.healthcare.service.PersonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O POST valida o payload; o PUT continua parcial de proposito, entao nao valida.
 * Sem o handler de MethodArgumentNotValidException no GlobalExceptionHandler, o
 * erro de validacao cairia no handler generico e voltaria como 500.
 */
@ExtendWith(MockitoExtension.class)
class RequestValidationTest {

    @Mock
    private PersonService personService;

    @Mock
    private PersonExportService personExportService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PersonController(personService, personExportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("deve responder 400 e nao chamar o service quando o email e invalido")
    void deveResponder400QuandoEmailInvalido() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "nao-e-email", "password", "s3nhaForte", "acceptedTerms", true));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("email: email invalido"));

        verify(personService, never()).createPerson(any());
    }

    @Test
    @DisplayName("deve responder 400 quando a senha e curta demais para o BCrypt")
    void deveResponder400QuandoSenhaCurta() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br", "password", "123", "acceptedTerms", true));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: senha deve ter entre 8 e 72 caracteres"));

        verify(personService, never()).createPerson(any());
    }

    @Test
    @DisplayName("deve acumular as mensagens quando varios campos estao invalidos")
    void deveAcumularMensagensDeVariosCampos() throws Exception {
        var body = json(Map.of("email", "nao-e-email", "acceptedTerms", true));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "email: email invalido; name: nome e obrigatorio; password: senha e obrigatoria"));
    }

    @Test
    @DisplayName("deve aceitar o payload valido e responder 201")
    void deveAceitarPayloadValido() throws Exception {
        var id = UUID.randomUUID();
        when(personService.createPerson(any())).thenReturn(PersonResponseDTO.builder().personId(id).build());

        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br", "password", "s3nhaForte", "acceptedTerms", true));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(personService).createPerson(any());
    }

    /**
     * Sem aceite nao ha base legal para tratar dado de saude, entao a conta nao pode
     * nascer. A checagem esta no request, e nao no service, para o 400 sair antes de
     * qualquer escrita.
     */
    @Test
    @DisplayName("cadastro sem aceite dos termos responde 400 e nao chama o service")
    void cadastroSemAceiteResponde400() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br",
                "password", "s3nhaForte"));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "acceptedTerms: e obrigatorio aceitar os termos e a politica de privacidade"));

        verify(personService, never()).createPerson(any());
    }

    /**
     * Recusar explicitamente e diferente de omitir, e as duas respondem igual: o
     * {@code @AssertTrue} sozinho consideraria nulo valido, entao ha {@code @NotNull}
     * junto - sem ele o campo ausente passaria e a conta nasceria sem consentimento.
     */
    @Test
    @DisplayName("cadastro recusando os termos tambem responde 400")
    void cadastroRecusandoTermosResponde400() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br",
                "password", "s3nhaForte", "acceptedTerms", false));

        mockMvc.perform(post("/persons").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "acceptedTerms: e obrigatorio aceitar os termos e a politica de privacidade"));

        verify(personService, never()).createPerson(any());
    }

    @Test
    @DisplayName("o update deve seguir aceitando payload parcial - a validacao nao vale para PUT")
    void updateDeveAceitarPayloadParcial() throws Exception {
        var id = UUID.randomUUID();
        when(personService.updateCurrentPerson(any())).thenReturn(PersonResponseDTO.builder().personId(id).build());

        var body = json(Map.of("phone", "11888888888"));

        mockMvc.perform(put("/persons/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(personService).updateCurrentPerson(any());
    }

    /**
     * A troca de senha e o unico PUT do projeto com @Valid: nos demais o payload
     * parcial e proposital, aqui os dois campos sao sempre obrigatorios. Se
     * alguem replicar o padrao dos outros PUT e remover a anotacao, o endpoint
     * passa a aceitar senha vazia sem reclamar - e este teste e quem avisa.
     */
    @Test
    @DisplayName("a troca de senha deve validar o payload, ao contrario dos demais PUT")
    void trocaDeSenhaDeveValidarPayload() throws Exception {
        var body = json(Map.of("currentPassword", "senha-atual", "newPassword", "curta"));

        mockMvc.perform(put("/persons/me/password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(personService, never()).changePassword(any());
    }

    @Test
    @DisplayName("a troca de senha deve responder 204 quando o payload esta completo")
    void trocaDeSenhaDeveResponder204() throws Exception {
        var body = json(Map.of("currentPassword", "senha-atual", "newPassword", "s3nhaNovaForte"));

        mockMvc.perform(put("/persons/me/password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        verify(personService).changePassword(any());
    }
}

package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.exception.GlobalExceptionHandler;
import br.com.petfy.healthcare.service.OwnerService;
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
    private OwnerService ownerService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OwnerController(ownerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("deve responder 400 e nao chamar o service quando o email e invalido")
    void deveResponder400QuandoEmailInvalido() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "nao-e-email", "password", "s3nhaForte"));

        mockMvc.perform(post("/owners/include").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("email: email invalido"));

        verify(ownerService, never()).createOwner(any());
    }

    @Test
    @DisplayName("deve responder 400 quando a senha e curta demais para o BCrypt")
    void deveResponder400QuandoSenhaCurta() throws Exception {
        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br", "password", "123"));

        mockMvc.perform(post("/owners/include").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: senha deve ter entre 8 e 72 caracteres"));

        verify(ownerService, never()).createOwner(any());
    }

    @Test
    @DisplayName("deve acumular as mensagens quando varios campos estao invalidos")
    void deveAcumularMensagensDeVariosCampos() throws Exception {
        var body = json(Map.of("email", "nao-e-email"));

        mockMvc.perform(post("/owners/include").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "email: email invalido; name: nome e obrigatorio; password: senha e obrigatoria"));
    }

    @Test
    @DisplayName("deve aceitar o payload valido e responder 201")
    void deveAceitarPayloadValido() throws Exception {
        var id = UUID.randomUUID();
        when(ownerService.createOwner(any())).thenReturn(OwnerResponseDTO.builder().ownerId(id).build());

        var body = json(Map.of("name", "Ulysses", "email", "ulysses@petfy.com.br", "password", "s3nhaForte"));

        mockMvc.perform(post("/owners/include").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(ownerService).createOwner(any());
    }

    @Test
    @DisplayName("o update deve seguir aceitando payload parcial - a validacao nao vale para PUT")
    void updateDeveAceitarPayloadParcial() throws Exception {
        var id = UUID.randomUUID();
        when(ownerService.updateOwner(any(), any())).thenReturn(OwnerResponseDTO.builder().ownerId(id).build());

        var body = json(Map.of("phone", "11888888888"));

        mockMvc.perform(put("/owners/{ownerId}", id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(ownerService).updateOwner(any(), any());
    }
}

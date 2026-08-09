package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.exception.GlobalExceptionHandler;
import br.com.petfy.healthcare.service.HealthRecordService;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O registro de atendimento respondia <b>500 para todo mundo</b>, e ninguem viu.
 *
 * O {@code category} do {@code HealthRecordRequestDTO} carregava um {@code @NotBlank}
 * herdado de quando o campo era texto livre. O {@code @NotBlank} so sabe validar
 * {@code CharSequence}: sobre um enum, o Hibernate Validator nao encontra validador e
 * lanca {@code UnexpectedTypeException} <b>antes de olhar o corpo</b>. Nao havia payload
 * que passasse - o correto e o invalido saiam iguais, com 500.
 *
 * <b>Por que passou despercebido:</b> o teste de validacao do projeto cobria
 * {@code /persons}, e o {@code OpenApiContractTest} compara o contrato, que descreve o
 * campo como enum obrigatorio - exatamente o que a anotacao dizia querer. So chamando a
 * rota o defeito aparece, e ele apareceu ao popular a tela do animal.
 */
@ExtendWith(MockitoExtension.class)
class HealthRecordValidationTest {

    @Mock
    private HealthRecordService healthRecordService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthRecordController(healthRecordService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String json(Map<String, Object> body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Test
    @DisplayName("payload valido responde 201 - era 500 enquanto o @NotBlank estava sobre o enum")
    void payloadValidoResponde201() throws Exception {
        var animalId = UUID.randomUUID();
        when(healthRecordService.createHealthRecord(any()))
                .thenReturn(HealthRecordResponseDTO.builder().healthRecordId(UUID.randomUUID()).build());

        var body = json(Map.of(
                "animalId", animalId.toString(),
                "category", "CONSULTA",
                "eventDate", "2026-03-03",
                "diagnosis", "Consulta de rotina"));

        mockMvc.perform(post("/health-records").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(healthRecordService).createHealthRecord(any());
    }

    /**
     * A categoria continua obrigatoria - o {@code @NotNull} e quem faz esse trabalho, e
     * sempre fez. Remover o {@code @NotBlank} corrigiu o 500 sem afrouxar a regra, e este
     * teste e quem prova as duas coisas ao mesmo tempo.
     */
    @Test
    @DisplayName("sem categoria responde 400, e nao chama o service")
    void semCategoriaResponde400() throws Exception {
        var corpo = new HashMap<String, Object>();
        corpo.put("animalId", UUID.randomUUID().toString());
        corpo.put("eventDate", "2026-03-03");

        mockMvc.perform(post("/health-records").contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(healthRecordService, never()).createHealthRecord(any());
    }

    @Test
    @DisplayName("categoria fora do enum responde 400, e nao 500")
    void categoriaInvalidaResponde400() throws Exception {
        var body = json(Map.of(
                "animalId", UUID.randomUUID().toString(),
                "category", "NAO_EXISTE",
                "eventDate", "2026-03-03"));

        mockMvc.perform(post("/health-records").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verify(healthRecordService, never()).createHealthRecord(any());
    }
}

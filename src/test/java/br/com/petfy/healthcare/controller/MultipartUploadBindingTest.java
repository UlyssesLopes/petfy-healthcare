package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalResponseDTO;
import br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO;
import br.com.petfy.healthcare.exception.GlobalExceptionHandler;
import br.com.petfy.healthcare.service.AttachmentService;
import br.com.petfy.healthcare.service.PetIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As duas rotas que recebem arquivo, conferidas na LIGACAO HTTP.
 *
 * <b>Por que este teste existe:</b> o upload de anexo e a leitura da carteirinha tinham codigo,
 * servico testado e armazenamento testado — e nenhum cliente conseguia mandar arquivo para
 * nenhuma das duas. Elas usavam {@code @RequestParam MultipartFile}, que o springdoc nao
 * documenta como corpo multipart: o {@code openapi.json} saia SEM {@code requestBody}, e o
 * cliente do front nasce dele. O passo 3 do onboarding — que o desenho chama de "o mais
 * trabalhoso e o mais valioso" —, a foto do animal e a foto da carteirinha ficaram trancados
 * por isso.
 *
 * A correcao foi trocar por {@code @RequestPart}. O ponto e que <b>nenhum teste anterior passava
 * pela ligacao</b>: os de servico chamam o metodo com um MultipartFile na mao, e por isso
 * continuariam verdes com a rota inalcancavel. Este entra pelo MockMvc, que e onde o defeito
 * morava.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("as rotas de arquivo: a ligacao multipart")
class MultipartUploadBindingTest {

    @Mock private AttachmentService attachmentService;
    @Mock private PetIdService petIdService;

    private MockMvc anexos;
    private MockMvc carteirinha;

    private static final UUID ANIMAL = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        anexos = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        carteirinha = MockMvcBuilders.standaloneSetup(new PetIdController(petIdService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockMultipartFile arquivo(String nome) {
        return new MockMultipartFile(nome, "carteirinha.jpg", "image/jpeg", "conteudo".getBytes());
    }

    @Nested
    @DisplayName("anexo do animal")
    class Anexo {

        @Test
        @DisplayName("aceita o arquivo na parte 'file' e entrega o mesmo conteudo ao servico")
        void aceitaOArquivo() throws Exception {
            when(attachmentService.upload(any(), any(), any(), any(), any()))
                    .thenReturn(AttachmentResponseDTO.builder().build());

            anexos.perform(multipart("/animals/{animalId}/attachments", ANIMAL).file(arquivo("file")))
                    .andExpect(status().isCreated());

            ArgumentCaptor<MultipartFile> recebido = ArgumentCaptor.forClass(MultipartFile.class);
            verify(attachmentService).upload(eq(ANIMAL), recebido.capture(), any(), any(), any());
            assertThat(recebido.getValue().getOriginalFilename()).isEqualTo("carteirinha.jpg");
            assertThat(recebido.getValue().getBytes()).isEqualTo("conteudo".getBytes());
        }

        /**
         * O que o contrato promete e o que a rota cobra tem de ser a mesma coisa: o
         * {@code openapi.json} declara {@code required: ["file"]}, entao mandar sem a parte nao
         * pode passar adiante.
         */
        @Test
        @DisplayName("sem a parte 'file' nao chama o servico")
        void semArquivoNaoChamaOServico() throws Exception {
            anexos.perform(multipart("/animals/{animalId}/attachments", ANIMAL).file(arquivo("outro")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(148));

            verify(attachmentService, never()).upload(any(), any(), any(), any(), any());
        }

        /**
         * Os campos que dizem O QUE o arquivo documenta continuam {@code @RequestParam}, e o
         * contrato os declara como query. Isso e proposital: mudar os quatro de uma vez trocaria
         * a forma de chamada da rota inteira sem necessidade — o que faltava era o arquivo.
         */
        @Test
        @DisplayName("vaccineId e description continuam vindo por query, junto do arquivo")
        void metadadosPorQuery() throws Exception {
            UUID vacina = UUID.randomUUID();
            when(attachmentService.upload(any(), any(), any(), any(), any()))
                    .thenReturn(AttachmentResponseDTO.builder().build());

            anexos.perform(multipart("/animals/{animalId}/attachments", ANIMAL)
                            .file(arquivo("file"))
                            .param("vaccineId", vacina.toString())
                            .param("description", "carteirinha de papel"))
                    .andExpect(status().isCreated());

            verify(attachmentService).upload(eq(ANIMAL), any(), eq(vacina), eq(null), eq("carteirinha de papel"));
        }
    }

    @Nested
    @DisplayName("carteirinha por OCR")
    class Carteirinha {

        @Test
        @DisplayName("aceita o arquivo na parte 'file' e responde 201")
        void aceitaOArquivo() throws Exception {
            when(petIdService.importAnimalFromIdCard(any())).thenReturn(AnimalResponseDTO.builder().build());

            carteirinha.perform(multipart("/pet-id/import-pet-id-card").file(arquivo("file")))
                    .andExpect(status().isCreated());

            verify(petIdService).importAnimalFromIdCard(any());
        }

        @Test
        @DisplayName("sem a parte 'file' nao chama o servico")
        void semArquivoNaoChamaOServico() throws Exception {
            carteirinha.perform(multipart("/pet-id/import-pet-id-card").file(arquivo("outro")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(148));

            verify(petIdService, never()).importAnimalFromIdCard(any());
        }
    }
}

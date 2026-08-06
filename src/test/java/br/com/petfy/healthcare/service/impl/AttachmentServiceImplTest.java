package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.PetTutores;
import br.com.petfy.healthcare.domain.entity.Attachment;
import br.com.petfy.healthcare.domain.entity.HealthRecord;
import br.com.petfy.healthcare.domain.entity.Owner;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Species;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.repository.AttachmentRepository;
import br.com.petfy.healthcare.domain.repository.HealthRecordRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.security.AnimalAccessGuard;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import br.com.petfy.healthcare.storage.AttachmentStorage;
import br.com.petfy.healthcare.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceImplTest {

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private VaccineRepository vaccineRepository;
    @Mock private HealthRecordRepository healthRecordRepository;
    @Mock private AttachmentStorage attachmentStorage;
    @Mock private AnimalAccessGuard animalAccessGuard;
    @Mock private CurrentOwnerProvider currentOwnerProvider;

    private AttachmentServiceImpl attachmentService;

    private static final UUID ANIMAL_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VACCINE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID ATTACHMENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentServiceImpl(attachmentRepository, vaccineRepository,
                healthRecordRepository, attachmentStorage, animalAccessGuard, currentOwnerProvider);
        ReflectionTestUtils.setField(attachmentService, "maxSizeBytes", 1024L);
    }

    private Owner ulysses() {
        return Owner.builder().ownerId(OWNER_ID).name("Ulysses").email("ulysses@petfy.com.br").build();
    }

    private Animal animal() {
        return Animal.builder().animalId(ANIMAL_ID).name("Rex").species(Species.CANINA)
                .tutors(PetTutores.titular(ulysses())).build();
    }

    /** PDF de mentira, mas com a assinatura que a deteccao exige. */
    private MockMultipartFile pdf(String nome) {
        byte[] conteudo = "%PDF-1.7 conteudo do laudo".getBytes(StandardCharsets.US_ASCII);
        return new MockMultipartFile("file", nome, "application/pdf", conteudo);
    }

    private StoredFile armazenado() {
        return new StoredFile("animals/" + ANIMAL_ID + "/" + UUID.randomUUID(), 26L, "a".repeat(64));
    }

    private Attachment anexoDoAnimal() {
        return Attachment.builder()
                .attachmentId(ATTACHMENT_ID)
                .animal(animal())
                .originalFilename("laudo.pdf")
                .contentType("application/pdf")
                .sizeBytes(26L)
                .checksumSha256("a".repeat(64))
                .storageKey("animals/" + ANIMAL_ID + "/chave")
                .creationDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("guarda o arquivo e devolve o metadado")
        void guardaEDevolveMetadado() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            var result = attachmentService.upload(ANIMAL_ID, pdf("laudo.pdf"), null, null, "exame de sangue");

            assertThat(result.getAnimalId()).isEqualTo(ANIMAL_ID);
            assertThat(result.getOriginalFilename()).isEqualTo("laudo.pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getDescription()).isEqualTo("exame de sangue");
            assertThat(result.getUploadedByOwnerName()).isEqualTo("Ulysses");
        }

        /**
         * O tipo gravado e o DETECTADO, nunca o declarado. Se o banco guardasse o
         * declarado, o download devolveria o cabecalho que o atacante escolheu.
         */
        @Test
        @DisplayName("grava o tipo detectado, e nao o Content-Type do upload")
        void gravaOTipoDetectado() {
            var mentiroso = new MockMultipartFile("file", "laudo.pdf", "image/png",
                    "%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            var result = attachmentService.upload(ANIMAL_ID, mentiroso, null, null, null);

            assertThat(result.getContentType()).isEqualTo("application/pdf");
        }

        /** O caso que a deteccao por conteudo existe para pegar. */
        @Test
        @DisplayName("executavel disfarcado de PDF responde 415 e nao chega ao storage")
        void executavelDisfarcadoERecusado() {
            var executavel = new MockMultipartFile("file", "laudo.pdf", "application/pdf",
                    new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0, 0, 0, 0, 0, 0, 0, 0});

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            assertThatThrownBy(() -> attachmentService.upload(ANIMAL_ID, executavel, null, null, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ATTACHMENT_TYPE_NOT_ALLOWED.getCode(),
                            HttpStatus.UNSUPPORTED_MEDIA_TYPE);

            verifyNoInteractions(attachmentStorage);
            verify(attachmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("arquivo vazio responde 400 sem tocar no storage")
        void arquivoVazioERecusado() {
            var vazio = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            assertThatThrownBy(() -> attachmentService.upload(ANIMAL_ID, vazio, null, null, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.ATTACHMENT_EMPTY.getCode());

            verifyNoInteractions(attachmentStorage);
        }

        @Test
        @DisplayName("arquivo acima do teto responde 413 sem tocar no storage")
        void arquivoGrandeERecusado() {
            byte[] grande = new byte[2048];
            System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, grande, 0, 5);
            var pesado = new MockMultipartFile("file", "grande.pdf", "application/pdf", grande);

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            assertThatThrownBy(() -> attachmentService.upload(ANIMAL_ID, pesado, null, null, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getCode(),
                            HttpStatus.PAYLOAD_TOO_LARGE);

            verifyNoInteractions(attachmentStorage);
        }

        /**
         * Storage antes do banco: invertido, o registro existiria apontando para um
         * arquivo que a gravacao pode nao ter escrito, e o download responderia 500 num
         * anexo que a listagem jura existir.
         */
        @Test
        @DisplayName("grava no storage antes do banco")
        void storageAntesDoBanco() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            attachmentService.upload(ANIMAL_ID, pdf("laudo.pdf"), null, null, null);

            var ordem = inOrder(attachmentStorage, attachmentRepository);
            ordem.verify(attachmentStorage).store(any(), any());
            ordem.verify(attachmentRepository).save(any(Attachment.class));
        }

        @Test
        @DisplayName("anexo de vacina exige que a vacina seja do mesmo animal")
        void anexoDeVacinaDoMesmoAnimal() {
            var vacina = Vaccine.builder().vaccineId(VACCINE_ID).animal(animal()).vaccineName("V10").build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(vacina));
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            var result = attachmentService.upload(ANIMAL_ID, pdf("carteirinha.pdf"), VACCINE_ID, null, null);

            assertThat(result.getVaccineId()).isEqualTo(VACCINE_ID);
        }

        /**
         * Sem esta checagem, informar o id de uma vacina de outro animal penduraria o anexo
         * nela - o CHECK do banco nao pega, porque a coluna esta preenchida com id valido.
         */
        @Test
        @DisplayName("vacina de outro animal responde 404 sem gravar nada")
        void vacinaDeOutroAnimalERecusada() {
            var deOutroAnimal = Vaccine.builder()
                    .vaccineId(VACCINE_ID)
                    .animal(Animal.builder().animalId(UUID.randomUUID()).name("Nina").build())
                    .build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(vaccineRepository.findById(VACCINE_ID)).thenReturn(Optional.of(deOutroAnimal));

            var arquivo = pdf("laudo.pdf");

            assertThatThrownBy(() -> attachmentService.upload(ANIMAL_ID, arquivo, VACCINE_ID, null, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code", "httpStatus")
                    .containsExactly(ErrorMessageEnum.VACCINE_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);

            verifyNoInteractions(attachmentStorage);
        }

        @Test
        @DisplayName("atendimento de outro animal responde 404 sem gravar nada")
        void atendimentoDeOutroAnimalERecusado() {
            var deOutroAnimal = HealthRecord.builder()
                    .healthRecordId(RECORD_ID)
                    .animal(Animal.builder().animalId(UUID.randomUUID()).name("Nina").build())
                    .build();

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(healthRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(deOutroAnimal));

            var arquivo = pdf("laudo.pdf");

            assertThatThrownBy(() -> attachmentService.upload(ANIMAL_ID, arquivo, null, RECORD_ID, null))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.HEALTH_RECORD_NOT_FOUND.getCode());

            verifyNoInteractions(attachmentStorage);
        }

        /**
         * O nome do cliente e guardado so para exibir, e nunca monta caminho - a chave e
         * gerada pelo storage. Mas separador no nome nao ajuda ninguem na tela nem no
         * header de download.
         */
        @Test
        @DisplayName("nome de arquivo com separador de caminho e neutralizado")
        void nomeComSeparadorENeutralizado() {
            var comCaminho = new MockMultipartFile("file", "../../etc/passwd",
                    "application/pdf", "%PDF-1.7 x".getBytes(StandardCharsets.US_ASCII));

            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            var result = attachmentService.upload(ANIMAL_ID, comCaminho, null, null, null);

            assertThat(result.getOriginalFilename())
                    .doesNotContain("/")
                    .doesNotContain("\\");
        }
    }

    @Nested
    @DisplayName("listByAnimal")
    class ListByAnimal {

        @Test
        @DisplayName("lista os anexos do animal")
        void listaOsAnexosDoAnimal() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID))
                    .thenReturn(List.of(anexoDoAnimal()));

            assertThat(attachmentService.listByAnimal(ANIMAL_ID, null, null)).hasSize(1);
        }

        /**
         * O filtro por animal vale mesmo na consulta por vacina: sem ele, passar o id de uma
         * vacina de outro animal devolveria os anexos dela, e a autorizacao teria sido feita
         * sobre o animal errado.
         */
        @Test
        @DisplayName("anexo de outro animal nao aparece, mesmo consultando por vacina")
        void naoVazaAnexoDeOutroAnimal() {
            var deOutroAnimal = Attachment.builder()
                    .attachmentId(UUID.randomUUID())
                    .animal(Animal.builder().animalId(UUID.randomUUID()).name("Nina").build())
                    .originalFilename("da-nina.pdf")
                    .contentType("application/pdf")
                    .sizeBytes(10L)
                    .checksumSha256("b".repeat(64))
                    .storageKey("animals/outro/chave")
                    .creationDate(LocalDateTime.now())
                    .build();

            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(attachmentRepository.findByVaccineVaccineIdOrderByCreationDateDesc(VACCINE_ID))
                    .thenReturn(List.of(anexoDoAnimal(), deOutroAnimal));

            assertThat(attachmentService.listByAnimal(ANIMAL_ID, VACCINE_ID, null))
                    .singleElement()
                    .satisfies(a -> assertThat(a.getAnimalId()).isEqualTo(ANIMAL_ID));
        }
    }

    @Nested
    @DisplayName("download")
    class Download {

        @Test
        @DisplayName("devolve o conteudo com nome e tipo do registro")
        void devolveOConteudo() {
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(anexoDoAnimal()));
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(attachmentStorage.read("animals/" + ANIMAL_ID + "/chave"))
                    .thenReturn(new ByteArrayInputStream("laudo".getBytes(StandardCharsets.UTF_8)));

            var conteudo = attachmentService.download(ATTACHMENT_ID);

            assertThat(conteudo.filename()).isEqualTo("laudo.pdf");
            assertThat(conteudo.contentType()).isEqualTo("application/pdf");
            assertThat(conteudo.sizeBytes()).isEqualTo(26L);
        }

        /**
         * A autorizacao sai do animal DO ANEXO, e nao de um animalId informado pelo cliente - e
         * por isso que a rota de download nao tem animalId. Pedi-lo abriria a possibilidade
         * de autorizar contra um animal e servir o arquivo de outro.
         */
        @Test
        @DisplayName("autoriza pelo animal do proprio anexo")
        void autorizaPeloAnimalDoAnexo() {
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(anexoDoAnimal()));
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(attachmentStorage.read(any()))
                    .thenReturn(new ByteArrayInputStream(new byte[0]));

            attachmentService.download(ATTACHMENT_ID);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
        }

        @Test
        @DisplayName("recusa do guard impede a leitura do storage")
        void recusaDoGuardNaoLeStorage() {
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(anexoDoAnimal()));
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenThrow(new PetfyHealthcareException(
                    ErrorMessageEnum.ANIMAL_NOT_FOUND.getMessage(),
                    ErrorMessageEnum.ANIMAL_NOT_FOUND.getCode(),
                    HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> attachmentService.download(ATTACHMENT_ID))
                    .isInstanceOf(PetfyHealthcareException.class);

            verifyNoInteractions(attachmentStorage);
        }

        @Test
        @DisplayName("anexo inexistente responde 404 sem consultar o guard")
        void anexoInexistenteResponde404() {
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attachmentService.download(ATTACHMENT_ID))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("code")
                    .isEqualTo(ErrorMessageEnum.ATTACHMENT_NOT_FOUND.getCode());

            verifyNoInteractions(animalAccessGuard);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        /**
         * A linha sai antes do arquivo: a transacao governa o banco, nao o disco. Se o
         * delete da linha falhar, o arquivo continua la e o anexo segue inteiro - ao
         * contrario, o tutor veria um anexo que o download nao consegue abrir.
         */
        @Test
        @DisplayName("apaga a linha antes do arquivo")
        void apagaALinhaAntesDoArquivo() {
            var anexo = anexoDoAnimal();
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(anexo));
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            attachmentService.delete(ATTACHMENT_ID);

            var ordem = inOrder(attachmentRepository, attachmentStorage);
            ordem.verify(attachmentRepository).delete(anexo);
            ordem.verify(attachmentRepository).flush();
            ordem.verify(attachmentStorage).delete(anyCollection());
        }

        @Test
        @DisplayName("remover anexo exige escrita, e nao apenas leitura")
        void removerExigeEscrita() {
            when(attachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(anexoDoAnimal()));
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());

            attachmentService.delete(ATTACHMENT_ID);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }
    }

    /**
     * A tabela operacao -> nivel, no mesmo formato dos outros servicos desde a V15.
     */
    @Nested
    @DisplayName("nivel exigido do guard")
    class NivelExigido {

        @Test
        @DisplayName("anexar exige escrita")
        void anexarExigeEscrita() {
            when(animalAccessGuard.requireEscrita(ANIMAL_ID)).thenReturn(animal());
            when(currentOwnerProvider.require()).thenReturn(ulysses());
            when(attachmentStorage.store(any(), any())).thenReturn(armazenado());
            when(attachmentRepository.save(any(Attachment.class))).thenAnswer(i -> i.getArgument(0));

            attachmentService.upload(ANIMAL_ID, pdf("laudo.pdf"), null, null, null);

            verify(animalAccessGuard).requireEscrita(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireLeitura(any());
        }

        /** Ver e baixar laudo e leitura: e para isso que o VIEWER existe. */
        @Test
        @DisplayName("listar e baixar exigem so leitura")
        void listarEBaixarExigemSoLeitura() {
            when(animalAccessGuard.requireLeitura(ANIMAL_ID)).thenReturn(animal());
            when(attachmentRepository.findByAnimalAnimalIdOrderByCreationDateDesc(ANIMAL_ID)).thenReturn(List.of());

            attachmentService.listByAnimal(ANIMAL_ID, null, null);

            verify(animalAccessGuard).requireLeitura(ANIMAL_ID);
            verify(animalAccessGuard, never()).requireEscrita(any());
        }
    }

    @Nested
    @DisplayName("o metadado nao expoe infraestrutura")
    class MetadadoNaoExpoeInfra {

        /**
         * A chave e detalhe de storage: muda quando o storage mudar, e um cliente que a
         * conhecesse acabaria tentando montar caminho com ela.
         */
        @Test
        @DisplayName("a resposta nao carrega a chave de storage")
        void respostaNaoCarregaStorageKey() {
            assertThat(br.com.petfy.healthcare.domain.dto.AttachmentResponseDTO.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("storageKey")
                    .contains("checksumSha256");
        }
    }

}

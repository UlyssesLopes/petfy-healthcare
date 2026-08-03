package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetIdServiceImplTest {

    @Mock
    private TesseractOcrServiceImpl tesseractOcrService;

    @Mock
    private ImageProcessorService imageProcessorService;

    @Mock
    private PetRepository petRepository;

    @Mock
    private CurrentOwnerProvider currentOwnerProvider;

    @InjectMocks
    private PetIdServiceImpl petIdService;

    @Nested
    @DisplayName("importPetFromIdCard")
    class ImportPetFromIdCard {

        @Test
        @DisplayName("deve resolver o owner antes de processar a imagem, e nao aceitar id do cliente")
        void deveResolverOwnerAntesDeProcessarImagem() {
            when(currentOwnerProvider.require()).thenThrow(new PetfyHealthcareException(
                    "Invalid email or password", 401, HttpStatus.UNAUTHORIZED));
            var file = new MockMultipartFile("file", "carteirinha.png", "image/png", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> petIdService.importPetFromIdCard(file))
                    .isInstanceOf(PetfyHealthcareException.class)
                    .extracting("httpStatus")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);

            verifyNoInteractions(imageProcessorService, tesseractOcrService, petRepository);
        }
    }

    @Nested
    @DisplayName("bornDateStringFormat")
    class BornDateStringFormat {

        @Test
        @DisplayName("deve converter data no formato dd/MM/yyyy")
        void deveConverterDataValida() {
            assertThat(petIdService.bornDateStringFormat("15/03/2021"))
                    .isEqualTo(LocalDate.of(2021, 3, 15));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "15-03-2021", "texto qualquer", "32/13/2021"})
        @DisplayName("deve retornar null em vez de estourar excecao quando o OCR nao le a data")
        void deveRetornarNullQuandoDataInvalida(String valorDoOcr) {
            assertThat(petIdService.bornDateStringFormat(valorDoOcr)).isNull();
        }

        @Test
        @DisplayName("deve retornar null quando a data esta ausente")
        void deveRetornarNullQuandoDataAusente() {
            assertThat(petIdService.bornDateStringFormat(null)).isNull();
        }
    }

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("deve mapear os campos lidos do documento para o response")
        void deveMapearCamposDoDocumento() {
            var campos = Map.of(
                    "Nome do Animal", "Rex",
                    "Registro Geral do Animal", "RG-12345",
                    "Cor", "Caramelo",
                    "Sexo", "Macho",
                    "Data de Nascimento", "15/03/2021",
                    "Naturalidade", "Sao Paulo"
            );

            var result = petIdService.parse(campos);

            assertThat(result.getName()).isEqualTo("Rex");
            assertThat(result.getGeneralRegistry()).isEqualTo("RG-12345");
            assertThat(result.getColor()).isEqualTo("Caramelo");
            assertThat(result.getGender()).isEqualTo("Macho");
            assertThat(result.getBornDate()).isEqualTo(LocalDate.of(2021, 3, 15));
            assertThat(result.getBornLocal()).isEqualTo("Sao Paulo");
        }

        @Test
        @DisplayName("deve mapear Especie para type e Raca para breed")
        void deveMapearEspecieParaTypeERacaParaBreed() {
            var result = petIdService.parse(Map.of(
                    "Espécie", "Canina",
                    "Raça", "Labrador"
            ));

            assertThat(result.getType()).isEqualTo("Canina");
            assertThat(result.getBreed()).isEqualTo("Labrador");
        }

        @Test
        @DisplayName("deve deixar breed nulo quando o documento nao traz o campo Raca")
        void deveDeixarBreedNuloSemCampoRaca() {
            var result = petIdService.parse(Map.of("Espécie", "Felina"));

            assertThat(result.getType()).isEqualTo("Felina");
            assertThat(result.getBreed()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Sim", "sim", "SIM", "S"})
        @DisplayName("deve interpretar o microchip como true quando o documento diz Sim")
        void deveInterpretarMicrochipComoTrue(String valorDoOcr) {
            var result = petIdService.parse(Map.of("Microchip", valorDoOcr));

            assertThat(result.getMicrochip()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Nao", "nao", "N", "Não"})
        @DisplayName("deve interpretar o microchip como false quando o documento diz Nao")
        void deveInterpretarMicrochipComoFalse(String valorDoOcr) {
            var result = petIdService.parse(Map.of("Microchip", valorDoOcr));

            assertThat(result.getMicrochip()).isFalse();
        }

        @Test
        @DisplayName("deve deixar o microchip nulo quando o campo nao foi lido pelo OCR")
        void deveDeixarMicrochipNuloQuandoAusente() {
            var result = petIdService.parse(Map.of("Nome do Animal", "Rex"));

            assertThat(result.getMicrochip()).isNull();
        }

        @Test
        @DisplayName("deve deixar o microchip nulo quando o valor lido nao e reconhecido")
        void deveDeixarMicrochipNuloQuandoValorDesconhecido() {
            var result = petIdService.parse(Map.of("Microchip", "1234567"));

            assertThat(result.getMicrochip()).isNull();
        }
    }

    @Nested
    @DisplayName("parseFields")
    class ParseFields {

        @Test
        @DisplayName("deve associar o rotulo da linha ao valor da linha seguinte")
        void deveAssociarRotuloAoValorDaLinhaSeguinte() {
            var ocr = String.join("\n",
                    "Nome do Animal",
                    "Rex",
                    "Cor",
                    "Caramelo");

            var result = petIdService.parseFields(ocr);

            assertThat(result)
                    .containsEntry("Nome do Animal", "Rex")
                    .containsEntry("Cor", "Caramelo");
        }

        @Test
        @DisplayName("deve dividir dois rotulos na mesma linha entre os valores da linha seguinte")
        void deveDividirDoisRotulosNaMesmaLinha() {
            var ocr = String.join("\n",
                    "Sexo Cor",
                    "Macho Caramelo escuro");

            var result = petIdService.parseFields(ocr);

            assertThat(result)
                    .containsEntry("Sexo", "Macho")
                    .containsEntry("Cor", "Caramelo escuro");
        }

        @Test
        @DisplayName("deve ignorar linhas em branco entre rotulo e valor")
        void deveIgnorarLinhasEmBranco() {
            var ocr = String.join("\n",
                    "Nome do Animal",
                    "   ",
                    "Rex");

            var result = petIdService.parseFields(ocr);

            assertThat(result).containsEntry("Nome do Animal", "Rex");
        }

        @Test
        @DisplayName("deve remover hifens de preenchimento do fim do valor")
        void deveRemoverHifensDoFimDoValor() {
            var ocr = String.join("\n",
                    "Naturalidade",
                    "Sao Paulo---");

            var result = petIdService.parseFields(ocr);

            assertThat(result).containsEntry("Naturalidade", "Sao Paulo");
        }

        @Test
        @DisplayName("deve retornar mapa vazio quando o OCR nao reconhece nenhum rotulo")
        void deveRetornarMapaVazioQuandoNenhumRotulo() {
            var result = petIdService.parseFields("texto\nsem\nrotulos conhecidos");

            assertThat(result).isEmpty();
        }
    }
}

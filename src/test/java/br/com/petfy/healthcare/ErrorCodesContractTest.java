package br.com.petfy.healthcare;

import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A tabela de codigo de erro vira arquivo versionado, pelo mesmo motivo e com a mesma
 * mecanica do {@link OpenApiContractTest}.
 *
 * <b>Por que ela precisa existir separada do contrato.</b> O OpenAPI publicado descreve
 * so o caminho feliz: {@code ErrorResponse} nao esta entre os schemas, e nenhuma rota
 * declara resposta de erro. Ou seja, a guarda que o contrato da ao corpo de sucesso -
 * mudou o controller, quebrou o build do front - <b>nao alcanca o erro</b>. Sem este
 * arquivo, um codigo novo no {@link ErrorMessageEnum} chega ao front sem traducao e cai
 * no fallback em silencio, que e a mesma familia de falha do teste que pula sem avisar.
 *
 * <b>Por que o front nao le a mensagem.</b> As mensagens daqui estao em ingles e o
 * produto e pt-BR. A traducao e por codigo, do lado do cliente; a {@code mensagemDaApi}
 * vai no arquivo apenas como referencia para quem escreve a traducao, e nunca para ser
 * exibida.
 *
 * <b>Como regenerar</b>, quando a mudanca for intencional:
 *
 * <pre>mvn test -Dtest=ErrorCodesContractTest -Dpetfy.errorcodes.update=true</pre>
 *
 * <b>Sem Spring e sem container</b>, ao contrario do {@code OpenApiContractTest}: o enum
 * e uma classe pura, e subir o contexto para le-lo seria custo sem informacao.
 */
class ErrorCodesContractTest {

    /** Ao lado do openapi.json, e pelo mesmo motivo: o consumidor e o front. */
    private static final Path TABELA = Path.of("contract", "error-codes.json");

    private static final String PROPRIEDADE_DE_ATUALIZACAO = "petfy.errorcodes.update";

    @Test
    @DisplayName("a tabela commitada deve ser igual a que o enum descreve")
    void tabelaCommitadaDeveBaterComOEnum() throws Exception {
        String gerada = gerar();

        if (Boolean.getBoolean(PROPRIEDADE_DE_ATUALIZACAO)) {
            Files.createDirectories(TABELA.getParent());
            Files.writeString(TABELA, gerada, StandardCharsets.UTF_8);
            return;
        }

        assertThat(TABELA)
                .withFailMessage("A tabela ainda nao foi gerada. Rode:%n"
                        + "  mvn test -Dtest=ErrorCodesContractTest -D%s=true", PROPRIEDADE_DE_ATUALIZACAO)
                .exists();

        String commitada = Files.readString(TABELA, StandardCharsets.UTF_8);

        assertThat(normalizarQuebrasDeLinha(gerada))
                .withFailMessage("""
                        O ErrorMessageEnum mudou e a tabela versionada ficou para tras.

                        Se a mudanca e intencional, regenere e commite junto:
                          mvn test -Dtest=ErrorCodesContractTest -D%s=true

                        E confira a traducao no front: codigo sem entrada na tabela de
                        mensagens cai no texto generico, sem ninguem ser avisado.""",
                        PROPRIEDADE_DE_ATUALIZACAO)
                .isEqualTo(normalizarQuebrasDeLinha(commitada));
    }

    /**
     * O enum declara os codigos fora de ordem em quatro pontos (135 a 138), o que torna
     * uma repeticao facil de nao enxergar na revisao. Codigo repetido faz duas falhas
     * diferentes chegarem ao cliente como a mesma coisa - e a traducao, que e por codigo,
     * nao teria como distinguir.
     */
    @Test
    @DisplayName("nenhum codigo de erro pode se repetir")
    void nenhumCodigoSeRepete() {
        Map<Integer, List<String>> porCodigo = Arrays.stream(ErrorMessageEnum.values())
                .collect(Collectors.groupingBy(ErrorMessageEnum::getCode,
                        Collectors.mapping(Enum::name, Collectors.toList())));

        List<Map.Entry<Integer, List<String>>> repetidos = porCodigo.entrySet().stream()
                .filter(entrada -> entrada.getValue().size() > 1)
                .toList();

        assertThat(repetidos)
                .withFailMessage("Codigos repetidos no ErrorMessageEnum: %s", repetidos)
                .isEmpty();
    }

    /**
     * Ordenado por codigo, e nao pela ordem de declaracao: a ordem do enum ja esta
     * embaralhada, e um arquivo que a seguisse produziria diff de linha movida a cada
     * insercao no meio.
     */
    private String gerar() throws Exception {
        List<Map<String, Object>> codigos = Arrays.stream(ErrorMessageEnum.values())
                .sorted(Comparator.comparingInt(ErrorMessageEnum::getCode))
                .map(erro -> {
                    Map<String, Object> linha = new LinkedHashMap<>();
                    linha.put("nome", erro.name());
                    linha.put("codigo", erro.getCode());
                    linha.put("mensagemDaApi", erro.getMessage());
                    return linha;
                })
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(codigos);
    }

    /** Mesmo motivo do OpenApiContractTest: CRLF no checkout do Windows, LF no Jackson. */
    private String normalizarQuebrasDeLinha(String texto) {
        return texto.replace("\r\n", "\n");
    }

}

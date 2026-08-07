package br.com.petfy.healthcare;

import br.com.petfy.healthcare.config.RotasPublicas;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O contrato vira arquivo versionado, e este teste e o que o mantem verdadeiro.
 *
 * <b>Por que ele existe.</b> O cliente do front e gerado do OpenAPI, e a decisao de
 * stack diz que mudanca de controller tem de quebrar o build do front. Sozinho, o
 * gerador nao entrega isso: o {@code /v3/api-docs} so existe com a aplicacao no ar,
 * entao o front geraria contra um contrato velho - ou nem geraria - e compilaria
 * verde enquanto a API ja responde outra coisa. E exatamente a divergencia que
 * aconteceu com o README.
 *
 * Com o contrato commitado, quem muda um controller e nao regenera **quebra este
 * teste**, no mesmo lugar em que o {@code AnimalPurgerCoverageContainerTest} quebra
 * quem cria tabela sem cobrir a exclusao: o build cobra, em vez de alguem lembrar.
 *
 * <b>Como regenerar</b>, quando a mudanca for intencional:
 *
 * <pre>mvn test -Dtest=OpenApiContractTest -Dpetfy.openapi.update=true</pre>
 *
 * O arquivo e reescrito, entra no mesmo commit da mudanca de controller, e a
 * revisao ve o delta de contrato ao lado do codigo que o causou.
 *
 * <b>Estende PostgresContainerTest</b> porque subir o contexto inteiro - que e o
 * que faz o springdoc enxergar os 19 controllers - exige o banco de verdade, com as
 * migrations aplicadas e o Hibernate validando o mapeamento.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest extends PostgresContainerTest {

    /**
     * Fora de src/ de proposito: nao e codigo nem recurso da aplicacao, e o
     * consumidor dele e o front, no mesmo repositorio.
     */
    private static final Path CONTRATO = Path.of("contract", "openapi.json");

    private static final String PROPRIEDADE_DE_ATUALIZACAO = "petfy.openapi.update";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("o contrato commitado deve ser igual ao que a aplicacao publica")
    void contratoCommitadoDeveBaterComOPublicado() throws Exception {
        String publicado = normalizar(publicado());

        if (Boolean.getBoolean(PROPRIEDADE_DE_ATUALIZACAO)) {
            Files.createDirectories(CONTRATO.getParent());
            Files.writeString(CONTRATO, publicado, StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRATO)
                .withFailMessage("O contrato ainda nao foi gerado. Rode:%n"
                        + "  mvn test -Dtest=OpenApiContractTest -D%s=true", PROPRIEDADE_DE_ATUALIZACAO)
                .exists();

        String commitado = Files.readString(CONTRATO, StandardCharsets.UTF_8);

        assertThat(normalizarQuebrasDeLinha(publicado))
                .withFailMessage("""
                        O contrato mudou e o arquivo versionado ficou para tras.

                        Se a mudanca e intencional, regenere e commite junto:
                          mvn test -Dtest=OpenApiContractTest -D%s=true

                        Se nao e, alguem mudou a forma publica da API sem querer - e o
                        cliente do front seria gerado contra o contrato velho.""",
                        PROPRIEDADE_DE_ATUALIZACAO)
                .isEqualTo(normalizarQuebrasDeLinha(commitado));
    }

    /**
     * O contrato congelado pegaria esta mudanca de qualquer jeito, mas como diff -
     * sem dizer o que ele significa. Estes dois casos existem para afirmar a
     * intencao: o contrato declara credencial por padrao, e abre excecao exatamente
     * para as rotas que o SecurityConfig deixa passar, porque as duas afirmacoes
     * saem da mesma lista.
     */
    @Test
    @DisplayName("as rotas publicas devem aparecer sem credencial no contrato")
    void rotasPublicasNaoExigemCredencialNoContrato() throws Exception {
        JsonNode contrato = new ObjectMapper().readTree(publicado());

        assertThat(contrato.at("/components/securitySchemes/bearer-jwt/scheme").asText())
                .isEqualTo("bearer");

        for (RotasPublicas.Rota rota : RotasPublicas.DE_API) {
            JsonNode security = contrato.at("/paths" + ponteiro(rota.padrao())
                    + "/" + rota.metodo().name().toLowerCase() + "/security");

            assertThat(security.isArray() && security.isEmpty())
                    .withFailMessage("%s %s e publica no SecurityConfig, mas o contrato "
                            + "nao a declara sem credencial", rota.metodo(), rota.padrao())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("uma rota comum deve herdar a exigencia global de credencial")
    void rotaComumHerdaAExigenciaGlobal() throws Exception {
        JsonNode contrato = new ObjectMapper().readTree(publicado());

        assertThat(contrato.at("/security/0/bearer-jwt")).isNotNull();
        assertThat(contrato.at("/paths/~1animals/get/security").isMissingNode())
                .withFailMessage("GET /animals nao pode ter security proprio: ela herda "
                        + "a exigencia global, e so as publicas a sobrescrevem")
                .isTrue();
    }

    /** JSON Pointer escapa a barra como ~1, inclusive a que abre o caminho. */
    private String ponteiro(String caminho) {
        return "/" + caminho.replace("/", "~1");
    }

    private String publicado() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * A ordem das chaves no JSON do springdoc segue a ordem em que os handlers sao
     * descobertos, e {@code getDeclaredMethods} nao promete ordem nenhuma entre
     * execucoes. Comparar o texto cru daria falha intermitente - o pior tipo de
     * guarda, porque ensina a ignora-la.
     *
     * Ordenar as chaves torna a comparacao estavel, e de quebra faz o diff de um
     * contrato de 5 mil linhas mostrar so o que mudou. <b>E preciso ler como Map, e
     * nao como JsonNode</b>: ORDER_MAP_ENTRIES_BY_KEYS ordena Map do Java, e um
     * ObjectNode passa por ele intacto - foi o que aconteceu na primeira versao.
     *
     * Os arrays ficam como estao: a ordem deles vem da declaracao (valores de enum,
     * parametros do metodo) e e informacao de contrato, nao ruido.
     *
     * <b>O no servers sai.</b> Ele carrega a URL de quem gerou - aqui, o MockMvc -,
     * e o contrato descreve a forma da API, nao onde ela esta hospedada. Cada
     * ambiente tem a sua, e o cliente recebe a base pela configuracao do front.
     */
    private String normalizar(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        @SuppressWarnings("unchecked")
        Map<String, Object> contrato = mapper.readValue(json, Map.class);
        contrato.remove("servers");

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(contrato);
    }

    /**
     * O repositorio converte para CRLF no checkout do Windows, e o Jackson sempre
     * escreve LF: sem isto, a guarda acusaria diferenca em toda linha de um arquivo
     * identico.
     */
    private String normalizarQuebrasDeLinha(String texto) {
        return texto.replace("\r\n", "\n");
    }

}

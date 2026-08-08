package br.com.petfy.healthcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O campo nulo nao vai no corpo, e este teste e o que sustenta a promessa.
 *
 * <b>Por que ele existe.</b> O contrato nao declara {@code required} em resposta - os
 * unicos 19 schemas que declaram sao {@code *RequestDTO}, porque quem faz o springdoc
 * emitir {@code required} e o {@code @NotNull} da validacao de <i>entrada</i>. Entao todo
 * campo de resposta chega ao cliente gerado como {@code campo?: T}. Enquanto o backend
 * mandava {@code null}, esse tipo mentia duas vezes: dizia que o campo podia faltar - e ele
 * vinha - e nao dizia que podia ser nulo - e ele era. O TypeScript aprovava
 * {@code campo !== undefined} logo antes de a tela quebrar, e foi assim que a home do tutor
 * caiu na primeira vez que abriu com dados de verdade.
 *
 * A correcao esta no {@code application.properties}, numa linha:
 * {@code spring.jackson.default-property-inclusion=non_null}. Uma linha de configuracao,
 * porem, e apagada sem que nada reclame - e o dano so apareceria numa tela, em runtime, no
 * navegador de alguem. <b>Este teste faz o build reclamar antes</b>, no mesmo espirito do
 * {@link OpenApiContractTest}: o contrato so vale se alguem o cobrar.
 *
 * <b>Ele afirma comportamento, e nao configuracao.</b> Ler a propriedade do
 * {@code Environment} provaria apenas que a string esta escrita; o que importa e o que o
 * {@code ObjectMapper} <b>injetado</b> - o mesmo que serializa toda resposta desta API -
 * de fato escreve. Por isso a afirmacao e sobre o JSON produzido.
 *
 * <b>Estende PostgresContainerTest</b> porque o bean vem do contexto inteiro, e o contexto
 * desta aplicacao exige o banco de verdade, com as migrations aplicadas.
 */
@SpringBootTest
class RespostaOmiteNulosTest extends PostgresContainerTest {

    @Autowired
    private ObjectMapper mapper;

    /** Espelha uma pendencia do feed: e o formato exato que derrubou a home. */
    private record Pendencia(String description, String lastFulfilledAt, Aninhado origem) {
    }

    private record Aninhado(String organizationName, String kind) {
    }

    private record Vazios(boolean overdue, int dias, String nota, String silenced) {
    }

    @Test
    @DisplayName("o campo nulo sai do corpo, em vez de viajar como null")
    void omiteNoTopo() throws Exception {
        var json = mapper.writeValueAsString(new Pendencia("Antirrabica", null, null));

        assertThat(json).doesNotContain("lastFulfilledAt");
        assertThat(json).doesNotContain("null");
        assertThat(json).contains("Antirrabica");
    }

    @Test
    @DisplayName("a regra desce em objeto aninhado e em lista")
    void omiteEmProfundidade() throws Exception {
        var json = mapper.writeValueAsString(
                List.of(new Pendencia("Antirrabica", null, new Aninhado(null, "PESSOA"))));

        assertThat(json).doesNotContain("organizationName");
        assertThat(json).doesNotContain("null");
        assertThat(json).contains("PESSOA");
    }

    @Test
    @DisplayName("vazio nao e nulo: false, 0 e string vazia continuam viajando")
    void naoConfundeVazioComNulo() throws Exception {
        // Sumir com eles trocaria um bug por outro: o cliente leria `undefined` onde a
        // API tem uma resposta, e `overdue` ausente e diferente de `overdue: false`.
        var json = mapper.writeValueAsString(new Vazios(false, 0, "", null));

        assertThat(json).contains("\"overdue\":false");
        assertThat(json).contains("\"dias\":0");
        assertThat(json).contains("\"nota\":\"\"");
        assertThat(json).doesNotContain("silenced");
    }

}

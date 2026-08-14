package br.com.petfy.healthcare.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O rotulo que a pessoa usa para reconhecer o proprio aparelho.
 *
 * <b>Espelha o `apelidoDoAparelho` do front</b> (`web/src/dados/aparelhos.ts`), e os casos aqui
 * sao os mesmos: se um dos dois mudar sem o outro, a lista do `/conta` e o e-mail passam a
 * chamar o mesmo aparelho de dois nomes.
 */
class RotuloDoAparelhoTest {

    /** A ordem importa: o Edge se diz Chrome, e o Chrome se diz Safari. O especifico vence. */
    @Test
    @DisplayName("o navegador mais especifico ganha do que ele finge ser")
    void oEspecificoVence() {
        assertThat(RotuloDoAparelho.de(
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537 Chrome/141 Safari/537 Edg/141"))
                .isEqualTo("Edge · Windows");

        assertThat(RotuloDoAparelho.de(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15) AppleWebKit/605 Safari/605"))
                .isEqualTo("Safari · Mac");
    }

    @Test
    @DisplayName("celular e reconhecido pelo sistema")
    void celular() {
        assertThat(RotuloDoAparelho.de("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Safari/604"))
                .isEqualTo("Safari · iPhone");
        assertThat(RotuloDoAparelho.de("Mozilla/5.0 (Linux; Android 14) Chrome/141 Mobile"))
                .isEqualTo("Chrome · Android");
    }

    /**
     * <b>Nulo, e nao uma frase.</b> Quem chama decide o que fazer com a ausencia — o e-mail
     * simplesmente omite a linha, em vez de escrever "aparelho desconhecido" no meio de um alerta
     * de seguranca.
     */
    @Test
    @DisplayName("sem nada reconhecivel, devolve nulo")
    void semNadaReconhecivel() {
        assertThat(RotuloDoAparelho.de(null)).isNull();
        assertThat(RotuloDoAparelho.de("   ")).isNull();
        assertThat(RotuloDoAparelho.de("curl/8.4.0")).isNull();
    }

    @Test
    @DisplayName("so o sistema, ou so o navegador, ja serve")
    void umDosDoisBasta() {
        assertThat(RotuloDoAparelho.de("PostmanRuntime (Windows NT 10.0)")).isEqualTo("Windows");
        assertThat(RotuloDoAparelho.de("Firefox/130")).isEqualTo("Firefox");
    }

}

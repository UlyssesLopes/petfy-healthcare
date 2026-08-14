package br.com.petfy.healthcare.notification;

/**
 * "Chrome · Windows" a partir do User-Agent.
 *
 * <b>Gemeo do `apelidoDoAparelho` em `web/src/dados/aparelhos.ts`</b>, e a duplicacao e
 * consciente: o e-mail sai do servidor e nao pode chamar o do navegador. As duas listas precisam
 * andar juntas — mexeu numa, mexe na outra.
 *
 * O rotulo existe para a pessoa <b>reconhecer o proprio aparelho</b>, e nada alem disso. Nao ha
 * IP e nao ha lugar: guardar por onde alguem entra e o que o produto recusa, e "189.4.x.x" nao
 * ajudaria ninguem a reconhecer coisa nenhuma.
 */
public final class RotuloDoAparelho {

    private RotuloDoAparelho() {
    }

    public static String de(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }

        // A ordem importa: Edge se diz Chrome, e Chrome se diz Safari. O mais especifico vence.
        String navegador = userAgent.contains("Edg/") ? "Edge"
                : userAgent.contains("OPR/") ? "Opera"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Safari/") ? "Safari"
                : null;

        String sistema = userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iPhone"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS X") || userAgent.contains("Macintosh") ? "Mac"
                : userAgent.contains("Linux") ? "Linux"
                : null;

        if (navegador == null && sistema == null) {
            return null;
        }

        if (navegador == null) {
            return sistema;
        }

        return sistema == null ? navegador : navegador + " · " + sistema;
    }

}

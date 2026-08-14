package br.com.petfy.healthcare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS, por perfil.
 *
 * <b>Sem isto a primeira requisicao da primeira tela falha no navegador</b>, antes
 * de qualquer codigo de produto: o SPA roda numa origem (o Vite em
 * localhost:5173) e a API em outra, o que e cross-origin.
 *
 * <b>Nunca {@code *}.</b> A API responde com credencial - token no header
 * Authorization -, e origem coringa transformaria qualquer site aberto na mesma
 * maquina em cliente autorizado a ler a resposta. O construtor recusa {@code *} na
 * subida em vez de confiar em quem configura: e o mesmo criterio do JWT_SECRET,
 * onde nao subir e melhor do que subir errado.
 *
 * <b>Vazio e o default, e ele fecha em vez de abrir.</b> Ambiente que nao declarar
 * CORS_ALLOWED_ORIGINS nao libera origem nenhuma. Fecha por omissao, nao abre.
 */
@Configuration
public class CorsConfig {

    /**
     * O header do contexto ativo (X-Petfy-Organization) e customizado, entao toda
     * requisicao que o carrega vira preflight - e sem ele nesta lista o navegador
     * recusa antes de a requisicao sair. E o header pelo qual a area de
     * organizacao inteira age em nome de uma organizacao.
     */
    private static final List<String> HEADERS_ACEITOS = List.of(
            "Authorization",
            "Content-Type",
            "X-Petfy-Organization"
    );

    /**
     * Header de resposta que o JS so consegue ler se estiver exposto: o padrao do
     * navegador entrega apenas um punhado de headers seguros, e Content-Disposition
     * nao esta entre eles.
     *
     * O download de anexo passa pela API autenticada, e nao por URL assinada
     * (ROADMAP, passo 11) - o nome do arquivo chega neste header, e sem expo-lo
     * todo anexo baixado salva com o nome errado.
     */
    private static final List<String> HEADERS_EXPOSTOS = List.of("Content-Disposition");

    private static final List<String> METODOS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"
    );

    private final List<String> origensPermitidas;

    public CorsConfig(@Value("${petfy.cors.allowed-origins:}") List<String> origensPermitidas) {
        List<String> limpas = origensPermitidas.stream()
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();

        if (limpas.contains("*")) {
            throw new IllegalStateException(
                    "petfy.cors.allowed-origins nao aceita '*': a API responde com credencial. "
                            + "Declare as origens do front, uma a uma.");
        }

        this.origensPermitidas = limpas;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();

        configuracao.setAllowedOrigins(origensPermitidas);
        configuracao.setAllowedMethods(METODOS);
        configuracao.setAllowedHeaders(HEADERS_ACEITOS);
        configuracao.setExposedHeaders(HEADERS_EXPOSTOS);

        /*
         * CHEGOU O DIA. Aqui estava escrito: "falso enquanto o token viaja no header Authorization.
         * So vira true no dia do refresh em cookie httpOnly - e ligar antes ampliaria a superficie
         * sem ninguem estar usando cookie nenhum". O refresh entrou na V51.
         *
         * <b>O que isto liga, e o que NAO liga.</b> Sem `allowCredentials`, o navegador nem manda o
         * cookie no `/auth/refresh` nem deixa o JS ler a resposta — a rota existiria e nunca
         * funcionaria. O que ele nao liga e origem coringa: o construtor desta classe recusa `*` na
         * subida, e com credencial ligada essa recusa passa de rigor a necessidade — o proprio
         * navegador proibe a combinacao.
         */
        configuracao.setAllowCredentials(true);

        // meia hora de cache do preflight: sem isso, cada requisicao com o header de
        // contexto paga duas viagens ate a API
        configuracao.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }

}

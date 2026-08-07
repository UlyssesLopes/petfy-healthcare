package br.com.petfy.healthcare.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * O contrato passa a dizer o que exige credencial.
 *
 * <b>O que isto nao faz.</b> Nao e daqui que o token sai para a rede: o cliente do
 * front e gerado so como tipo, e quem poe o header Authorization e a camada de dados
 * dele. O que se ganha aqui e o contrato deixar de ser omisso sobre autenticacao - e
 * o Swagger UI ganhar o botao Authorize, que hoje obriga a colar o header a mao.
 *
 * <b>Por que por customizer, e nao por anotacao.</b> A alternativa seria
 * {@code @OpenAPIDefinition} na classe mais {@code @SecurityRequirements} vazio nas
 * sete operacoes publicas - o que espalharia a resposta de "quem e publico" por
 * quatro controllers, ao lado da lista que o SecurityConfig ja mantem. Aqui as duas
 * afirmacoes saem do mesmo {@link RotasPublicas}.
 */
@Configuration
public class OpenApiConfig {

    static final String ESQUEMA = "bearer-jwt";

    @Bean
    public OpenApiCustomizer credencialNoContrato() {
        return openApi -> {
            openApi.getComponents().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("O token devolvido por POST /auth/login."));

            // exigencia global: o default do produto e rota autenticada, igual ao
            // anyRequest().authenticated() do SecurityConfig
            openApi.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));

            RotasPublicas.DE_API.forEach(rota -> operacoesDe(openApi.getPaths().get(rota.padrao()))
                    .stream()
                    .filter(entrada -> rota.vale(entrada.metodo()))
                    // lista vazia na operacao sobrescreve a exigencia global: e assim
                    // que o OpenAPI diz "esta aqui nao pede credencial"
                    .forEach(entrada -> entrada.operacao().setSecurity(List.of())));
        };
    }

    private record Entrada(String metodo, Operation operacao) {
    }

    /**
     * Uma rota publica que nao existir no contrato e erro silencioso: a lista teria
     * um caminho que ninguem serve, ou o caminho teria mudado de forma sem alguem
     * atualizar a lista. Falhar na subida e melhor do que publicar um contrato que
     * marca a operacao errada como publica.
     */
    private List<Entrada> operacoesDe(PathItem item) {
        if (item == null) {
            throw new IllegalStateException(
                    "Rota declarada publica em RotasPublicas nao existe no contrato OpenAPI");
        }

        return item.readOperationsMap().entrySet().stream()
                .map(entrada -> new Entrada(entrada.getKey().name(), entrada.getValue()))
                .toList();
    }

}

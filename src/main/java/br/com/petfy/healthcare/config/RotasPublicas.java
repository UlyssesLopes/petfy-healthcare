package br.com.petfy.healthcare.config;

import org.springframework.http.HttpMethod;

import java.util.List;

/**
 * Quem nao exige credencial, numa lista so.
 *
 * <b>Por que existe.</b> Estas rotas precisam ser afirmadas em dois lugares: no
 * {@link SecurityConfig}, que decide quem passa, e no contrato OpenAPI, que declara
 * o que o cliente precisa mandar. Duas listas escritas a mao divergem - e a que
 * perde e sempre a documentacao, exatamente como aconteceu com o README antes do
 * passo 4. Aqui elas sao a mesma lista, lida por dois consumidores.
 *
 * <b>O que NAO entra aqui:</b> {@code /professional/**}, que nao e publica - ela
 * exige credencial profissional conferida no banco, e quem decide e o
 * ProfessionalAccessManager.
 */
public final class RotasPublicas {

    /**
     * Metodo nulo significa qualquer metodo.
     */
    public record Rota(HttpMethod metodo, String padrao) {

        public boolean vale(String metodoHttp) {
            return metodo == null || metodo.name().equalsIgnoreCase(metodoHttp);
        }
    }

    /**
     * Publicas <b>do produto</b>: aparecem no contrato, e e o cliente quem as chama.
     * O padrao esta na forma do OpenAPI ({@code {token}}), que o Spring tambem
     * entende como matcher - assim a mesma string serve aos dois.
     */
    public static final List<Rota> DE_API = List.of(
            new Rota(HttpMethod.POST, "/auth/login"),
            /*
             * O REFRESH E PUBLICO PORQUE QUEM O CHAMA NAO TEM TOKEN — e esse e o ponto dele.
             *
             * Quem acabou de recarregar a pagina perdeu o JWT: ele vive em memoria, por decisao
             * contra XSS. Exigir autenticacao aqui seria exigir justamente o que a rota existe
             * para devolver.
             *
             * <b>Publica nao e sem credencial:</b> a credencial e o cookie httpOnly, que o
             * navegador manda sozinho e que JavaScript nenhum le. E o mesmo criterio do
             * `/share/{token}` e do convite — o token no path faz o papel da credencial.
             */
            new Rota(HttpMethod.POST, "/auth/refresh"),
            // sair tambem: quem sai pode estar com o JWT ja vencido, e falhar ao sair deixaria a
            // sessao aberta no servidor por causa de um token que nao importa mais
            new Rota(HttpMethod.POST, "/auth/logout"),
            // quem esqueceu a senha nao tem como se autenticar para pedir a troca
            new Rota(HttpMethod.POST, "/auth/password-reset"),
            new Rota(HttpMethod.POST, "/auth/password-reset/confirm"),
            // quem clica no link do e-mail pode nem ter feito login: o token do
            // e-mail e a credencial do fluxo
            new Rota(HttpMethod.POST, "/auth/email-verification/resend"),
            new Rota(HttpMethod.POST, "/auth/email-verification/confirm"),
            // sem cadastro publico nao existe primeiro usuario
            new Rota(HttpMethod.POST, "/persons"),
            // o cartao: quem recebe o link nao tem conta, e o token no path faz o
            // papel da credencial
            new Rota(HttpMethod.GET, "/share/{token}"),
            // quem achou um animal na rua nao vai criar conta as 23h com o bicho no colo. Aqui a
            // credencial e o proprio numero do microchip — e por isso esta rota, sozinha entre as
            // publicas do produto, depende do limite por IP para nao virar uma listagem de
            // tutores com telefone
            new Rota(HttpMethod.POST, "/found")
    );

    /**
     * Publicas <b>de infraestrutura</b>: nao sao produto e nao interessam ao cliente
     * gerado. A documentacao precisa abrir sem token, senao nao serve para explorar
     * a API; e o health e chamado pelo provedor de hospedagem sem credencial, senao
     * ele le 401, conclui que a instancia morreu e reinicia em loop.
     */
    public static final List<Rota> DE_INFRA = List.of(
            new Rota(null, "/v3/api-docs/**"),
            new Rota(null, "/swagger-ui/**"),
            new Rota(null, "/swagger-ui.html"),
            new Rota(HttpMethod.GET, "/actuator/health")
    );

    private RotasPublicas() {
    }

}

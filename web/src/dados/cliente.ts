import createClient, { type Middleware } from "openapi-fetch";

import { API } from "./endereco.ts";
import type { paths } from "./gerado/api";
import { encerrarSessao, ensureFresh, lerSessao, organizacaoAtiva } from "./sessao.ts";

/**
 * O unico cliente HTTP da aplicacao.
 *
 * <b>A regra dura: nenhuma tela chama HTTP.</b> Ela existe para manter o BFF possivel —
 * no dia em que a API deixar de ser falada direto pelo navegador, muda esta pasta e nao
 * as telas. O teste `regra-da-camada.test.ts` cobra isso; sem ele a regra seria intencao.
 *
 * <b>Os tipos sao gerados, a costura e nossa</b> (ROADMAP.md, bloco 1). Foi por isso que
 * a decisao recusou gerador de SDK e de hooks: com hooks gerados, a camada de dados
 * passaria a ser gerada, e regenera-la no dia do BFF encostaria em toda tela.
 */

export const cliente = createClient<paths>({
  baseUrl: API,
  headers: { "Content-Type": "application/json" },
  /*
   * O COOKIE DO REFRESH VIAJA, E O RESTO CONTINUA IGUAL.
   *
   * `include` e o que faz o navegador mandar o cookie httpOnly para uma origem diferente — o front
   * esta em :5173 e a API em :8080. Sem isto o `/auth/refresh` existiria e nunca receberia nada.
   *
   * <b>Nao e o mesmo que autenticar por cookie.</b> O que autoriza cada requisicao continua sendo o
   * JWT no header `Authorization`, e o cookie so e aceito pelas rotas de `/auth` — e o `Path` dele
   * que garante isso, e nao a boa vontade de quem escreve o cliente.
   */
  credentials: "include",
});

/**
 * <b>Por que nao existe uma lista de rotas publicas aqui.</b> O contrato marca as sete
 * com `security: []`, mas o `.d.ts` gerado nao carrega essa informacao — reescrever a
 * lista no front criaria a segunda fonte que o `RotasPublicas` do backend existe para
 * evitar, e a que perde e sempre a copia.
 *
 * Entao a regra e por ausencia: sem sessao, a requisicao sai sem `Authorization`. Rota
 * publica funciona; rota protegida volta 401, e o `onResponse` abaixo trata.
 */
const autenticacao: Middleware = {
  async onRequest({ request }) {
    const token = await ensureFresh();
    if (token === null) {
      return request;
    }

    request.headers.set("Authorization", `Bearer ${token}`);

    const organizacao = organizacaoAtiva();
    if (organizacao !== null) {
      request.headers.set("X-Petfy-Organization", organizacao);
    }

    return request;
  },

  onResponse({ response }) {
    if (response.status !== 401) {
      return response;
    }

    /*
     * 401 com sessao viva significa que o servidor recusou um token que ainda nao tinha
     * expirado — e o caso conhecido e a troca de senha, que invalida por `iat`. Nao e
     * erro, e a mensagem para quem le a tela e outra: por isso o motivo e distinto de
     * "expirada".
     */
    if (lerSessao().autenticada) {
      encerrarSessao("invalidada");
    }

    return response;
  },
};

cliente.use(autenticacao);

import createClient from "openapi-fetch";
import { describe, expect, it } from "vitest";

import type { paths } from "./gerado/api";

/**
 * O `Pageable` do Spring e a unica coisa do contrato que o cliente nao pode obedecer
 * literalmente.
 *
 * O springdoc o documenta como UM parametro de query chamado `pageable`; o Spring le
 * `page`, `size` e `sort` <b>soltos</b>. Se o cliente emitir `?pageable=...`, o servidor
 * ignora e devolve a primeira pagina — que por acaso e o que queremos, e por acaso e a
 * pior forma de estar certo: funcionaria na area do tutor e falharia calado na area de
 * organizacao, que le centenas.
 *
 * Este teste fixa o que o cliente emite hoje. Se um dia o gerador passar a serializar o
 * objeto vazio como `?pageable=`, ele quebra aqui e nao na tela.
 */
describe("o parametro pageable", () => {
  it("nao vira query nenhuma quando vai vazio", async () => {
    let urlPedida = "";

    const cliente = createClient<paths>({
      baseUrl: "http://exemplo.invalido",
      fetch: async (requisicao: Request) => {
        urlPedida = requisicao.url;
        return new Response(JSON.stringify({ content: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      },
    });

    await cliente.GET("/animals", { params: { query: { pageable: {} } } });

    expect(urlPedida).toBe("http://exemplo.invalido/animals");
  });
});

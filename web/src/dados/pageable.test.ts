import createClient from "openapi-fetch";
import { describe, expect, it } from "vitest";

import type { paths } from "./gerado/api";

/**
 * A paginacao do Spring chega ao cliente como tres parametros soltos — `page`, `size` e
 * `sort` —, e nao como um objeto `pageable`.
 *
 * Ate 2026-08-08 o springdoc documentava UM parametro de query chamado `pageable`, e
 * ainda por cima `required`. O Spring nunca leu isso: ele le os tres soltos. O cliente
 * gerado era entao obrigado a emitir algo que o servidor ignora, e recebia sempre a
 * primeira pagina — o que por acaso era o que a area do tutor queria, e falharia calado
 * na area de organizacao, que le centenas. O `springdoc.default-flat-param-object=true`
 * desfez a mentira no contrato.
 *
 * Este teste fixa o formato do que sai na URL. Se alguem regenerar o contrato sem aquela
 * propriedade, o `pageable` volta e quebra aqui — na compilacao dos tipos e nestas duas
 * asserções —, e nao calado na tela.
 */
describe("a paginacao na URL", () => {
  function clienteQueGravaAUrl() {
    const gravado = { url: "" };

    const cliente = createClient<paths>({
      baseUrl: "http://exemplo.invalido",
      fetch: async (requisicao: Request) => {
        gravado.url = requisicao.url;
        return new Response(JSON.stringify({ content: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      },
    });

    return { cliente, gravado };
  }

  it("nao emite query nenhuma quando ninguem pede pagina", async () => {
    const { cliente, gravado } = clienteQueGravaAUrl();

    await cliente.GET("/animals");

    expect(gravado.url).toBe("http://exemplo.invalido/animals");
  });

  it("emite page, size e sort soltos — nunca um objeto pageable", async () => {
    const { cliente, gravado } = clienteQueGravaAUrl();

    await cliente.GET("/animals", {
      params: { query: { page: 2, size: 50, sort: ["name,asc"] } },
    });

    const query = new URL(gravado.url).searchParams;

    expect(query.get("page")).toBe("2");
    expect(query.get("size")).toBe("50");
    expect(query.getAll("sort")).toEqual(["name,asc"]);
    expect(gravado.url).not.toContain("pageable");
  });
});

import { describe, expect, it } from "vitest";

import { corpoDe } from "./resposta.ts";

/**
 * A normalizacao de nulos que morava aqui saiu, com o defeito que ela cobria: o backend
 * agora omite o campo nulo, e quem cobra isso e o `RespostaOmiteNulosTest`, no build do
 * backend. Testar aqui de novo seria afirmar sobre um dado que este lado nao produz.
 */
describe("corpoDe", () => {
  it("devolve o corpo como a API o entregou", () => {
    expect(corpoDe({ data: { nome: "Code" } })).toEqual({ nome: "Code" });
  });

  it("lanca o erro da API como ele veio, para o i18n traduzir por codigo", () => {
    const erro = { code: 119, status: 403, message: "irrelevante", timestamp: "" };

    expect(() => corpoDe({ error: erro })).toThrow();
    try {
      corpoDe({ error: erro });
    } catch (lancado) {
      expect(lancado).toBe(erro);
    }
  });

  it("recusa corpo ausente onde havia corpo esperado", () => {
    expect(() => corpoDe({})).toThrow();
  });
});

import { describe, expect, it } from "vitest";

import { corpoDe, semNulos } from "./resposta.ts";

/**
 * O caso que quebrou a home na primeira vez que ela abriu com dados de verdade: o feed
 * chegou com `lastFulfilledAt: null`, o guard `!== undefined` deixou passar, e o
 * `.slice()` estourou. O tipo gerado dizia `string | undefined`.
 */
describe("semNulos", () => {
  it("tira a chave nula, para que ler devolva undefined", () => {
    const pendencia = semNulos({
      description: "Antirrábica",
      lastFulfilledAt: null,
      lastFulfilledByName: null,
    });

    expect(pendencia.lastFulfilledAt).toBeUndefined();
    expect("lastFulfilledAt" in pendencia).toBe(false);
    expect(pendencia.description).toBe("Antirrábica");
  });

  it("desce em lista e em objeto aninhado", () => {
    const contexto = semNulos({
      available: [{ organizationName: null, kind: "PESSOA" }],
      active: { organizationId: null, role: "VETERINARIO" },
    });

    expect(contexto.available[0]?.organizationName).toBeUndefined();
    expect(contexto.available[0]?.kind).toBe("PESSOA");
    expect(contexto.active.organizationId).toBeUndefined();
  });

  it("nao confunde vazio com nulo", () => {
    // `false`, `0` e `""` sao valores, e sumir com eles seria trocar um bug por outro.
    const item = semNulos({ overdue: false, dias: 0, nota: "", silenced: null });

    expect(item.overdue).toBe(false);
    expect(item.dias).toBe(0);
    expect(item.nota).toBe("");
    expect(item.silenced).toBeUndefined();
  });
});

describe("corpoDe", () => {
  it("devolve o corpo ja normalizado", () => {
    expect(corpoDe({ data: { nome: "Code", raca: null } })).toEqual({ nome: "Code" });
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

import { describe, expect, it } from "vitest";

import { montarCarteira } from "./carteira.ts";
import type { components } from "./gerado/api";

type Status = NonNullable<components["schemas"]["VaccineAgendaItemDTO"]["status"]>;

const CODE = "11111111-1111-1111-1111-111111111111";
const OUTRO = "22222222-2222-2222-2222-222222222222";

const dose = (nome: string, extra: Record<string, unknown> = {}) => ({
  animalId: CODE,
  vaccineName: nome,
  applicationDate: "2024-03-12",
  ...extra,
});

const item = (nome: string, status: Status, extra: Record<string, unknown> = {}) => ({
  animalId: CODE,
  vaccineName: nome,
  status,
  ...extra,
});

const doCatalogo = (nome: string) => ({ name: nome });

describe("a carteira de vacinacao", () => {
  /**
   * <b>O caso que motivou o teste, e ele mentia de um jeito caro.</b>
   *
   * A agenda so lista o que pede acao. Montar a carteira a partir dela fazia a dose EM DIA
   * desaparecer da lista e reaparecer pelo catalogo como "sem registro" — o produto
   * dizendo "nunca tomou" sobre uma dose que ele mesmo guardou.
   */
  it("a dose em dia aparece como em dia, e nao como sem registro", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [],
      doses: [dose("V10", { nextDoseDate: "2027-03-09" })],
      catalogo: [doCatalogo("V10")],
    });

    expect(linhas).toHaveLength(1);
    expect(linhas[0]!.estado).toBe("emDia");
    expect(linhas[0]!.proximaDose).toBe("2027-03-09");
  });

  it("o estado vencida vem da agenda, e nao de conta sobre a data", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [
        item("Antirrabica", "OVERDUE", { nextDoseDate: "2025-03-12", daysUntilNextDose: -515 }),
      ],
      doses: [dose("Antirrabica", { nextDoseDate: "2025-03-12" })],
      catalogo: [doCatalogo("Antirrabica")],
    });

    expect(linhas[0]!.estado).toBe("vencida");
    expect(linhas[0]!.diasAteProximaDose).toBe(-515);
  });

  /** O quarto marcador da secao 05 — o anel tracejado —, que so existe cruzando as duas. */
  it("a vacina do catalogo sem nenhuma dose vira sem registro", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [],
      doses: [],
      catalogo: [doCatalogo("Gripe canina")],
    });

    expect(linhas).toHaveLength(1);
    expect(linhas[0]!.estado).toBe("semRegistro");
  });

  it("dose sem proxima dose prevista nao vira em dia nem vencida", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [],
      doses: [dose("Giardia")],
      catalogo: [],
    });

    expect(linhas[0]!.estado).toBe("semProximaDose");
  });

  /** Uma serie, uma linha: o historico e a linha do tempo, nao a carteira. */
  it("duas doses da mesma vacina viram uma linha, com a mais recente", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [],
      doses: [
        dose("Antirrabica", { applicationDate: "2023-03-12", nextDoseDate: "2024-03-12" }),
        dose("Antirrabica", { applicationDate: "2026-03-12", nextDoseDate: "2027-03-12" }),
      ],
      catalogo: [doCatalogo("Antirrabica")],
    });

    expect(linhas).toHaveLength(1);
    expect(linhas[0]!.proximaDose).toBe("2027-03-12");
  });

  /**
   * As duas leituras trazem os animais TODOS que a pessoa alcanca. Sem o filtro, a carteira
   * do Code mostraria a vacina do gato — e o gato tem outro catalogo.
   */
  it("ignora dose e pendencia de outro animal", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [item("V4", "OVERDUE", { animalId: OUTRO, nextDoseDate: "2025-01-01" })],
      doses: [dose("V4", { animalId: OUTRO, nextDoseDate: "2025-01-01" })],
      catalogo: [],
    });

    expect(linhas).toEqual([]);
  });

  it("a juncao por nome ignora caixa e espaco em volta", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [],
      doses: [dose("  antirrabica CANINA ", { nextDoseDate: "2027-01-01" })],
      catalogo: [doCatalogo("Antirrabica canina")],
    });

    expect(linhas).toHaveLength(1);
    expect(linhas[0]!.estado).toBe("emDia");
  });

  /** "Se tudo interrompe, nada interrompe" (secao 05): a ordem e a hierarquia mais barata. */
  it("ordena por gravidade, e nao por nome", () => {
    const linhas = montarCarteira({
      animalId: CODE,
      daAgenda: [
        item("Antirrabica", "OVERDUE", { nextDoseDate: "2025-03-12" }),
        item("Leishmaniose", "DUE_SOON", { nextDoseDate: "2026-09-01" }),
      ],
      doses: [
        dose("Antirrabica", { nextDoseDate: "2025-03-12" }),
        dose("Leishmaniose", { nextDoseDate: "2026-09-01" }),
        dose("V10", { nextDoseDate: "2027-03-09" }),
      ],
      catalogo: [doCatalogo("Gripe canina")],
    });

    expect(linhas.map((l) => l.estado)).toEqual([
      "vencida",
      "vencendo",
      "semRegistro",
      "emDia",
    ]);
  });
});

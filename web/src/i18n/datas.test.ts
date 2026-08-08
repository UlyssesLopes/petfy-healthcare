import { describe, expect, it } from "vitest";

import { dataLocalDe, diasAte } from "./datas.ts";

/**
 * O bug que este teste existe para impedir: `new Date("2026-08-10")` e meia-noite UTC,
 * que em qualquer fuso a oeste de Greenwich cai no dia anterior. Sem isso, uma vacina que
 * vence hoje aparece como vencida ontem — e a linha do feed passa a mentir sobre o fato,
 * que e o oposto do que a secao 2 do DESIGN.md exige.
 */
describe("dataLocalDe", () => {
  it("le o dia que a string nomeia, e nao o dia em UTC", () => {
    const data = dataLocalDe("2026-08-10");

    expect(data.getFullYear()).toBe(2026);
    expect(data.getMonth()).toBe(7); // agosto
    expect(data.getDate()).toBe(10);
  });

  it("difere do construtor com string, que e o erro que se quer evitar", () => {
    // Prova que o cuidado nao e teorico: so vale a pena manter a funcao se os dois
    // resultados forem mesmo diferentes no fuso em que o teste roda.
    const cru = new Date("2026-08-10");
    const nosso = dataLocalDe("2026-08-10");

    if (cru.getTimezoneOffset() > 0) {
      expect(cru.getDate()).not.toBe(nosso.getDate());
    } else {
      expect(cru.getDate()).toBe(nosso.getDate());
    }
  });

  it("recusa o que nao e uma data", () => {
    expect(() => dataLocalDe("ontem")).toThrow();
  });
});

describe("diasAte", () => {
  const hoje = new Date(2026, 7, 7, 23, 30); // 7 de agosto, 23h30

  it("conta por dia do calendario, e nao por 24 horas", () => {
    // As 23h30, "amanha" ainda e 1 - contar por 24h daria 0.
    expect(diasAte("2026-08-08", hoje)).toBe(1);
  });

  it("da zero para hoje, em qualquer hora do dia", () => {
    expect(diasAte("2026-08-07", hoje)).toBe(0);
    expect(diasAte("2026-08-07", new Date(2026, 7, 7, 0, 1))).toBe(0);
  });

  it("da negativo para o que ja passou", () => {
    expect(diasAte("2026-08-04", hoje)).toBe(-3);
  });
});

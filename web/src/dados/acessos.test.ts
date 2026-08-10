import { describe, expect, it } from "vitest";

import { mensagens } from "../i18n/mensagens/pt-BR.ts";
import { fimDoDia, FRASE_DO_ESCOPO } from "./acessos.ts";

/**
 * O prazo que a Tela 09 grava, e o que ele significa para quem le.
 *
 * O campo do desenho e uma data seca — "31/12/2027". O contrato pede `date-time`, e a
 * conversao nao e cosmetica: escolher a meia-noite encurta o acesso em um dia inteiro, e
 * quem descobre e a clinica, na porta fechada, no dia em que o tutor jurava ter dado prazo.
 */
describe("fimDoDia", () => {
  it("estende a data ao ultimo segundo do dia, e nao a meia-noite", () => {
    expect(fimDoDia("2027-12-31")).toBe("2027-12-31T23:59:59");
  });

  it("nao carrega fuso: comparar com fuso e a divida 3, que tem PR proprio", () => {
    expect(fimDoDia("2026-08-10")).not.toContain("Z");
    expect(fimDoDia("2026-08-10")).not.toMatch(/[+-]\d\d:\d\d$/);
  });
});

/**
 * <b>A tela nunca diz "escopo", e nunca diz o nome da constante.</b> A secao 09 da voz
 * proibe a palavra com o tutor, e este teste e o que impede a proibicao de virar intencao:
 * cada valor do enum precisa ter frase, e a frase nao pode ser o proprio codigo.
 *
 * Os sete valores vem do contrato (`GrantScope`). Se o backend ganhar o oitavo, este teste
 * nao acusa — ele cobra o que existe hoje, e a lista mora no `FRASE_DO_ESCOPO`.
 */
describe("FRASE_DO_ESCOPO", () => {
  const escopos = ["CARTEIRA", "CONDICOES", "PRONTUARIO", "OBSERVACOES", "PESO", "ANEXOS", "CONTATO"];

  it("tem frase traduzida para todo escopo que a API concede", () => {
    for (const escopo of escopos) {
      const chave = FRASE_DO_ESCOPO[escopo];

      expect(chave, escopo).toBeDefined();
      expect(mensagens[chave as keyof typeof mensagens], chave).toBeDefined();
    }
  });

  it("nenhuma frase mostra a constante nem a palavra proibida", () => {
    for (const escopo of escopos) {
      const frase = mensagens[FRASE_DO_ESCOPO[escopo] as keyof typeof mensagens] as string;

      expect(frase, escopo).not.toContain(escopo);
      expect(frase.toLowerCase(), escopo).not.toContain("escopo");
    }
  });
});

/**
 * As quatro linhas concediveis da Tela 09 tem titulo e motivo, e o motivo nunca e o nome do
 * dado. O desenho e explicito: nomear o que a organizacao vai VER, e nunca a abstracao.
 */
describe("as linhas da Tela 09", () => {
  const linhas = ["CONDICOES", "CARTEIRA", "PRONTUARIO", "OBSERVACOES"];

  it("cada uma tem titulo e por que, e nenhuma diz a palavra proibida", () => {
    for (const linha of linhas) {
      const titulo = mensagens[`conceder.item.${linha}` as keyof typeof mensagens] as string;
      const porque = mensagens[`conceder.item.${linha}.porque` as keyof typeof mensagens] as string;

      expect(titulo, linha).toBeDefined();
      expect(porque, linha).toBeDefined();
      expect(`${titulo} ${porque}`.toLowerCase(), linha).not.toContain("escopo");
    }
  });

  it("a linha de registrar atendimento diz por que nao da, e nao fica muda", () => {
    const motivo = mensagens["conceder.item.registrar.indisponivel"];

    expect(motivo).toBeDefined();
    expect(motivo.length).toBeGreaterThan(40);
  });
});

import { describe, expect, it } from "vitest";

import { mensagens } from "./mensagens/pt-BR.ts";

/**
 * As regras de voz que valem por tela, e que so um teste impede de virar intencao.
 *
 * Nao e teste de traducao — e de <b>promessa</b>. Uma frase pode estar bem escrita, passar
 * no `tsc`, aparecer bonita na tela e ainda assim afirmar algo que o servidor nao faz.
 * Quando isso acontece, quem descobre e a pessoa que confiou.
 */

describe("a Tela 11 nao promete revogacao que o backend nao faz", () => {
  /*
   * O desenho pede "quem deixa de ver o Code", com co-tutora e organizacoes perdendo acesso
   * no aceite. Conferido no PetTutorServiceImpl, nos dois caminhos de transferencia: NENHUMA
   * concessao e revogada — nem de pessoa, nem de organizacao. O titular anterior ainda ganha
   * uma concessao EDITOR, que e escrita.
   *
   * Se alguem "consertar" estes textos na direcao do desenho sem mexer no backend, este
   * teste cai. E ele deve cair junto no dia em que o backend passar a revogar: ai as duas
   * coisas mudam no mesmo commit, que e o ponto.
   */
  const frases = [
    mensagens["transferir.continua.voce.texto"],
    mensagens["transferir.continua.coTutor.texto"],
    mensagens["transferir.continua.organizacao.texto"],
    mensagens["transferir.continua.tese"],
  ];

  it("nenhuma frase diz que alguem perde ou deixa de ver", () => {
    for (const frase of frases) {
      expect(frase.toLowerCase(), frase).not.toMatch(/perde|deixa de ver|deixam de ver/);
    }
  });

  it("o rotulo do painel fala de quem CONTINUA, e nao de quem sai", () => {
    expect(mensagens["transferir.continua.rotulo"].toLowerCase()).toContain("continua");
  });

  it("a frase do titular anterior nao chama de leitura o que e escrita", () => {
    // O backend concede EDITOR ao titular anterior. Prometer "leitura" seria descrever
    // menos poder do que a pessoa realmente mantem sobre o animal.
    const texto = mensagens["transferir.continua.voce.texto"].toLowerCase();

    expect(texto).toContain("registrar");
    expect(texto).not.toMatch(/somente leitura|apenas leitura|so leitura/);
  });
});

describe("o campo sem destino no servidor explica, e nao fica mudo", () => {
  /*
   * A secao 06 da identidade: "o desabilitado nunca aparece mudo, e a frase vem antes do
   * gesto, nao depois". Sao dois hoje — o motivo da transferencia e registrar atendimento
   * na Tela 09 —, e os dois so podem existir desabilitados com a razao escrita.
   */
  const motivos = [
    mensagens["transferir.motivo.indisponivel"],
    mensagens["conceder.item.registrar.indisponivel"],
  ];

  it("cada afordancia desabilitada tem frase, e a frase diz por que", () => {
    for (const motivo of motivos) {
      expect(motivo.length, motivo).toBeGreaterThan(40);
      expect(motivo.toLowerCase(), motivo).toMatch(/não|nao/);
    }
  });
});

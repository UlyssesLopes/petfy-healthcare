import { describe, expect, it } from "vitest";

import { mensagens } from "./mensagens/pt-BR.ts";

/**
 * As regras de voz que valem por tela, e que so um teste impede de virar intencao.
 *
 * Nao e teste de traducao — e de <b>promessa</b>. Uma frase pode estar bem escrita, passar
 * no `tsc`, aparecer bonita na tela e ainda assim afirmar algo que o servidor nao faz.
 * Quando isso acontece, quem descobre e a pessoa que confiou.
 */

describe("a Tela 11 promete a revogacao que o backend agora faz", () => {
  /*
   * ESTE BLOCO ERA O CONTRARIO, E CAIU DE PROPOSITO.
   *
   * Ele travava as frases na verdade de entao: "nenhuma frase diz que alguem perde acesso",
   * porque o `PetTutorServiceImpl` nao revogava nada e ainda dava EDITOR ao titular anterior.
   * Estava escrito ali que ele deveria cair no dia em que o backend passasse a revogar, com as
   * duas coisas mudando no mesmo commit. Foi o que aconteceu.
   *
   * Agora ele guarda a promessa nova, e pela mesma razao: no aceite, `revogarAcessosHerdados`
   * derruba TODA concessao vigente do animal — pessoa, organizacao e link —, e o titular
   * anterior recebe VIEWER. Se alguem voltar o backend atras sem mexer aqui, este teste cai.
   */
  it("as frases dizem que o acesso cai no aceite", () => {
    expect(mensagens["transferir.continua.coTutor.texto"].toLowerCase()).toMatch(/perde/);
    expect(mensagens["transferir.continua.organizacao.texto"].toLowerCase()).toMatch(/perde/);
  });

  it("o link compartilhado e nomeado, porque e o acesso que ninguem ve na tela", () => {
    expect(mensagens["transferir.continua.organizacao.texto"].toLowerCase()).toContain("link");
  });

  it("a tese e a do desenho: acessos nao sao herdados", () => {
    expect(mensagens["transferir.continua.tese"].toLowerCase()).toContain("não são herdados");
  });

  it("o titular anterior fica com leitura, e a frase nao promete registrar", () => {
    // O backend concede VIEWER. Dizer "registrar" aqui prometeria um poder que a pessoa
    // perdeu no aceite — o defeito simetrico do que este bloco guardava antes.
    const texto = mensagens["transferir.continua.voce.texto"].toLowerCase();

    expect(texto).toMatch(/ver|leitura/);
    expect(texto).not.toMatch(/permissão de registrar|com permissão de registrar/);
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

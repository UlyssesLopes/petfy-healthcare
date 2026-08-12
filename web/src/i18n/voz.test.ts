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

describe("a Tela 33 nao promete o que o produto nao faz", () => {
  /*
   * O desenho e explicito sobre o silencio depois do registro: "nao manda condolencias
   * automaticas, nao sugere adotar outro, nao pergunta a causa da morte". Duas dessas tres sao
   * fáceis de quebrar com uma frase gentil escrita de boa-fe, e por isso ficam aqui.
   */
  /*
   * A caixa "o que o Petfy nao faz" fica de fora, e nao por conveniencia: ela e a unica frase da
   * tela que CITA essas coisas para negá-las. Inclui-la faria a declaracao da regra ser lida como
   * violacao dela — e a saida obvia, afrouxar o padrao, deixaria passar a frase de verdade.
   */
  const daTela = Object.entries(mensagens)
    .filter(([chave]) => chave.startsWith("fim.") && !chave.startsWith("fim.oQueNaoFazemos"))
    .map(([, texto]) => texto.toLowerCase());

  it("nenhuma frase sugere adotar outro animal", () => {
    for (const texto of daTela) {
      expect(texto, texto).not.toMatch(/outro animal|adotar|novo bicho/);
    }
  });

  it("nenhuma frase pergunta a causa da morte", () => {
    for (const texto of daTela) {
      expect(texto, texto).not.toMatch(/causa|do que ele morreu|motivo da morte/);
    }
  });

  it("a unica concessao emocional e o 'sentimos muito' do desenho, e ela e uma so", () => {
    const comPesames = daTela.filter((texto) => /sentimos muito|lamentamos|nossos p[êe]sames/.test(texto));
    expect(comPesames).toHaveLength(1);
  });

  /*
   * A linha "quem cuida dele é avisado" so pode existir porque o `AnimalDeathNotifier` avisa de
   * fato — pessoas e organizacoes, por e-mail. Se alguem tirar o notificador do servico, esta
   * frase vira promessa vazia, e e este teste que precisa cair junto.
   */
  it("o aviso e afirmado, e o backend o faz", () => {
    expect(mensagens["fim.acontece.organizacoes"].toLowerCase()).toMatch(/avisad/);
  });

  it("o que fica guardado e dito, porque encerrar nao e apagar", () => {
    expect(mensagens["fim.acontece.historico"].toLowerCase()).toMatch(/continua|inteiro/);
  });
});

describe("a Tela 34 promete so o que a rota entrega", () => {
  /*
   * "Nao guardamos quem fez a busca" e verdade: o log registra o acesso sem ator. Ja "e por
   * onde" NAO tem de onde sair — o produto nao sabe onde a pessoa esta —, e a frase do rodape
   * nao pode afirmar isso.
   */
  it("nao promete dizer ao tutor a regiao de quem procurou", () => {
    const daTela = Object.entries(mensagens)
      .filter(([chave]) => chave.startsWith("encontrado."))
      .map(([, texto]) => texto.toLowerCase());

    for (const texto of daTela) {
      expect(texto, texto).not.toMatch(/regi[ãa]o|onde voc[êe] est[áa]|localiza/);
    }
  });

  it("diz que nao guarda quem buscou, e que quem responde e avisado", () => {
    const semConta = mensagens["encontrado.semConta"].toLowerCase();

    expect(semConta).toMatch(/não guardamos/);
    expect(semConta).toMatch(/procurado/);
  });

  /*
   * O vazio dirige para o proximo passo, e nao anuncia falha. A frase mora no `erro.161` porque
   * o cliente traduz por codigo — e e la que ela precisa passar neste teste.
   */
  it("o numero que nao esta no Petfy diz o que fazer em seguida", () => {
    const vazio = mensagens["erro.161"].toLowerCase();

    expect(vazio).toMatch(/cl[íi]nica|prefeitura/);
    expect(vazio).not.toMatch(/nenhum resultado|falhou|erro/);
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

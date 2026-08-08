import { describe, expect, it } from "vitest";

import tabelaCommitada from "../../../contract/error-codes.json" with { type: "json" };
import { chaveDoErro } from "./erroDaApi.ts";
import { mensagens } from "./mensagens/pt-BR.ts";

/**
 * A guarda da tabela de erro, do lado do front.
 *
 * O `ErrorCodesContractTest` mantem o `contract/error-codes.json` verdadeiro sobre o
 * enum; este teste mantem a traducao verdadeira sobre o arquivo. Sem os dois, um codigo
 * novo chega a tela como texto generico sem ninguem ser avisado — e teste que falha em
 * silencio ja custou caro neste projeto.
 *
 * Le a tabela por `import`, e nao por `fs`: ler por `fs` exigiria os tipos do Node no
 * mesmo tsconfig da aplicacao, e ai `process` e `fs` ficariam alcancaveis do codigo que
 * roda no navegador — um buraco permanente aberto por um teste.
 */

interface LinhaDaTabela {
  nome: string;
  codigo: number;
  mensagemDaApi: string;
}

const tabela: LinhaDaTabela[] = tabelaCommitada;

/*
 * So as chaves `erro.<numero>`. O generico fica de fora porque nao corresponde a codigo
 * nenhum, e as mensagens de interface tambem — senao a primeira string de tela seria
 * acusada de orfa, e a guarda passaria a cobrar a coisa errada.
 */
const chavesDeCodigo = Object.keys(mensagens).filter((chave) => /^erro\.\d+$/.test(chave));

describe("a tabela de codigo de erro", () => {
  it("tem os 45 codigos do enum, e nenhum a menos", () => {
    expect(tabela.length).toBeGreaterThan(0);

    const semTraducao = tabela
      .filter((linha) => !(`erro.${linha.codigo}` in mensagens))
      .map((linha) => `${linha.codigo} (${linha.nome})`);

    expect(
      semTraducao,
      "Codigos sem traducao em pt-BR. Se vieram de uma mudanca no ErrorMessageEnum, "
        + "escreva a mensagem seguindo a secao 2 do DESIGN.md antes de fechar.",
    ).toEqual([]);
  });

  it("nao guarda traducao para codigo que o enum nao tem mais", () => {
    const codigosConhecidos = new Set(tabela.map((linha) => `erro.${linha.codigo}`));

    const orfas = chavesDeCodigo.filter((chave) => !codigosConhecidos.has(chave));

    expect(orfas, "Traducoes de codigos que sairam do enum — sobra que confunde a revisao.").toEqual(
      [],
    );
  });

  it("nunca repete a mensagem em ingles da API", () => {
    const copiadas = tabela
      .filter((linha) => {
        const traducao = mensagens[`erro.${linha.codigo}` as keyof typeof mensagens] as
          | string
          | undefined;
        return traducao !== undefined && traducao.trim() === linha.mensagemDaApi.trim();
      })
      .map((linha) => linha.codigo);

    expect(copiadas, "A mensagem da API e referencia, nao texto de tela.").toEqual([]);
  });
});

describe("chaveDoErro", () => {
  it("traduz pelo codigo, e nao pelo status", () => {
    expect(chaveDoErro({ code: 119, status: 403, message: "irrelevante", timestamp: "" })).toBe(
      "erro.119",
    );
  });

  it("cai no generico quando o codigo e desconhecido", () => {
    expect(chaveDoErro({ code: 999, status: 500, message: "", timestamp: "" })).toBe(
      "erro.desconhecido",
    );
  });

  it("cai no generico quando a resposta nem forma de erro tem", () => {
    expect(chaveDoErro("<html>502 Bad Gateway</html>")).toBe("erro.desconhecido");
    expect(chaveDoErro(null)).toBe("erro.desconhecido");
    expect(chaveDoErro(undefined)).toBe("erro.desconhecido");
  });
});

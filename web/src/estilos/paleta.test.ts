import { describe, expect, it } from "vitest";

/**
 * A guarda contra cor que nao existe — e, agora, contra escuro que nao existe.
 *
 * O `@theme` apaga a paleta de fabrica do Tailwind, entao nenhuma cor de fora da secao
 * 02 da entrega de design consegue chegar a tela. <b>Mas apagar nao avisa:</b> uma classe
 * removida nao da erro - ela simplesmente nao emite CSS. Quem colar `bg-red-500` de um
 * exemplo ve um bloco sem fundo e pode achar que e o design.
 */

const fontes = import.meta.glob("/src/**/*.{ts,tsx}", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const estilos = import.meta.glob("/src/**/*.css", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const PREFIXOS = "bg|text|border|ring|fill|stroke|outline|divide|from|via|to|accent|caret|placeholder|shadow";

/** `bg-red-500`, `text-slate-700`, `border-zinc-200` — a assinatura da paleta de fabrica. */
const COR_NUMERADA = new RegExp(`\\b(?:${PREFIXOS})-[a-z]+-\\d{2,3}\\b`, "g");

/** `bg-white` e `text-black`, que nao tem numero e tambem nao estao na secao 02. */
const BRANCO_OU_PRETO = new RegExp(`\\b(?:${PREFIXOS})-(?:white|black)\\b`, "g");

describe("a paleta", () => {
  it("acha os arquivos da aplicacao", () => {
    expect(Object.keys(fontes)).toContain("/src/rotas/entrar.tsx");
    expect(Object.keys(estilos)).toContain("/src/estilos/tokens.css");
  });

  it("nenhum arquivo usa classe de cor que nao existe mais", () => {
    const encontradas: string[] = [];

    for (const [caminho, conteudo] of Object.entries(fontes)) {
      for (const padrao of [COR_NUMERADA, BRANCO_OU_PRETO]) {
        for (const achada of conteudo.match(padrao) ?? []) {
          encontradas.push(`${caminho}: ${achada}`);
        }
      }
    }

    expect(
      encontradas,
      "Classe da paleta de fabrica do Tailwind, que este projeto apagou. Ela nao emite "
        + "CSS nenhum - o elemento fica sem cor, em silencio. Use um token da secao 02 da "
        + "entrega de design; se faltar um, ele entra na paleta com o contraste medido antes.",
    ).toEqual([]);
  });

  /**
   * <b>Por que a variante `dark:` e proibida, e por que isso nao e pedantismo.</b>
   *
   * Nao existe tema escuro: a secao 08 da entrega declara a intencao e da quatro valores,
   * o que nao e um tema. O `tokens.css` fixa `color-scheme: light` por causa disso — mas
   * `color-scheme` governa o que o NAVEGADOR desenha (campo, barra de rolagem), e nao a
   * media query. Uma classe `dark:` continuaria valendo para quem tem o sistema em escuro,
   * e o resultado seria meia tela escura sobre papel claro: pior que nao ter escuro.
   *
   * Quando a entrega trouxer o escuro por extenso, quem sai e esta guarda — nao o teste.
   */
  it("ninguem escreve tema escuro enquanto ele nao existe", () => {
    const encontradas: string[] = [];

    for (const [caminho, conteudo] of Object.entries(fontes)) {
      for (const achada of conteudo.match(/\bdark:[a-z-]+/g) ?? []) {
        encontradas.push(`${caminho}: ${achada}`);
      }
    }

    for (const [caminho, conteudo] of Object.entries(estilos)) {
      if (/prefers-color-scheme/.test(conteudo)) {
        encontradas.push(`${caminho}: prefers-color-scheme`);
      }
    }

    expect(
      encontradas,
      "Tema escuro nao existe neste produto ainda. A entrega de design da quatro valores "
        + "escuros e nenhum para tinta, linha, tinta secundaria ou telha - um escuro aqui "
        + "seria cor inventada passando por design aprovado. Ver a nota no tokens.css.",
    ).toEqual([]);
  });

  /**
   * A cor mora no `tokens.css`, e so nele. O `global.css` a alcanca por `var(--petfy-*)`;
   * um nome trocado de lado nao quebra nada visivelmente — o Tailwind emite a variavel
   * vazia e o elemento fica sem cor, do mesmo jeito silencioso de sempre.
   */
  it("todo token citado na costura existe na paleta", () => {
    const tokens = estilos["/src/estilos/tokens.css"] ?? "";
    const costura = estilos["/src/estilos/global.css"] ?? "";

    const declarados = new Set(
      [...tokens.matchAll(/^\s*(--petfy-[a-z-]+)\s*:/gm)].map((m) => m[1]),
    );
    const citados = new Set(
      [...costura.matchAll(/var\((--petfy-[a-z-]+)\)/g)].map((m) => m[1]),
    );

    const orfaos = [...citados].filter((t) => !declarados.has(t));

    expect(orfaos, "Token citado no global.css que o tokens.css nao declara.").toEqual([]);
  });

  /**
   * A entrega escreve a cor em oklch, e o projeto tambem. Nao e preciosismo de formato:
   * musgo, ocre e telha sao tres matizes (150, 70, 30) que se movem so em luminosidade e
   * croma, e em hex essa relacao vira quinze numeros sem parentesco visivel. Um hex solto
   * num componente e uma cor que ninguem mediu.
   */
  it("nenhuma cor literal fora da paleta", () => {
    const encontradas: string[] = [];
    const literais = /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(/g;

    for (const [caminho, conteudo] of Object.entries({ ...fontes, ...estilos })) {
      if (caminho === "/src/estilos/tokens.css") continue;

      for (const achada of conteudo.match(literais) ?? []) {
        encontradas.push(`${caminho}: ${achada}`);
      }
    }

    expect(
      encontradas,
      "Cor literal fora do tokens.css. Toda cor deste produto tem um numero de contraste "
        + "medido ao lado dela, e uma cor escrita direto no componente nao tem.",
    ).toEqual([]);
  });
});

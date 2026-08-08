import { describe, expect, it } from "vitest";

/**
 * A guarda contra classe de cor que nao existe.
 *
 * O `@theme` apaga a paleta de fabrica do Tailwind, entao nenhuma cor de fora da secao 3
 * do DESIGN.md consegue chegar a tela. <b>Mas apagar nao avisa:</b> uma classe removida
 * nao da erro - ela simplesmente nao emite CSS. Quem colar `bg-red-500` de um exemplo ve
 * um bloco sem fundo e pode achar que e o design.
 *
 * Entao a guarda e pelo formato do nome. A paleta do Tailwind tem uma assinatura -
 * `cor-numero` -, e mais `white` e `black`, que a secao 3 tambem nao usa: a superficie
 * escura e quente (`#141311`) justamente para nao ser `#000000`.
 *
 * <b>Por que pelo formato, e nao por lista branca dos tokens:</b> os mesmos prefixos
 * carregam utilitario que nao e cor - `text-center`, `border-2`, `outline-none` -, e uma
 * lista branca teria de prever todos eles. Reprovaria codigo correto, e guarda que
 * reprova o certo e desligada na primeira pressa.
 */

const arquivos = import.meta.glob("/src/**/*.{ts,tsx}", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const PREFIXOS = "bg|text|border|ring|fill|stroke|outline|divide|from|via|to|accent|caret|placeholder|shadow";

/** `bg-red-500`, `text-slate-700`, `border-zinc-200` — a assinatura da paleta de fabrica. */
const COR_NUMERADA = new RegExp(`\\b(?:${PREFIXOS})-[a-z]+-\\d{2,3}\\b`, "g");

/** `bg-white` e `text-black`, que nao tem numero e tambem nao estao na secao 3. */
const BRANCO_OU_PRETO = new RegExp(`\\b(?:${PREFIXOS})-(?:white|black)\\b`, "g");

describe("a paleta", () => {
  it("acha os arquivos da aplicacao", () => {
    expect(Object.keys(arquivos)).toContain("/src/rotas/entrar.tsx");
  });

  it("nenhum arquivo usa classe de cor que nao existe mais", () => {
    const encontradas: string[] = [];

    for (const [caminho, conteudo] of Object.entries(arquivos)) {
      for (const padrao of [COR_NUMERADA, BRANCO_OU_PRETO]) {
        for (const achada of conteudo.match(padrao) ?? []) {
          encontradas.push(`${caminho}: ${achada}`);
        }
      }
    }

    expect(
      encontradas,
      "Classe da paleta de fabrica do Tailwind, que este projeto apagou. Ela nao emite "
        + "CSS nenhum - o elemento fica sem cor, em silencio. Use um token da secao 3 do "
        + "DESIGN.md; se faltar um, ele entra na paleta com o contraste medido antes.",
    ).toEqual([]);
  });
});

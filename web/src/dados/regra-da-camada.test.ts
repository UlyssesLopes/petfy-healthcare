import { describe, expect, it } from "vitest";

/**
 * A guarda da regra mais importante desta pasta: <b>nenhuma tela chama HTTP.</b>
 *
 * Ela existe para manter o BFF possivel — no dia em que a API deixar de ser falada
 * direto pelo navegador, muda `src/dados/` e nao as telas. Escrita num documento, a
 * regra dura ate a primeira pressa; escrita aqui, ela quebra o build.
 *
 * <b>Le por `import.meta.glob`, e nao por `fs`</b>, pelo mesmo motivo do teste do i18n:
 * `fs` exigiria os tipos do Node no tsconfig da aplicacao, deixando `process` alcancavel
 * do codigo que roda no navegador.
 */

const arquivos = import.meta.glob("/src/**/*.{ts,tsx}", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

/*
 * Este arquivo cita todos os nomes proibidos e nao se denuncia: o Vite exclui do
 * resultado o proprio arquivo que declara o glob. Se um dia isso mudar, o teste passa a
 * acusar a si mesmo — e a correcao e filtrar por caminho, nao afrouxar o padrao.
 */
const foraDaCamada = Object.entries(arquivos).filter(
  ([caminho]) => !caminho.startsWith("/src/dados/"),
);

const proibicoes: { nome: string; padrao: RegExp }[] = [
  { nome: "o cliente HTTP", padrao: /from\s+["'][^"']*dados\/cliente(\.ts)?["']/ },
  { nome: "os tipos gerados do contrato", padrao: /from\s+["'][^"']*dados\/gerado\// },
  { nome: "o openapi-fetch direto", padrao: /from\s+["']openapi-fetch["']/ },
  { nome: "fetch cru", padrao: /\bfetch\s*\(/ },
  { nome: "XMLHttpRequest", padrao: /\bXMLHttpRequest\b/ },
];

describe("a camada de dados e unica", () => {
  /*
   * Sem esta verificacao, um glob que deixasse de casar com qualquer coisa faria o teste
   * seguinte passar varrendo zero arquivo — a guarda verde sem ter guardado nada, que e
   * exatamente a falha que este projeto ja teve com os testes de container.
   */
  it("acha os arquivos da aplicacao", () => {
    const caminhos = Object.keys(arquivos);

    expect(caminhos.length).toBeGreaterThan(5);
    expect(caminhos).toContain("/src/main.tsx");
    expect(caminhos).toContain("/src/rotas/index.tsx");
    expect(caminhos).toContain("/src/dados/cliente.ts");
  });

  it("nenhum arquivo fora de src/dados/ fala HTTP", () => {
    const infratores: string[] = [];

    for (const [caminho, conteudo] of foraDaCamada) {
      for (const proibicao of proibicoes) {
        if (proibicao.padrao.test(conteudo)) {
          infratores.push(`${caminho} usa ${proibicao.nome}`);
        }
      }
    }

    expect(
      infratores,
      "Tela falando HTTP. O que a tela precisa e um hook em src/dados/ — a fronteira e o "
        + "que mantem o BFF possivel, e o que impede que muda-la encoste em toda tela.",
    ).toEqual([]);
  });
});

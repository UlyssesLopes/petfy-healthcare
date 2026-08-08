import { createFileRoute } from "@tanstack/react-router";

/**
 * Placeholder. Aqui mora a <b>home do tutor</b> — o feed de pendencias, que e o centro da
 * area do tutor (PRODUTO.md 9.3) e a unica tela com referencia renderizada aprovada.
 * Ela e o bloco 2 do passo 5 do ROADMAP.md, e o backend dela esta inteiro.
 */
export const Route = createFileRoute("/")({
  component: Inicio,
});

function Inicio() {
  return (
    <main className="mx-auto max-w-2xl p-8">
      <h1 className="text-nome-animal text-tinta">Petfy</h1>
      <p className="text-apoio mt-2 text-tinta-secundaria">
        Scaffold do front. A primeira tela e a home do tutor.
      </p>
    </main>
  );
}

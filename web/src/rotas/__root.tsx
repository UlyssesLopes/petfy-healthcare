import { createRootRoute, Link, Outlet } from "@tanstack/react-router";
import { FormattedMessage } from "react-intl";

import { FaixaDeConsentimento } from "../componentes/FaixaDeConsentimento.tsx";

/**
 * A raiz da arvore de rotas.
 *
 * <b>A rota e tipada</b> (ROADMAP.md, bloco 1): endereco escrito errado nao compila, e
 * parametro de rota chega com tipo. E a mesma familia de guarda do
 * `ControllerPathVariableTest` no backend, que existe porque nome de path variable
 * divergindo do parametro quebra em runtime e compila.
 *
 * Os providers — idioma e dados — ficam fora daqui, no `main.tsx`, para valerem tambem
 * para o que a rota nao alcanca.
 */
export const Route = createRootRoute({
  component: Raiz,
  notFoundComponent: NaoEncontrada,
});

function Raiz() {
  return (
    <>
      {/*
       * A faixa de consentimento e da Tela 07, mas nao daquela rota: o texto pode mudar
       * enquanto a pessoa esta em qualquer lugar do produto, e o desenho pede faixa e nao
       * bloqueio. Por isso ela mora na raiz, acima do `Outlet`.
       */}
      <FaixaDeConsentimento />
      <Outlet />
    </>
  );
}

function NaoEncontrada() {
  return (
    <main className="mx-auto max-w-2xl p-8">
      <h1 className="text-nome-animal text-tinta">
        <FormattedMessage id="rota.naoEncontrada.titulo" />
      </h1>
      <p className="mt-4">
        <Link
          to="/"
          className="text-corpo-denso inline-flex min-h-toque items-center text-musgo underline"
        >
          <FormattedMessage id="rota.naoEncontrada.acao" />
        </Link>
      </p>
    </main>
  );
}

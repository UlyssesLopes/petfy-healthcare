import { RouterProvider, createRouter } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { Dados } from "./dados/Dados.tsx";
import { Idioma } from "./i18n/Idioma.tsx";
import { routeTree } from "./arvore-de-rotas.gen.ts";
import "./estilos/global.css";

const router = createRouter({ routeTree });

/**
 * O que torna a rota tipada: sem este registro, `to="/"` seria apenas uma string, e um
 * endereco escrito errado so apareceria no navegador.
 */
declare module "@tanstack/react-router" {
  interface Register {
    router: typeof router;
  }
}

const raiz = document.getElementById("root");
if (!raiz) {
  throw new Error("O elemento #root nao existe no index.html.");
}

createRoot(raiz).render(
  <StrictMode>
    <Idioma>
      <Dados>
        <RouterProvider router={router} />
      </Dados>
    </Idioma>
  </StrictMode>,
);

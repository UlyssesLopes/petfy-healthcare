import { RouterProvider, createRouter } from "@tanstack/react-router";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { Dados } from "./dados/Dados.tsx";
import { recuperarSessao } from "./dados/sessao.ts";
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

/*
 * ------------------------------------------------------ A SESSAO E RECUPERADA ANTES DE MONTAR
 *
 * <b>E o que faz a recarga da pagina deixar de deslogar.</b> O token vive em memoria, por decisao
 * contra XSS, e a memoria comeca vazia a cada carregamento — o `beforeLoad` de toda rota
 * autenticada perguntava "ha sessao?", ouvia "nao" e mandava para o `/entrar`.
 *
 * <b>Tem de ser ANTES do `render`, e nao dentro de um `useEffect`.</b> O roteador decide o destino
 * na primeira passagem: recuperar depois faria a pessoa ver a tela de entrada piscar e so entao ser
 * levada de volta — que e pior do que o problema, porque parece que ela foi deslogada e readmitida.
 *
 * <b>E o `await` nao trava quem nao tem sessao:</b> sem cookie, o servidor responde 401 na hora, e
 * a espera e a de uma requisicao que ja ia acontecer de qualquer jeito.
 */
await recuperarSessao();

createRoot(raiz).render(
  <StrictMode>
    <Idioma>
      <Dados>
        <RouterProvider router={router} />
      </Dados>
    </Idioma>
  </StrictMode>,
);

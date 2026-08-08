import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";

/**
 * A camada de dados vista de fora: um provider, e mais nada exposto.
 *
 * As telas consomem hooks escritos aqui dentro — nunca o `cliente`, nunca os tipos
 * gerados. E a mesma fronteira que o `regra-da-camada.test.ts` cobra.
 */
const clienteDeConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      /*
       * Nao repetir 401 e 403: token invalidado e falta de alcance nao melhoram na
       * segunda tentativa, e insistir so atrasa a tela de login aparecer. 404 tambem
       * fica de fora porque aqui ele NAO significa "some ainda nao existe": o backend
       * responde 404 para animal fora do alcance, de proposito.
       */
      retry: (tentativas, erro) => {
        const status = (erro as { status?: number }).status;
        if (status === 401 || status === 403 || status === 404) {
          return false;
        }
        return tentativas < 2;
      },
    },
  },
});

export function Dados({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={clienteDeConsultas}>{children}</QueryClientProvider>;
}

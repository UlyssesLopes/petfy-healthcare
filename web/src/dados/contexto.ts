import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";

export type MeuContexto = components["schemas"]["ActiveContextResponseDTO"];

/**
 * Em nome de quem estou agindo, e em nome de quem eu poderia.
 *
 * <b>A area nao vem de um campo de papel</b> (PRODUTO.md 9.3): quem responde essa
 * pergunta e o que a pessoa TEM - custodia de um animal, vinculo com uma organizacao -,
 * e e isso que esta rota devolve. Se a tela decidisse por um booleano de cadastro, a 3.1
 * voltaria como condicional de interface.
 */
export function useMeuContexto() {
  return useQuery({
    queryKey: ["meu-contexto"],
    queryFn: async () => {
      const { data, error } = await cliente.GET("/me/context", {});

      if (error !== undefined) {
        throw error;
      }
      if (data === undefined) {
        throw new Error("O contexto respondeu sem corpo.");
      }

      return data;
    },
  });
}

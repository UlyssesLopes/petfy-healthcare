import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Paciente = components["schemas"]["VetPetDTO"];

/**
 * Os animais que o contexto ativo alcanca — a lista da veterinaria (Tela 03).
 *
 * <b>A busca e do SERVIDOR aqui</b>, ao contrario da lista de organizacoes da Tela 09: a rota
 * tem `q`, e uma clinica com 318 pacientes nao caberia numa pagina para filtrar no cliente.
 *
 * <b>O que ela NAO devolve, e e o que a tela mais precisa:</b> o `VetPetDTO` tem nome, tutor,
 * raca, sexo, nascimento, peso e desde quando o acesso existe — e nada sobre saude. Nao ha
 * situacao de vacina, nao ha ultima visita, nao ha tratamento em curso. Ver a tela.
 */
export function usePacientes(busca: string) {
  const alvo = busca.trim();

  return useQuery({
    queryKey: ["pacientes", alvo],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/professional/animals", {
          params: { query: { size: 50, ...(alvo === "" ? {} : { q: alvo }) } },
        }),
      ),
  });
}

import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Animal = components["schemas"]["AnimalResponseDTO"];

/**
 * Os animais que eu alcanco.
 *
 * A listagem e paginada (`Page`, nao array) desde as dividas operacionais de 2026-08-05.
 * A area do tutor responde por um a tres animais (PRODUTO.md 9.2), entao a primeira
 * pagina e a lista inteira na pratica — e quando deixar de ser, quem pagina e a area de
 * organizacao, que le centenas e tem busca e recorte proprios.
 */
export function useAnimais() {
  return useQuery({
    queryKey: ["animais"],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals", {
          /*
           * O springdoc documenta o `Pageable` do Spring como UM parametro chamado
           * `pageable`, mas o Spring le `page`, `size` e `sort` soltos na query. Seguir o
           * contrato ao pe da letra mandaria `?pageable=...`, que o servidor ignora.
           *
           * Entao o objeto vai vazio — o que faz o cliente nao emitir query nenhuma — e
           * valem os defaults do servidor. A divergencia entre o contrato e o que a API
           * aceita esta registrada no ROADMAP.md; ela nao morde aqui porque o tutor
           * responde por um a tres animais, e vai morder na area de organizacao, que le
           * centenas e precisa mesmo paginar.
           */
          params: { query: { pageable: {} } },
        }),
      ).content ?? [],
  });
}

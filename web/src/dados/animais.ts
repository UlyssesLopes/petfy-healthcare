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
        // Sem `page` nem `size`: valem os defaults do servidor, e agora o contrato os
        // declara — 20 itens, ordenados por nome. Ate 2026-08-08 era preciso mandar um
        // `pageable` vazio aqui para driblar o contrato, que descrevia a paginacao de um
        // jeito que o Spring nao le.
        await cliente.GET("/animals"),
      ).content ?? [],
  });
}

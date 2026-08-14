import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type CartaoCompartilhado = components["schemas"]["SharedVaccineCardDTO"];
export type ContatoDoCartaoCompartilhado = components["schemas"]["SharedContactDTO"];
export type CondicaoDoCartao = components["schemas"]["SharedConditionDTO"];

/**
 * O cartao que o link do tutor abre (Tela 04).
 *
 * <b>Query, e nao mutation:</b> abrir o mesmo link duas vezes e a mesma leitura, e quem chega
 * aqui pode ter de reabrir a pagina no meio de uma emergencia.
 *
 * <b>Sem `retry`.</b> Token invalido, revogado e expirado respondem 404 iguais — insistir nao
 * muda a resposta, e cada tentativa vira uma linha no log de leitura do animal.
 */
export function useCartaoCompartilhado(token: string) {
  return useQuery({
    queryKey: ["cartao-compartilhado", token],
    queryFn: async () =>
      corpoDe(await cliente.GET("/share/{token}", { params: { path: { token } } })),
    retry: false,
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Conta = components["schemas"]["AccountOverviewDTO"];
export type ResultadoDaBusca = components["schemas"]["AnimalSearchResultDTO"];
export type AnimalEncontrado = components["schemas"]["AnimalSearchItemDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A busca e a conta (Telas 35 e 36), pelo lado da tela.
 *
 * <b>Quase tudo aqui ja existia e nao tinha tela:</b> trocar senha, exportar os dados e encerrar a
 * conta sao rotas do P1 e do P4. O que o bloco 9 acrescentou foi a busca, a declaracao de registro
 * profissional depois do cadastro, e uma REGRA: ninguem sai do Petfy deixando um animal sem quem
 * responda por ele.
 */

export function useBuscaDeAnimais(termo: string) {
  return useQuery({
    queryKey: ["busca", termo],
    // a trava de tres letras e do servidor; a tela nem chama, para nao pedir e receber 400
    enabled: termo.trim().length >= 3,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/search", { params: { query: { q: termo.trim() } } }),
      ),
  });
}

export function useConta() {
  return useQuery({
    queryKey: ["conta"],
    queryFn: async () => corpoDe(await cliente.GET("/persons/me/account", {})),
  });
}

export function useDeclararCredencial() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: { crmv: string; uf: string; especialidade?: string }) =>
      corpoDe(
        await cliente.POST("/persons/me/professional-credential", {
          body: { crmv: pedido.crmv, uf: pedido.uf, specialty: pedido.especialidade },
        }),
      ),
    /*
     * Invalida TUDO: declarar credencial muda o que a pessoa pode fazer no produto inteiro — as
     * rotas de ato clinico passam a responder, e a busca de encaminhamento passa a encontra-la.
     */
    onSuccess: () => consultas.invalidateQueries(),
  });
}

/**
 * Encerrar a conta.
 *
 * <b>Recusa com 409 enquanto houver animal sob a responsabilidade de quem sai</b>, e a tela sabe
 * disso antes de tentar: o `canDeleteAccount` da conta ja diz.
 */
export function useEncerrarConta() {
  return useMutation({
    mutationFn: async () => {
      await cliente.DELETE("/persons/me", {});
    },
  });
}

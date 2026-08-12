import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type GatoDaColonia = components["schemas"]["ColonyAnimalDTO"];
export type PedidoDeConcordancia = components["schemas"]["GroupApprovalResponseDTO"];
export type TipoDeAcordo = NonNullable<
  components["schemas"]["GroupApprovalRequestDTO"]["kind"]
>;

/** Os quatro filtros do desenho, na ordem em que a tela os mostra. */
export const FILTROS = ["TODOS", "FALTA_CASTRAR", "EM_TRATAMENTO", "SUMIDOS"] as const;

export type Filtro = (typeof FILTROS)[number];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O animal que e de todos (Telas 43 e 44), pelo lado da tela.
 *
 * <b>Tudo aqui depende da organizacao declarada no cabecalho</b>, e nao de um id na URL: quem
 * cuida de duas colonias precisa dizer de qual esta falando, e o produto inteiro ja resolve isso
 * pelo contexto ativo. Uma colonia no path faria a tela discordar do "agindo como" do cabecalho.
 */

/** Os gatos do grupo, com o ultimo avistamento de cada um. */
export function useGatosDaColonia(filtro: Filtro) {
  return useQuery({
    queryKey: ["colonia", filtro],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/group/animals", {
          params: { query: filtro === "TODOS" ? {} : { filtro } },
        }),
      ),
  });
}

/**
 * "Vi o gato."
 *
 * <b>Invalida a lista inteira, e nao so a linha</b>: marcar que viu tira o animal do filtro de
 * sumidos, e a contagem de cada aba muda junto. Atualizar so a celula deixaria a aba "Sumidos ·
 * 1" acesa depois de o gato ter sido visto.
 */
export function useMarcarQueViu() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (animalId: string) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/sightings", {
          params: { path: { animalId } },
          body: {},
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["colonia"] }),
  });
}

/** O que espera a concordancia de uma segunda pessoa. */
export function usePedidosDeConcordancia() {
  return useQuery({
    queryKey: ["concordancias"],
    queryFn: async () => corpoDe(await cliente.GET("/group-approvals", {})),
  });
}

/** Pede que outra pessoa do grupo concorde. */
export function usePedirConcordancia() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: {
      tipo: TipoDeAcordo;
      animalId?: string;
      paraQuemId?: string;
      pessoaAlvoId?: string;
      motivo?: string;
      quandoMorreu?: string;
    }) =>
      corpoDe(
        await cliente.POST("/group-approvals", {
          body: {
            kind: pedido.tipo,
            animalId: pedido.animalId,
            toPersonId: pedido.paraQuemId,
            targetPersonId: pedido.pessoaAlvoId,
            reason: pedido.motivo,
            deceasedOn: pedido.quandoMorreu,
          },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

/**
 * Concorda, e o ato acontece.
 *
 * Invalida tudo porque o efeito e amplo e varia por tipo: a adocao cria um convite, o obito
 * encerra a custodia e as matriculas, a remocao desliga alguem do grupo. Escolher chaves a dedo
 * aqui deixaria alguma tela mostrando o estado de antes.
 */
export function useConcordar() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (groupApprovalId: string) =>
      corpoDe(
        await cliente.POST("/group-approvals/{groupApprovalId}/agree", {
          params: { path: { groupApprovalId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

export function useRecusar() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (groupApprovalId: string) =>
      corpoDe(
        await cliente.POST("/group-approvals/{groupApprovalId}/reject", {
          params: { path: { groupApprovalId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

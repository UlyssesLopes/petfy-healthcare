import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type OfertaDeApadrinhamento = components["schemas"]["SponsorshipOfferDTO"];
export type LinhaDeCusto = components["schemas"]["SponsorshipCostLineDTO"];
export type Apadrinhamento = components["schemas"]["SponsorshipResponseDTO"];
export type EventoDoPadrinho = components["schemas"]["SponsoredEventDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * Quem banca o cuidado (Tela 46), pelo lado da tela.
 *
 * <b>NADA AQUI MOVE DINHEIRO.</b> O produto nao tem meio de pagamento em lugar nenhum, e o desenho
 * promete outra coisa, que ele sabe fazer: "quando o remedio dele for comprado, voce vai ver o
 * evento — com data, valor e quem comprou". Apadrinhar registra um compromisso; o valor corre fora.
 *
 * <b>E o padrinho nao alcanca o animal.</b> O que ele le sao eventos de CUSTO, a partir do
 * apadrinhamento dele — nunca a linha do tempo, nunca o prontuario.
 */

/** A tela de apadrinhar: quem ele e, o que custa por mes, e quantos ja bancam. */
export function useOfertaDeApadrinhamento(animalId: string) {
  return useQuery({
    queryKey: ["apadrinhar", "oferta", animalId],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/sponsorship-offer", {
          params: { path: { animalId } },
        }),
      ),
  });
}

/** Passa a bancar. Registra o compromisso, e nao cobra nada de ninguem. */
export function useApadrinhar(animalId: string) {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: {
      descricao: string;
      valor: number;
      custoId?: string;
    }) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/sponsorships", {
          params: { path: { animalId } },
          body: {
            description: pedido.descricao,
            amount: pedido.valor,
            sourceCostId: pedido.custoId,
          },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["apadrinhar"] }),
  });
}

/** "O que voce banca" — inclusive o que ja acabou. */
export function useMeusApadrinhamentos() {
  return useQuery({
    queryKey: ["apadrinhar", "meus"],
    queryFn: async () => corpoDe(await cliente.GET("/sponsorships/mine", {})),
  });
}

/** "O que voce passa a receber": os eventos de custo do que ele banca. */
export function useEventosDoPadrinho(sponsorshipId: string) {
  return useQuery({
    queryKey: ["apadrinhar", "eventos", sponsorshipId],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/sponsorships/{sponsorshipId}/events", {
          params: { path: { sponsorshipId } },
        }),
      ),
  });
}

/** Parar, com os trinta dias que a tela promete ao abrigo. */
export function useEncerrarApadrinhamento() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (sponsorshipId: string) =>
      corpoDe(
        await cliente.POST("/sponsorships/{sponsorshipId}/end", {
          params: { path: { sponsorshipId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["apadrinhar"] }),
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type PedidoDeUniao = components["schemas"]["AnimalMergeRequestResponseDTO"];
export type LadoDaUniao = NonNullable<PedidoDeUniao["absorbed"]>;

/**
 * O mesmo animal, cadastrado duas vezes (Tela 32).
 *
 * <b>Quem percebe nao e quem decide, e a camada de dados espelha isso:</b> `usePedirUniao` e da
 * clinica que viu a duplicata, `useAceitarUniao` e `useRecusarUniao` sao de quem responde pelo
 * animal. O servidor recusa com 403 quem tentar o segundo sem custodia, e a tela nao tenta
 * adivinhar isso antes — ela mostra o que o contexto permite e deixa o servidor ser a verdade.
 */

/**
 * Outros cadastros com o mesmo microchip.
 *
 * <b>`retry: false`.</b> A resposta vazia e o caso NORMAL — a esmagadora maioria dos cadastros nao
 * tem duplicata —, e insistir numa leitura que quase sempre devolve nada seria pagar tres vezes
 * pelo silencio.
 */
export function useDuplicatas(animalId: string | undefined) {
  return useQuery({
    queryKey: ["duplicatas", animalId],
    enabled: animalId !== undefined,
    retry: false,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/duplicates", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** Os pedidos esperando decisao de quem responde por este animal. */
export function usePedidosDeUniao(animalId: string | undefined) {
  return useQuery({
    queryKey: ["pedidos-de-uniao", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/merge-requests", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/**
 * Pede a uniao a quem responde pelo animal que sobrevive.
 *
 * O `animalId` e o SOBREVIVENTE — e dele o dono que decide, e e na tela dele que o pedido aparece.
 */
export function usePedirUniao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: {
      sobreviventeId: string;
      absorvidoId: string;
      motivo: string;
    }) => {
      const { data, error } = await cliente.POST("/animals/{animalId}/merge-requests", {
        params: { path: { animalId: pedido.sobreviventeId } },
        body: { absorbedAnimalId: pedido.absorvidoId, reason: pedido.motivo },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async (_dados, pedido) => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["duplicatas", pedido.absorvidoId] }),
        consultas.invalidateQueries({ queryKey: ["pedidos-de-uniao", pedido.sobreviventeId] }),
      ]);
    },
  });
}

/**
 * "São animais diferentes" — dito por quem percebeu, e sem pedido nenhum.
 *
 * <b>Não passa por quem responde pelo animal, e não deveria.</b> Pedir a união mexe na vida
 * registrada e por isso precisa dele; dizer "são outros bichos" não mexe em nada — só acende uma
 * marca. Quem tem o animal na frente e o leitor na mão é quem sabe.
 */
export function useMarcarComoDiferentes() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (par: { animalId: string; outroAnimalId: string }) => {
      const { error } = await cliente.POST(
        "/animals/{animalId}/duplicates/{otherAnimalId}/distinct",
        { params: { path: { animalId: par.animalId, otherAnimalId: par.outroAnimalId } } },
      );

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async (_dados, par) => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["duplicatas", par.animalId] }),
        consultas.invalidateQueries({ queryKey: ["animal", par.animalId] }),
      ]);
    },
  });
}

/**
 * Aceita — e depois disso <b>tudo</b> mudou.
 *
 * Limpa o cache inteiro em vez de invalidar chaves escolhidas, e nao por preguica: a uniao move
 * eventos entre dois animais, muda o que a linha do tempo de um deles contem, esvazia a do outro e
 * transforma um cadastro em apontador. Escolher a dedo o que envelheceu seria assinar uma lista que
 * a proxima tela nova deixaria incompleta — e o sintoma seria a tela mostrando o histórico de antes
 * da uniao que a pessoa acabou de aprovar.
 */
export function useAceitarUniao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (animalMergeRequestId: string) => {
      const { data, error } = await cliente.POST("/merge-requests/{animalMergeRequestId}/accept", {
        params: { path: { animalMergeRequestId } },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await consultas.invalidateQueries();
    },
  });
}

/**
 * "Sao animais diferentes."
 *
 * Recusar nao arquiva o pedido: AFIRMA que os dois sao bichos distintos, e o servidor marca o
 * microchip em conflito nos dois cadastros.
 */
export function useRecusarUniao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (animalMergeRequestId: string) => {
      const { data, error } = await cliente.POST("/merge-requests/{animalMergeRequestId}/reject", {
        params: { path: { animalMergeRequestId } },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await consultas.invalidateQueries();
    },
  });
}

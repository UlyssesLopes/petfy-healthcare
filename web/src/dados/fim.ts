import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type FichaFechada = components["schemas"]["ClosedLifeResponseDTO"];
export type CartaoDeEncontrado = components["schemas"]["FoundAnimalCardDTO"];
export type ContatoDeEncontrado = components["schemas"]["FoundContactDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O fim e o reencontro (Telas 33 e 34), pelo lado da tela.
 *
 * <b>As duas telas mais distantes do produto moram no mesmo arquivo, e nao por comodidade:</b>
 * as duas sao sobre o animal deixar de estar por perto — uma para sempre, a outra por algumas
 * horas. Nenhuma das duas e uma tela de rotina, e as duas dependem do mesmo cuidado: nao
 * inventar informacao que o produto nao tem.
 */

/**
 * Encerra a linha do tempo de um animal que morreu.
 *
 * <b>Nao ha invalidacao seletiva aqui, e e de proposito:</b> encerrar mexe na lista de animais,
 * nas pendencias, na matricula e na propria ficha. Escolher chaves a dedo deixaria a lista de
 * animais mostrando o bicho por mais um instante — na tela em que isso e mais cruel.
 */
export function useEncerrarLinhaDoTempo() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (fim: {
      animalId: string;
      quando: string;
      onde?: string;
      despedida?: string;
    }) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/death", {
          params: { path: { animalId: fim.animalId } },
          body: {
            deceasedOn: fim.quando,
            place: fim.onde,
            farewellNote: fim.despedida,
          },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

/** A ficha fechada: os numeros do cartao de depois. 404 no animal que nao foi encerrado. */
export function useFichaFechada(animalId: string | undefined, habilitada = true) {
  return useQuery({
    queryKey: ["ficha-fechada", animalId],
    enabled: animalId !== undefined && habilitada,
    /* Nao insiste: 404 aqui e a resposta normal para animal vivo, e nao uma falha de rede. */
    retry: false,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/death", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** "Quem ja esteve com voce" — a lista que recebe o animal que saiu da outra. */
export function useQueJaEstiveramComigo() {
  return useQuery({
    queryKey: ["animais-anteriores"],
    queryFn: async () => corpoDe(await cliente.GET("/animals/former", {})),
  });
}

/**
 * A busca de animal encontrado, sem conta.
 *
 * <b>Mutation, e nao query, e a razao nao e estilo:</b> a rota e POST porque so o POST passa pelo
 * limite por IP, e porque um GET poria o microchip no historico do navegador de quem buscou. Uma
 * query tambem refaria a busca sozinha ao remontar a tela, gastando o limite de quem esta na
 * calcada com o animal no colo.
 */
export function useProcurarPorMicrochip() {
  return useMutation({
    mutationFn: async (microchip: string) =>
      corpoDe(
        await cliente.POST("/found", {
          body: { microchipNumber: microchip },
        }),
      ),
  });
}

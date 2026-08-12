import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Hospedagem = components["schemas"]["BoardingResponseDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O animal fora de casa por uma semana (Tela 47), pelo lado da tela.
 *
 * <b>Nao ha recurso novo: uma estadia E uma custodia.</b> "A creche nao ganha acesso novo: ela ganha
 * responsabilidade, com prazo, e devolve no dia marcado."
 *
 * <b>Os eventos NAO vem daqui.</b> Eles vem da linha do tempo de sempre, com `since` igual ao
 * `startedAt` da estadia — o mesmo endpoint, o mesmo mascaramento por escopo, nenhuma segunda verdade
 * sobre o que aconteceu com o animal.
 */

export function useHospedagemEmCurso(animalId: string) {
  return useQuery({
    queryKey: ["hospedagem", animalId],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/boarding", {
          params: { path: { animalId } },
        }),
      ),
    /*
     * Nao insiste quando responde 404: "este animal nao esta hospedado" e a resposta NORMAL desta
     * consulta, e nao uma falha de rede. Repetir tres vezes atrasaria a tela em todo animal que
     * esta em casa, que sao quase todos.
     */
    retry: false,
  });
}

/** Entrega o animal: a custodia passa, com prazo. */
export function useHospedar(animalId: string) {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: { organizacaoId: string; voltaEm: string }) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/boarding", {
          params: { path: { animalId } },
          body: {
            organizationId: pedido.organizacaoId,
            expectedReturnOn: pedido.voltaEm,
          },
        }),
      ),
    /*
     * Invalida TUDO, e nao so a hospedagem: a custodia mudou de mao. Quem responde pelo animal, quem
     * aparece na rede de cuidado, o que a lista de acessos mostra — tres telas passam a dizer outra
     * coisa, e escolher chaves a dedo aqui deixaria alguma delas mostrando o estado de antes.
     */
    onSuccess: () => consultas.invalidateQueries(),
  });
}

/**
 * "Isto e o que aconteceu com ele desde que saiu de casa."
 *
 * <b>O recorte e do SERVIDOR</b>, e nao um filtro sobre o que ja foi carregado: a pagina tem vinte
 * itens e a estadia pode ter mais, e um filtro de cliente responderia errado com toda a confianca do
 * mundo — a mesma licao do recorte da Tela 30.
 */
export function useLinhaDoTempoDaEstadia(animalId: string, desde: string | undefined) {
  return useQuery({
    queryKey: ["hospedagem", "linha", animalId, desde],
    enabled: desde !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/timeline", {
          params: { path: { animalId }, query: { size: 200, since: desde } },
        }),
      ).content ?? [],
  });
}

/** Registra a volta: a custodia retorna a quem entregou. */
export function useDevolverDaHospedagem(animalId: string) {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async () =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/boarding/end", {
          params: { path: { animalId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";

export type Pendencia = components["schemas"]["DueItemResponseDTO"];
export type TipoDePendencia = NonNullable<Pendencia["kind"]>;

/** Uma chave, e uma so: silenciar e cumprir mexem no mesmo feed. */
const CHAVE = ["pendencias"] as const;

/**
 * O feed de pendencias.
 *
 * <b>A ordem vem do servidor, e a tela nao reordena.</b> O backend devolve do mais
 * atrasado ao menos urgente, com o que nao tem data primeiro - que e o consentimento,
 * e ele bloqueia o resto do produto. Reordenar aqui seria o cliente refazendo uma regra
 * de dominio, que e exatamente o que a 9.5 do PRODUTO.md proibe para este feed.
 *
 * A janela default e a do backend: 30 dias. Nao repetimos o numero aqui - duas fontes
 * para a mesma janela divergem no dia em que uma das duas mudar.
 */
export function usePendencias(incluirSilenciadas: boolean) {
  return useQuery({
    queryKey: [...CHAVE, { incluirSilenciadas }],
    queryFn: async () => {
      const { data, error } = await cliente.GET("/due-items", {
        params: { query: { includeSilenced: incluirSilenciadas } },
      });

      if (error !== undefined) {
        throw error;
      }

      return data ?? [];
    },
  });
}

interface Alvo {
  kind: TipoDePendencia;
  sourceId: string;
}

/**
 * Silenciar mora na pendencia, e nao em preferencias (DESIGN.md 5.3): silencio e
 * funcionalidade, e funcionalidade escondida em configuracao nao e oferecida.
 *
 * O consentimento nao pode ser silenciado - o backend responde com o codigo 142 -, e a
 * tela nem oferece a acao. Quem decide isso e `podeSilenciar`, abaixo, para a regra
 * ficar num lugar so.
 */
export function useSilenciar() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({ kind, sourceId }: Alvo) => {
      const { error } = await cliente.PUT("/due-items/{kind}/{sourceId}/silence", {
        params: { path: { kind, sourceId } },
      });

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: () => consultas.invalidateQueries({ queryKey: CHAVE }),
  });
}

export function useDeixarDeSilenciar() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({ kind, sourceId }: Alvo) => {
      const { error } = await cliente.DELETE("/due-items/{kind}/{sourceId}/silence", {
        params: { path: { kind, sourceId } },
      });

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: () => consultas.invalidateQueries({ queryKey: CHAVE }),
  });
}

/**
 * Confirmar que a orientacao foi cumprida.
 *
 * <b>Corpo vazio de proposito:</b> `fulfilledAt` e `note` sao opcionais, e o gesto que a
 * 9.4 do PRODUTO.md descreve e "dei o remedio", com um toque e agora. Quem quiser
 * registrar outro instante ou uma observacao faz pela tela da orientacao, que nao existe
 * ainda.
 *
 * <b>Cumprir nao apaga a pendencia, move</b> (DESIGN.md 5.3): vira evento na linha do
 * tempo. Por isso o feed e invalidado em vez de ter o item removido na mao - quem diz o
 * que sobrou e o servidor.
 */
export function useCumprirOrientacao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({
      animalId,
      careInstructionId,
    }: {
      animalId: string;
      careInstructionId: string;
    }) => {
      const { error } = await cliente.POST(
        "/animals/{animalId}/care-instructions/{careInstructionId}/fulfillments",
        { params: { path: { animalId, careInstructionId } }, body: {} },
      );

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: () => consultas.invalidateQueries({ queryKey: CHAVE }),
  });
}

/**
 * O consentimento bloqueia o resto do produto, entao silencia-lo esconderia o bloqueio.
 * A regra vive aqui, e nao na tela, porque o backend a aplica do outro lado - e duas
 * copias da mesma regra divergem.
 */
export function podeSilenciar(pendencia: Pendencia): boolean {
  return pendencia.kind !== "CONSENTIMENTO_PENDENTE";
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Registro = components["schemas"]["HealthRecordResponseDTO"];
export type CorrecaoDeRegistro = components["schemas"]["HealthRecordCorrectionResponseDTO"];
export type Observacao = components["schemas"]["ObservationResponseDTO"];

/** Um atendimento, pelo id que a linha do tempo carrega em `eventId`. */
export function useRegistro(healthRecordId: string | undefined) {
  return useQuery({
    queryKey: ["registro", healthRecordId],
    enabled: healthRecordId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/health-records/{healthRecordId}", {
          params: { path: { healthRecordId: healthRecordId! } },
        }),
      ),
  });
}

/**
 * As correcoes de um atendimento — cada uma guarda o que estava escrito ANTES.
 *
 * E o que sustenta a frase da Tela 23: "a correcao aparece ao lado do original... o registro
 * nao desaparece, ele passa a ter duas versoes, e as duas ficam visiveis". Nao e promessa de
 * tela: o `HealthRecordCorrectionResponseDTO` guarda `previousDescription`,
 * `previousEventDate` e `previousEventType`.
 */
export function useCorrecoes(healthRecordId: string | undefined) {
  return useQuery({
    queryKey: ["correcoes", healthRecordId],
    enabled: healthRecordId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/health-records/{healthRecordId}/corrections", {
          params: { path: { healthRecordId: healthRecordId! } },
        }),
      ),
  });
}

/**
 * Registrar uma observacao — e e por aqui que a discordancia da Tela 23 entra.
 *
 * <b>Nao existe apagar, e nao existe editar o registro de outra pessoa.</b> "Quem escreveu e
 * quem corrige — e o que faz o registro valer alguma coisa". O que o tutor pode e escrever ao
 * lado, assinado por ele, e isso tambem fica para sempre.
 *
 * O `quando` e a data do registro contestado, e nao hoje: a observacao precisa cair NO DIA
 * daquele atendimento para a linha do tempo por as duas coisas lado a lado. O servico ja
 * previa esse uso — "quem registra as 18h o que viu as 9h preenche, e a linha do tempo o
 * coloca as 9h".
 */
export function useRegistrarObservacao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({
      animalId,
      texto,
      quando,
    }: {
      animalId: string;
      texto: string;
      quando?: string;
    }) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/observations", {
          params: { path: { animalId } },
          body: {
            description: texto,
            ...(quando === undefined ? {} : { observedAt: quando }),
          },
        }),
      ),
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["linha-do-tempo"] }),
        consultas.invalidateQueries({ queryKey: ["observacoes"] }),
      ]);
    },
  });
}

/** O limite vem do contrato (`maxLength: 1000`), e a tela conta com ele na cara. */
export const LIMITE_DA_OBSERVACAO = 1000;

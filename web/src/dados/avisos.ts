import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Aviso = components["schemas"]["PersonNotificationResponseDTO"];

/**
 * O aviso que fica dentro do produto.
 *
 * <b>O texto vem PRONTO do servidor</b>, e a tela nao o remonta a partir de tipo de evento e ids.
 * Isso e decisao do modelo, e nao economia: "Ana passou a cuidar do Code" continua sendo o que a
 * pessoa leu no dia em que Ana apagar a conta. Aviso e um FATO — vale depois de o fato deixar de
 * valer —, e uma tela que reconstruisse a frase a partir do estado de agora faria o passado mudar.
 *
 * <b>E e o mesmo texto que foi por e-mail</b>, montado da mesma `Notification` no servidor: duas
 * redacoes do mesmo evento divergiriam no primeiro ajuste de frase, e quem recebe os dois leria
 * coisas diferentes sobre o mesmo fato.
 */
export function useAvisos() {
  return useQuery({
    queryKey: ["avisos"],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/persons/me/notifications", {
          params: { query: { page: 0, size: 50 } },
        }),
      ).content ?? [],
  });
}

/**
 * Quantos ainda nao foram lidos — a marca no sino.
 *
 * <b>Consulta propria, e nao um `length` do feed.</b> O sino vive na moldura, entao ele e
 * perguntado de TODA tela; carregar cinquenta avisos em cada uma para mostrar um numero seria
 * pagar a leitura inteira pelo enfeite. O servidor tem rota so para isto.
 */
export function useAvisosNaoLidos() {
  return useQuery({
    queryKey: ["avisos", "nao-lidos"],
    queryFn: async () => corpoDe(await cliente.GET("/persons/me/notifications/unread-count")) ?? 0,
    /* A moldura fica montada a sessao inteira; sem isto a marca so mudaria ao recarregar. */
    refetchInterval: 60_000,
  });
}

export function useMarcarAvisoLido() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (personNotificationId: string) =>
      corpoDe(
        await cliente.POST("/persons/me/notifications/{personNotificationId}/read", {
          params: { path: { personNotificationId } },
        }),
      ),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["avisos"] });
    },
  });
}

export function useMarcarTodosLidos() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async () => corpoDe(await cliente.POST("/persons/me/notifications/read", {})),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["avisos"] });
    },
  });
}

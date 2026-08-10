import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type AcessoDeOrganizacao = components["schemas"]["OrganizationAccessResponseDTO"];
export type Leitura = components["schemas"]["SensitiveAccessLogResponseDTO"];
export type Escopo = NonNullable<AcessoDeOrganizacao["scopes"]>[number];

/** As organizacoes que alcancam o animal — vigentes, vencidas e revogadas. */
export function useAcessosDeOrganizacao(animalId: string | undefined) {
  return useQuery({
    queryKey: ["acessos-de-organizacao", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/organization-access", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/**
 * O registro de quem LEU o que, e nao de quem tem permissao.
 *
 * <b>E a diferenca entre conceder e confiar.</b> A Tela 22 existe porque conceder acesso
 * e um ato de fe enquanto ninguem ve o que acontece depois; o log e o que transforma
 * "a clinica pode ver" em "a Ana abriu o historico as 9h12".
 *
 * <b>O que ele NAO registra: tentativa negada.</b> O desenho mostra uma linha vermelha —
 * "Douglas Prado tentou abrir o historico clinico, sem acesso" — e o modelo so grava
 * leitura que aconteceu. Nao da para inventar: a ausencia da linha nao e um vazio de tela,
 * e um evento que o servidor nunca guardou.
 */
export function useLeituras(animalId: string | undefined) {
  return useQuery({
    queryKey: ["leituras", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/access-log", {
          params: { path: { animalId: animalId! }, query: { size: 100 } },
        }),
      ),
  });
}

/**
 * Revogar fecha a porta a partir de agora — e so isso.
 *
 * O que a organizacao registrou continua na linha do tempo, assinado por quem registrou.
 * Revogar acesso nao apaga trabalho feito, e a propria tela diz isso ao lado do botao.
 */
export function useRevogarAcesso() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({ animalId, organizationId }: { animalId: string; organizationId: string }) => {
      const { error } = await cliente.DELETE(
        "/animals/{animalId}/organization-access/{organizationId}",
        { params: { path: { animalId, organizationId } } },
      );

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["acessos-de-organizacao"] }),
        consultas.invalidateQueries({ queryKey: ["rede-de-cuidado"] }),
      ]);
    },
  });
}

/**
 * O escopo em frases, e nunca a palavra "escopo" (secao 09 da identidade e voz).
 *
 * A Tela 09 e explicita: "o problema de design mais dificil da area do tutor e fazer
 * alguem que nunca ouviu a palavra escopo escolher um. A saida e nao nomear a abstracao —
 * nomear o que a organizacao vai ver". Entao o mapa vive aqui, perto do dado, e nao
 * espalhado por tela.
 */
export const FRASE_DO_ESCOPO: Record<string, string> = {
  CARTEIRA: "acesso.escopo.CARTEIRA",
  CONDICOES: "acesso.escopo.CONDICOES",
  PRONTUARIO: "acesso.escopo.PRONTUARIO",
  OBSERVACOES: "acesso.escopo.OBSERVACOES",
  PESO: "acesso.escopo.PESO",
  ANEXOS: "acesso.escopo.ANEXOS",
  CONTATO: "acesso.escopo.CONTATO",
};

/** O que foi lido, dito como a tela diz: "abriu o historico clinico". */
export const FRASE_DA_LEITURA: Record<string, string> = {
  VACCINES: "acesso.leitura.VACCINES",
  HEALTH_RECORDS: "acesso.leitura.HEALTH_RECORDS",
  VACCINE_CORRECTIONS: "acesso.leitura.VACCINE_CORRECTIONS",
  HEALTH_RECORD_CORRECTIONS: "acesso.leitura.HEALTH_RECORD_CORRECTIONS",
  ATTACHMENTS: "acesso.leitura.ATTACHMENTS",
  SHARED_CARD: "acesso.leitura.SHARED_CARD",
};

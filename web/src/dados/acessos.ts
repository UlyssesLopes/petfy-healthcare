import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type AcessoDeOrganizacao = components["schemas"]["OrganizationAccessResponseDTO"];
export type Leitura = components["schemas"]["SensitiveAccessLogResponseDTO"];
export type Escopo = NonNullable<AcessoDeOrganizacao["scopes"]>[number];
export type Organizacao = components["schemas"]["OrganizationResponseDTO"];

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
 * As organizacoes que o tutor pode escolher para conceder acesso.
 *
 * <b>A rota devolve TODAS as organizacoes cadastradas</b> — o `listAllOrganizations` e um
 * `findAll` paginado, e o SecurityConfig so restringe `/professional/**` e os convites.
 * Isso e o certo para o caso de uso: o tutor precisa achar a clinica onde ele acabou de
 * chegar, e ela nao tem relacao nenhuma com ele antes desta tela.
 *
 * <b>A busca e no cliente, e isso e divida.</b> Nao ha parametro de nome na rota; pedimos
 * uma pagina grande e filtramos aqui. Funciona enquanto o cadastro de organizacoes for
 * pequeno, e para de funcionar sem avisar — o gatilho para criar busca no servidor e o
 * cadastro passar de algumas centenas.
 */
export function useOrganizacoes() {
  return useQuery({
    queryKey: ["organizacoes"],
    queryFn: async () =>
      corpoDe(await cliente.GET("/organizations", { params: { query: { size: 200 } } })).content ??
      [],
  });
}

/**
 * O campo do desenho e uma data; o contrato pede `date-time`.
 *
 * <b>Fim do dia, e nao meia-noite.</b> "Ate 31/12/2027" para quem le significa o dia 31
 * inteiro — converter para `2027-12-31T00:00:00` encurtaria o acesso em um dia sem avisar
 * ninguem, e o tutor descobriria pela clinica batendo na porta fechada.
 *
 * Sem fuso de proposito: o backend guarda `LocalDateTime` e comparar com fuso e a divida 3,
 * que tem PR proprio. Mandar `Z` aqui seria escolher UTC no lugar dela.
 */
export function fimDoDia(data: string): string {
  return `${data}T23:59:59`;
}

/**
 * Conceder acesso, e tambem AJUSTAR: e o mesmo POST.
 *
 * O servidor reativa a concessao vigente em vez de acumular linhas, entao conceder de novo
 * a quem ja tem e ajustar o que ela ve sao o mesmo gesto para a API. Foi este caminho que
 * respondia 500 antes do `@Transactional` no `grant` — o quinto caso de DTO montado fora de
 * transacao no projeto.
 */
export function useConcederAcesso() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({
      animalId,
      organizationId,
      escopos,
      ate,
    }: {
      animalId: string;
      organizationId: string;
      escopos: Escopo[];
      ate: string | undefined;
    }) => {
      const { data, error } = await cliente.POST("/animals/{animalId}/organization-access", {
        params: { path: { animalId } },
        body: {
          organizationId,
          scopes: escopos,
          expiresAt: ate === undefined ? undefined : fimDoDia(ate),
        },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
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

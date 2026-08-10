import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Organizacao = components["schemas"]["OrganizationResponseDTO"];
export type ConviteDeOrganizacao = components["schemas"]["OrganizationInviteResponseDTO"];

/**
 * Criar uma organizacao (Tela 15).
 *
 * <b>Criar nao da acesso a animal nenhum</b>, e a tela diz isso antes do botao: "cada tutor
 * concede o que quiser, animal por animal, e pode revogar quando quiser. A Creche Quintal
 * comeca vazia". Nao e ressalva juridica — e a arquitetura: custodia e acesso sao coisas
 * separadas (ROADMAP.md, P2).
 *
 * <b>AS CAPACIDADES NAO ENTRAM AQUI.</b> O desenho pede "o que voces fazem" com cinco opcoes
 * marcaveis, e o `OrganizationRequestDTO` nao tem campo para elas — o `OrganizationCapability`
 * existe no modelo e aparece no `/me/context`, mas nada no contrato permite declara-las na
 * criacao. Ver a tela.
 */
export function useCriarOrganizacao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (organizacao: {
      nome: string;
      cnpj?: string;
      telefone?: string;
      endereco?: string;
      cidade?: string;
      estado?: string;
    }) =>
      corpoDe(
        await cliente.POST("/organizations", {
          body: {
            name: organizacao.nome,
            ...(organizacao.cnpj === undefined ? {} : { cnpj: organizacao.cnpj }),
            ...(organizacao.telefone === undefined ? {} : { phone: organizacao.telefone }),
            ...(organizacao.endereco === undefined ? {} : { address: organizacao.endereco }),
            ...(organizacao.cidade === undefined ? {} : { city: organizacao.cidade }),
            ...(organizacao.estado === undefined ? {} : { state: organizacao.estado }),
          },
        }),
      ),
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["organizacoes"] }),
        consultas.invalidateQueries({ queryKey: ["meu-contexto"] }),
      ]);
    },
  });
}

/**
 * Os convites da organizacao — e o unico pedaco da equipe que a API mostra.
 *
 * Nao existe rota que liste MEMBROS: o contrato tem `GET /organizations/invites`,
 * `POST` e `DELETE /organizations/invites/{id}`, e nada que devolva quem ja entrou. Ver a
 * Tela 16.
 */
export function useConvitesDaOrganizacao() {
  return useQuery({
    queryKey: ["convites-de-organizacao"],
    queryFn: async () => corpoDe(await cliente.GET("/organizations/invites", {})),
  });
}

/**
 * Convidar alguem para a equipe.
 *
 * <b>Sem funcao.</b> O `OrganizationInviteRequestDTO` tem e-mail e prazo, e mais nada — a
 * funcao (`VETERINARIO`, `MONITOR`, `VOLUNTARIO`, `ADMINISTRADOR`) existe no
 * `ContextOptionDTO` mas nao ha por onde escolhe-la ao convidar. Quem entra, entra sem papel
 * declarado.
 */
export function useConvidarParaEquipe() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (email: string) => {
      const { data, error } = await cliente.POST("/organizations/invites", {
        body: { email },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["convites-de-organizacao"] });
    },
  });
}

/** Revogar convite que ainda nao foi aceito. */
export function useRevogarConvite() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (organizationInviteId: string) => {
      const { error } = await cliente.DELETE("/organizations/invites/{organizationInviteId}", {
        params: { path: { organizationInviteId } },
      });

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["convites-de-organizacao"] });
    },
  });
}

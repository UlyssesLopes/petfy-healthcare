import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Organizacao = components["schemas"]["OrganizationResponseDTO"];
export type ConviteDeOrganizacao = components["schemas"]["OrganizationInviteResponseDTO"];
export type MembroDaEquipe = components["schemas"]["MembershipResponseDTO"];

/**
 * As quatro funcoes, vindas do contrato e nao escritas a mao aqui: funcao nova no backend tem
 * de quebrar o `tsc` deste arquivo, e nao aparecer em branco na tela.
 */
export type FuncaoNaEquipe = NonNullable<MembroDaEquipe["role"]>;

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
 * A equipe: quem ja entrou, com funcao, desde quando e o registro profissional de quem tem.
 *
 * Enquanto esta rota nao existia, a Tela 16 mostrava so convites e DIZIA que a tabela nao
 * tinha de onde sair — porque uma tabela de equipe vazia teria sido mentira: os membros
 * existiam, e o produto e que nao sabia mostra-los.
 */
export function useEquipe() {
  return useQuery({
    queryKey: ["equipe"],
    queryFn: async () => corpoDe(await cliente.GET("/organizations/members", {})),
  });
}

/** O "ajustar" do desenho. So administrador — o servidor recusa o resto com 151. */
export function useAjustarFuncao() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (ajuste: { membershipId: string; funcao: FuncaoNaEquipe }) => {
      const { data, error } = await cliente.PATCH("/organizations/members/{membershipId}", {
        params: { path: { membershipId: ajuste.membershipId } },
        body: { role: ajuste.funcao },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["equipe"] });
    },
  });
}

/**
 * O desligamento. O servidor marca a saida e nao apaga o vinculo — o que a pessoa registrou
 * continua no historico dos animais.
 */
export function useDesligarDaEquipe() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (membershipId: string) => {
      const { error } = await cliente.DELETE("/organizations/members/{membershipId}", {
        params: { path: { membershipId } },
      });

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["equipe"] });
    },
  });
}

/** Os convites em aberto. A outra metade da equipe: quem foi chamado e ainda nao entrou. */
export function useConvitesDaOrganizacao() {
  return useQuery({
    queryKey: ["convites-de-organizacao"],
    queryFn: async () => corpoDe(await cliente.GET("/organizations/invites", {})),
  });
}

/**
 * Convidar alguem para a equipe, <b>com a funcao que ela tera</b>.
 *
 * A funcao viaja no CONVITE, e nao no cadastro de quem aceita: deixar quem se cadastra
 * escolher a propria funcao seria deixa-lo escolher a propria permissao. Quem convida ja e da
 * organizacao, e e dele a decisao.
 */
export function useConvidarParaEquipe() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (convite: { email: string; funcao: FuncaoNaEquipe }) => {
      const { data, error } = await cliente.POST("/organizations/invites", {
        body: { email: convite.email, role: convite.funcao },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      /* O convite aceito vira membro, entao a equipe tambem envelhece quando um convite muda. */
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["convites-de-organizacao"] }),
        consultas.invalidateQueries({ queryKey: ["equipe"] }),
      ]);
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

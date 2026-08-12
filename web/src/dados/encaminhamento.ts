import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Encaminhamento = components["schemas"]["ReferralResponseDTO"];
export type OpcoesDoEncaminhamento = components["schemas"]["ReferralOptionsDTO"];
export type CaixaDoQueVaiJunto = components["schemas"]["ReferralScopeOptionDTO"];
export type CandidatoAEncaminhamento = components["schemas"]["ReferralCandidateDTO"];
export type Escopo = NonNullable<CaixaDoQueVaiJunto["scope"]>;

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O caso que passa adiante (Tela 45), pelo lado da tela.
 *
 * <b>"Encaminhar e voce indicando o caminho; conceder acesso continua sendo dele."</b> Nada aqui
 * concede acesso a ninguem: o `useEncaminhar` cria um PEDIDO, e o que o `useAutorizar` produz — do
 * outro lado, na mao de quem responde pelo animal — e uma concessao comum com prazo.
 *
 * <b>Duas telas, tres papeis.</b> Quem encaminha usa `useOpcoes`, `useCandidatos` e `useEncaminhar`;
 * quem responde pelo animal usa `usePendentesParaDecisao`, `useAutorizar` e `useRecusarEncaminhamento`;
 * quem recebe usa `useEncaminhamentosRecebidos`. As tres listas vem do mesmo DTO de proposito, porque
 * e o mesmo fato visto de tres lugares.
 */

/**
 * O que pode ir junto, com o numero de cada caixa.
 *
 * <b>Uma chamada, e nao uma por escopo</b>: as contagens saem de uma consulta agregada na linha do
 * tempo, com o mesmo mapeamento de tipo para escopo que a guarda aplica ao mascarar. O numero que
 * esta tela promete e o que o especialista abre depois.
 */
export function useOpcoesDeEncaminhamento(animalId: string) {
  return useQuery({
    queryKey: ["encaminhamento", "opcoes", animalId],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/referral-options", {
          params: { path: { animalId } },
        }),
      ),
  });
}

/**
 * "Buscar outro profissional."
 *
 * <b>Nao busca com menos de tres letras, e a trava e do servidor.</b> A tela nem chama: uma letra
 * devolveria meio cadastro de profissionais, e e isso que separa "buscar" de "listar todo mundo".
 */
export function useCandidatosAEncaminhamento(animalId: string, busca: string) {
  const termo = busca.trim();

  return useQuery({
    queryKey: ["encaminhamento", "candidatos", animalId, termo],
    enabled: termo.length >= 3,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/referral-candidates", {
          params: { path: { animalId }, query: { busca: termo } },
        }),
      ),
  });
}

/** Encaminha o caso. Nao concede nada: cria o pedido que o tutor decide. */
export function useEncaminhar(animalId: string) {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: {
      paraQuemId: string;
      motivo: string;
      escopos: Escopo[];
      dias?: number;
    }) =>
      corpoDe(
        await cliente.POST("/animals/{animalId}/referrals", {
          params: { path: { animalId } },
          body: {
            toPersonId: pedido.paraQuemId,
            reason: pedido.motivo,
            scopes: pedido.escopos,
            accessDays: pedido.dias,
          },
        }),
      ),
    /*
     * Invalida tudo, e nao so a lista deste animal. Quando quem encaminha JA responde pelo animal, o
     * pedido nasce autorizado e a concessao sai na hora — a tela de acessos do animal fica errada
     * sem isto, e escolher chaves a dedo aqui exigiria a tela saber de que caso ela e.
     */
    onSuccess: () => consultas.invalidateQueries(),
  });
}

/** O que esta clinica encaminhou deste animal, e no que deu. */
export function useEncaminhamentosDoAnimal(animalId: string) {
  return useQuery({
    queryKey: ["encaminhamento", "doAnimal", animalId],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/referrals", {
          params: { path: { animalId } },
        }),
      ),
  });
}

/** O que espera a decisao de quem responde pelos animais de quem esta lendo. */
export function usePendentesParaDecisao() {
  return useQuery({
    queryKey: ["encaminhamento", "pendentes"],
    queryFn: async () => corpoDe(await cliente.GET("/referrals/pending", {})),
  });
}

/**
 * O que encaminharam para quem esta lendo.
 *
 * O pendente vem SEM o animal — nome nulo, id nulo. O tutor ainda nao autorizou nada, e o nome ja e
 * informacao sobre um animal que nao e desta pessoa.
 */
export function useEncaminhamentosRecebidos() {
  return useQuery({
    queryKey: ["encaminhamento", "recebidos"],
    queryFn: async () => corpoDe(await cliente.GET("/referrals/received", {})),
  });
}

/** Autoriza, e a concessao nasce na mesma transacao. */
export function useAutorizarEncaminhamento() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (referralId: string) =>
      corpoDe(
        await cliente.POST("/referrals/{referralId}/authorize", {
          params: { path: { referralId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

export function useRecusarEncaminhamento() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (referralId: string) =>
      corpoDe(
        await cliente.POST("/referrals/{referralId}/reject", {
          params: { path: { referralId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

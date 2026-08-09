import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type EntradaDaLinha = components["schemas"]["TimelineEntryResponseDTO"];
export type Tutor = components["schemas"]["PetTutorResponseDTO"];
export type AcessoDeOrganizacao = components["schemas"]["OrganizationAccessResponseDTO"];

/**
 * A vida do animal em ordem, atravessando custodias e organizacoes.
 *
 * <b>Uma leitura, e nao seis.</b> A linha do tempo e uma view no banco, ordenada por
 * QUANDO ACONTECEU — nao por quando foi digitado. Foi ela que encerrou a ideia de o
 * cliente chamar seis endpoints e ordenar em memoria, e por isso a tela nao reordena
 * nada aqui: a cronologia e regra de dominio (ROADMAP.md, P4).
 */
export function useLinhaDoTempo(animalId: string | undefined) {
  return useQuery({
    queryKey: ["linha-do-tempo", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/timeline", {
          /*
           * 200 em vez dos 20 do servidor. A Tela 02 nao pagina a vida do animal — ela
           * filtra por tipo e mostra a faixa de anos no cabecalho, e as duas coisas
           * precisam da serie inteira em maos. Um animal de 7 anos com consulta
           * semestral, vacina anual e observacao de creche nao chega perto disso; quando
           * chegar, quem pagina e a area de organizacao, que ja le centenas.
           */
          params: { path: { animalId: animalId! }, query: { size: 200 } },
        }),
      ).content ?? [],
  });
}

/**
 * Quem responde pelo animal e quem o alcanca por concessao.
 *
 * <b>O que aparece aqui e quem tem alcance de fato</b> — custodia e acesso —, nunca uma
 * lista de contatos (DESIGN.md 5.4).
 */
export function useTutores(animalId: string | undefined) {
  return useQuery({
    queryKey: ["tutores", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/tutors", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** As organizacoes que alcancam este animal — clinica, creche, abrigo. */
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
 * As iniciais de quem registrou, que e a marca do evento (DESIGN.md 5.2): elas ocupam o
 * lugar que num produto comum teria um icone de tipo.
 *
 * O avatar e `aria-hidden` na tela — o nome de quem registrou esta no texto, e iniciais
 * anunciadas por leitor de tela sao decoracao (secao 6).
 */
export function iniciaisDe(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);

  if (partes.length === 0) {
    return "";
  }

  const primeira = partes[0]!.charAt(0);
  const ultima = partes.length > 1 ? partes[partes.length - 1]!.charAt(0) : "";

  return (primeira + ultima).toUpperCase();
}

/** Ato clinico tem bloco de acento e linha de credencial; observacao nao (DESIGN.md 5.5). */
export function ehAtoClinico(entrada: EntradaDaLinha): boolean {
  return (
    entrada.eventType === "VACINA"
    || entrada.eventType === "ATENDIMENTO"
    || entrada.eventType === "ANTIPARASITARIO"
  );
}

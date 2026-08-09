import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type MembroDaRede = components["schemas"]["CareNetworkMemberDTO"];
export type Condicao = components["schemas"]["AnimalHealthConditionResponseDTO"];
export type Pesagem = components["schemas"]["AnimalWeightResponseDTO"];
export type Anexo = components["schemas"]["AttachmentResponseDTO"];
type Dose = components["schemas"]["VaccineResponseDTO"];

/** O animal em si — nome, especie, raca, microchip, RGA. */
export function useAnimal(animalId: string | undefined) {
  return useQuery({
    queryKey: ["animal", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/**
 * Quem alcanca o animal, e por que caminho.
 *
 * <b>O `reach` e o que a tela precisa e o que uma lista de contatos nao teria:</b>
 * `CUSTODIA` e quem responde pelo animal, `CONCESSAO` e quem recebeu acesso. O
 * cabecalho da Tela 02 le so o primeiro — "sob custodia de X desde Y".
 */
export function useRedeDeCuidado(animalId: string | undefined) {
  return useQuery({
    queryKey: ["rede-de-cuidado", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/care-network", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** Alergia e condicao cronica, com desde quando e quao grave. */
export function useCondicoes(animalId: string | undefined) {
  return useQuery({
    queryKey: ["condicoes", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/conditions", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** O historico de peso. A carteira desenha as 6 ultimas como barras. */
export function usePesagens(animalId: string | undefined) {
  return useQuery({
    queryKey: ["pesagens", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/weights", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** Antiparasitario, que na Tela 01 e uma das quatro linhas do "de relance". */
export function useAntiparasitarios(animalId: string | undefined) {
  return useQuery({
    queryKey: ["antiparasitarios", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/antiparasitics", { params: { query: { animalId: animalId! } } }),
      ),
  });
}

/** Carteirinha de papel fotografada, exame, laudo. */
export function useAnexos(animalId: string | undefined) {
  return useQuery({
    queryKey: ["anexos", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/attachments", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/** Os quatro estados da secao 05, mais o quinto que so existe cruzando com o catalogo. */
export type EstadoDaDose = "vencida" | "vencendo" | "emDia" | "semProximaDose" | "semRegistro";

export type LinhaDaCarteira = {
  nome: string;
  estado: EstadoDaDose;
  proximaDose: string | undefined;
  diasAteProximaDose: number | undefined;
};

const ESTADO_DA_AGENDA: Record<string, EstadoDaDose> = {
  OVERDUE: "vencida",
  DUE_SOON: "vencendo",
  UP_TO_DATE: "emDia",
  NO_NEXT_DOSE: "semProximaDose",
};

/**
 * A carteira de vacinacao da Tela 02, que e uma juncao de duas leituras.
 *
 * <b>O estado vem do servidor, e isso nao e detalhe.</b> Vencida, vencendo e em dia sao
 * regra de dominio — a mesma que decide o que entra no feed —, e recalcula-los aqui
 * criaria uma segunda verdade sobre o animal. Por isso o `status` sai do
 * `/vaccines/agenda` em vez de sair de uma conta sobre `nextDoseDate`.
 *
 * <b>O "sem registro" e o unico que a API nao sabe dizer, e ele importa.</b> E o quarto
 * marcador da secao 05 — o anel tracejado —, e o documento e explicito: e "o estado mais
 * frequente e o mais mal representado nos produtos do setor". Nao saber != nao existe !=
 * irregular. Ele nasce aqui de `catalogo menos o que ja tem registro`.
 *
 * <b>A juncao e por NOME, e isso e uma divida.</b> O `VaccineAgendaItemDTO` nao carrega
 * `vaccineCatalogId` — so `vaccineName` —, entao nao ha como casar catalogo e agenda por
 * identidade. Nome bate hoje porque a dose nasce com o nome do catalogo, e deixa de bater
 * no dia em que alguem registrar "Antirrabica" a mao. O conserto e no backend: expor o
 * catalogo no item da agenda, ou uma rota de carteira por animal.
 */
export function useCarteira(animalId: string | undefined) {
  const agenda = useQuery({
    queryKey: ["agenda-de-vacinas"],
    queryFn: async () =>
      corpoDe(await cliente.GET("/vaccines/agenda", { params: { query: { windowDays: 30 } } })),
  });

  const doses = useQuery({
    queryKey: ["doses-registradas"],
    queryFn: async () => corpoDe(await cliente.GET("/vaccines", { params: { query: { size: 200 } } })),
  });

  const catalogo = useQuery({
    queryKey: ["catalogo-de-vacinas", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/vaccine-catalog", { params: { query: { animalId: animalId! } } }),
      ),
  });

  const carregando = agenda.isPending || doses.isPending || catalogo.isPending;
  const erro = agenda.error ?? doses.error ?? catalogo.error;

  const linhas = montarCarteira({
    animalId,
    daAgenda: agenda.data?.items ?? [],
    doses: doses.data?.content ?? [],
    catalogo: catalogo.data ?? [],
  });

  return { linhas, carregando, erro };
}

/**
 * A juncao das tres leituras numa carteira, isolada da rede para poder ser testada.
 *
 * <b>A AGENDA NAO E A CARTEIRA, e confundir as duas produz uma mentira especifica.</b>
 * O `items` da agenda traz so o que PEDE ACAO — vencida e vencendo. A dose em dia entra na
 * contagem e nao na lista. Montar a carteira a partir dele faz a dose em dia sumir e
 * reaparecer como "sem registro" pelo catalogo: o produto diria "nunca tomou" sobre uma
 * dose que ele mesmo registrou. E o erro exato que a secao 05 nomeia — nao saber, nao
 * existir e estar irregular sao tres coisas diferentes.
 *
 * Entao a lista sai de `/vaccines` e a agenda entra so como AUTORIDADE DE ESTADO: quem ela
 * aponta recebe o estado dela, e o resto se resolve por exclusao. Assim o limiar de
 * "vencendo" continua morando no servidor, onde ele pode mudar sem o front saber.
 *
 * <b>A juncao e por NOME, e isso e divida.</b> O `VaccineAgendaItemDTO` nao carrega
 * `vaccineCatalogId` — so `vaccineName` —, entao nao ha como casar as tres leituras por
 * identidade. Nome bate hoje porque a dose nasce com o nome do catalogo, e deixa de bater
 * no dia em que alguem registrar "Antirrabica" a mao. O conserto e no backend: uma rota de
 * carteira por animal, que tambem acabaria com as tres leituras — duas delas trazem os
 * dados de TODOS os animais que a pessoa alcanca para o cliente jogar fora o que nao e
 * deste.
 */
export function montarCarteira({
  animalId,
  daAgenda: itensDaAgenda,
  doses: dosesRegistradas,
  catalogo: catalogoDaEspecie,
}: {
  animalId: string | undefined;
  daAgenda: components["schemas"]["VaccineAgendaItemDTO"][];
  doses: Dose[];
  catalogo: components["schemas"]["VaccineCatalogResponseDTO"][];
}): LinhaDaCarteira[] {
  const serieDe = (nome: string | undefined) => (nome ?? "").trim().toLowerCase();

  const daAgenda = new Map(
    itensDaAgenda
      .filter((item) => item.animalId === animalId)
      .map((item) => [serieDe(item.vaccineName), item]),
  );

  /* Uma serie, uma linha: a carteira mostra a dose MAIS RECENTE de cada vacina, e nao o
   * historico. Duas antirrabicas viram uma linha — o historico e a linha do tempo. */
  const maisRecentePorSerie = new Map<string, Dose>();

  for (const dose of dosesRegistradas) {
    if (dose.animalId !== animalId) {
      continue;
    }

    const serie = serieDe(dose.vaccineName);
    const atual = maisRecentePorSerie.get(serie);

    if (atual === undefined || (dose.applicationDate ?? "") > (atual.applicationDate ?? "")) {
      maisRecentePorSerie.set(serie, dose);
    }
  }

  const registradas: LinhaDaCarteira[] = [...maisRecentePorSerie.entries()].map(([serie, dose]) => {
    const apontada = daAgenda.get(serie);

    if (apontada !== undefined) {
      return {
        nome: dose.vaccineName ?? "",
        estado: ESTADO_DA_AGENDA[apontada.status ?? ""] ?? "semProximaDose",
        proximaDose: apontada.nextDoseDate,
        diasAteProximaDose: apontada.daysUntilNextDose,
      };
    }

    return {
      nome: dose.vaccineName ?? "",
      estado: dose.nextDoseDate === undefined ? "semProximaDose" : "emDia",
      proximaDose: dose.nextDoseDate,
      diasAteProximaDose: undefined,
    };
  });

  const jaRegistrada = new Set(registradas.map((linha) => serieDe(linha.nome)));

  const ausentes: LinhaDaCarteira[] = catalogoDaEspecie
    .filter((entrada) => !jaRegistrada.has(serieDe(entrada.name)))
    .map((entrada) => ({
      nome: entrada.name ?? "",
      estado: "semRegistro" as const,
      proximaDose: undefined,
      diasAteProximaDose: undefined,
    }));

  /*
   * A ordem e a da gravidade, e nao a alfabetica: o que venceu primeiro, o que esta
   * vencendo depois, e o "sem registro" antes do "em dia" — porque a ausencia pede acao e
   * o "em dia" nao pede nada. A secao 05 diz "se tudo interrompe, nada interrompe", e a
   * ordem e a forma mais barata de hierarquia.
   */
  const ordem: Record<EstadoDaDose, number> = {
    vencida: 0,
    vencendo: 1,
    semRegistro: 2,
    semProximaDose: 3,
    emDia: 4,
  };

  return [...registradas, ...ausentes].sort(
    (a, b) => ordem[a.estado] - ordem[b.estado] || a.nome.localeCompare(b.nome, "pt-BR"),
  );
}

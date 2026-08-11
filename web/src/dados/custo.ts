import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Custo = components["schemas"]["AnimalCostResponseDTO"];
export type TipoDeCusto = NonNullable<components["schemas"]["AnimalCostRequestDTO"]["kind"]>;

/**
 * Por onde o valor entra (Telas 40, 41 e 42), pelo lado da tela.
 *
 * <b>A ASSIMETRIA E A REGRA INTEIRA, e ela e do servidor:</b> escreve quem registra, le so quem
 * responde pelo animal. A clinica lanca o valor do atendimento que ela mesma fez e nao consegue
 * ler nem esse — "o que a Clinica Vet Norte cobra do Marcelo nao e assunto da creche, do petshop
 * nem de outra clinica". A tela nao reimplementa isso: ela pede, e o servidor responde 403.
 *
 * <b>Nao ha, e nao havera, consulta que agregue preco por organizacao.</b> Com preco de milhares
 * de atendimentos seria facil mostrar ao tutor que a mesma consulta custa menos duas ruas adiante,
 * e "registro clinico que vira comparador de preco deixa de ser lugar seguro para a clinica
 * registrar a verdade". A ausencia dessa rota e decisao, e nao lacuna.
 */

/** O que se gastou com o animal. Exige custodia: e a unica leitura do produto que pede isso. */
export function useCustos(animalId: string | undefined) {
  return useQuery({
    queryKey: ["custos", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/costs", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/**
 * Lanca um valor.
 *
 * <b>O campo e opcional em toda tela que o oferece</b>, e um evento sem valor e normal — nunca um
 * erro, nunca um alerta. Quem chama daqui ja decidiu que ha valor a lancar.
 */
export function useLancarCusto() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (lancamento: {
      animalId: string;
      descricao: string;
      valor: number;
      tipo?: TipoDeCusto;
      pago?: boolean;
      mensal?: boolean;
      atendimentoId?: string;
      matriculaId?: string;
    }) => {
      const { data, error } = await cliente.POST("/animals/{animalId}/costs", {
        params: { path: { animalId: lancamento.animalId } },
        body: {
          description: lancamento.descricao,
          amount: lancamento.valor,
          ...(lancamento.tipo === undefined ? {} : { kind: lancamento.tipo }),
          /* `pago` so viaja quando alguem marcou: nulo e "ninguem disse", e mandar `false`
             afirmaria "nao foi pago" sobre algo que ninguem afirmou. */
          ...(lancamento.pago === true ? { paid: true } : {}),
          ...(lancamento.mensal === true ? { recurrence: "MENSAL" as const } : {}),
          ...(lancamento.atendimentoId === undefined
            ? {}
            : { sourceHealthRecordId: lancamento.atendimentoId }),
          ...(lancamento.matriculaId === undefined
            ? {}
            : { sourceEnrollmentId: lancamento.matriculaId }),
        },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async (_dados, lancamento) => {
      await consultas.invalidateQueries({ queryKey: ["custos", lancamento.animalId] });
    },
  });
}

/**
 * "Combinado com o tutor" — a caixa da Tela 41, gravada pela creche.
 *
 * <b>SUBSTITUI O COMBINADO INTEIRO.</b> O que a tela nao mandar passa a ser nulo, e isso e do
 * contrato: combinar e um ato unico, e quem renegocia diz de novo o que passou a valer. Por isso
 * o formulario manda sempre os quatro campos, e nao so o que mudou.
 */
export function useGravarCombinado() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (combinado: {
      enrollmentId: string;
      mensalidade?: number;
      diaDoVencimento?: number;
      diaria?: number;
      dias: string[];
    }) => {
      const { data, error } = await cliente.PUT(
        "/professional/creche/enrollments/{enrollmentId}/agreement",
        {
          params: { path: { enrollmentId: combinado.enrollmentId } },
          body: {
            ...(combinado.mensalidade === undefined ? {} : { monthlyFee: combinado.mensalidade }),
            ...(combinado.diaDoVencimento === undefined
              ? {}
              : { dueDay: combinado.diaDoVencimento }),
            ...(combinado.diaria === undefined ? {} : { dailyRate: combinado.diaria }),
            weekdays: combinado.dias,
          },
        },
      );

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["matriculas-da-turma"] }),
        consultas.invalidateQueries({ queryKey: ["matriculas-do-animal"] }),
      ]);
    },
  });
}

/**
 * Os dias da semana, na ordem em que a semana acontece.
 *
 * <b>O nome do {@code DayOfWeek} e o que viaja</b>, e nao o indice: 1 e segunda na ISO e domingo
 * em metade das bibliotecas de tela, e essa e a classe de erro que aparece uma vez por ano, no dia
 * errado, para um animal so.
 */
export const DIAS_DA_SEMANA = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
] as const;

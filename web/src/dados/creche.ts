import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Turma = components["schemas"]["ClassGroupResponseDTO"];
export type Matricula = components["schemas"]["EnrollmentResponseDTO"];
export type LinhaDaComprovacao = components["schemas"]["HealthProofItemDTO"];
export type Presenca = components["schemas"]["AttendanceResponseDTO"];
export type Exigencia = components["schemas"]["VaccineRequirementResponseDTO"];

/**
 * A operacao da creche, pelo lado da tela.
 *
 * <b>Nenhuma decisao de aptidao mora aqui.</b> O servidor devolve a comprovacao linha por linha,
 * com `state` e `blocks` prontos — e isso e deliberado do outro lado: "o produto responde pela
 * saude" (Tela 10). Se a tela recalculasse, a primeira que errasse deixaria entrar um animal com
 * antirrabica vencida.
 */

/** As turmas da organizacao ativa, com a ocupacao contada. */
export function useTurmas() {
  return useQuery({
    queryKey: ["turmas"],
    queryFn: async () => corpoDe(await cliente.GET("/professional/creche/class-groups", {})),
  });
}

export function useCriarTurma() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({ nome, vagas }: { nome: string; vagas?: number }) =>
      corpoDe(
        await cliente.POST("/professional/creche/class-groups", {
          body: { name: nome, ...(vagas === undefined ? {} : { capacity: vagas }) },
        }),
      ),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["turmas"] });
    },
  });
}

/** O que a creche exige da carteira de quem entra. */
export function useExigencias() {
  return useQuery({
    queryKey: ["exigencias"],
    queryFn: async () => corpoDe(await cliente.GET("/professional/creche/vaccine-requirements", {})),
  });
}

/** As matriculas da turma, com a comprovacao reavaliada a cada leitura. */
export function useMatriculasDaTurma(classGroupId: string | undefined) {
  return useQuery({
    queryKey: ["matriculas-da-turma", classGroupId],
    enabled: classGroupId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/professional/creche/class-groups/{classGroupId}/enrollments", {
          params: { path: { classGroupId: classGroupId! } },
        }),
      ),
  });
}

/**
 * As matriculas de um animal, pelo lado do tutor.
 *
 * E a Tela 10 vista por quem concedeu acesso: o tutor precisa ver o que a creche esta esperando
 * dele — "falta a antirrabica em dia" — sem ser membro de creche nenhuma.
 */
export function useMatriculasDoAnimal(animalId: string | undefined) {
  return useQuery({
    queryKey: ["matriculas-do-animal", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/enrollments", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

export function useMatricular() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async ({ animalId, classGroupId }: { animalId: string; classGroupId: string }) =>
      corpoDe(
        await cliente.POST(
          "/professional/creche/class-groups/{classGroupId}/enrollments/{animalId}",
          { params: { path: { classGroupId, animalId } } },
        ),
      ),
    onSuccess: async () => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["matriculas-da-turma"] }),
        consultas.invalidateQueries({ queryKey: ["matriculas-do-animal"] }),
        consultas.invalidateQueries({ queryKey: ["turmas"] }),
        consultas.invalidateQueries({ queryKey: ["dia-da-turma"] }),
      ]);
    },
  });
}

/**
 * O dia da turma.
 *
 * Sem data e HOJE, que e o gesto das 7h30 — e a rota trata a ausencia, para a tela nao ter de
 * calcular "hoje" e errar de fuso na virada da meia-noite.
 */
export function useDiaDaTurma(classGroupId: string | undefined) {
  return useQuery({
    queryKey: ["dia-da-turma", classGroupId],
    enabled: classGroupId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/professional/creche/class-groups/{classGroupId}/day", {
          params: { path: { classGroupId: classGroupId! } },
        }),
      ),
  });
}

/**
 * Marcar entrada, saida e falta.
 *
 * <b>As tres invalidam o dia inteiro, e nao a linha.</b> Marcar entrada muda a contagem do topo
 * ("6 ja chegaram"), e uma tela que atualizasse so a linha mostraria um numero velho ao lado de
 * uma linha nova — que e o tipo de divergencia que faz a monitora contar cachorro na mao.
 */
function useGestoDoDia(gesto: "check-in" | "check-out" | "absence") {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (enrollmentId: string) => {
      const { data, error } =
        gesto === "check-in"
          ? await cliente.POST("/professional/creche/enrollments/{enrollmentId}/check-in", {
              params: { path: { enrollmentId } },
            })
          : gesto === "check-out"
            ? await cliente.POST("/professional/creche/enrollments/{enrollmentId}/check-out", {
                params: { path: { enrollmentId } },
              })
            : await cliente.POST("/professional/creche/enrollments/{enrollmentId}/absence", {
                params: { path: { enrollmentId } },
              });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["dia-da-turma"] });
    },
  });
}

export const useMarcarEntrada = () => useGestoDoDia("check-in");
export const useMarcarSaida = () => useGestoDoDia("check-out");
export const useMarcarFalta = () => useGestoDoDia("absence");

/** As frases dos tres estados da comprovacao. "Nao sabemos" nao e "esta ruim". */
export const FRASE_DA_COMPROVACAO: Record<string, string> = {
  EM_DIA: "creche.comprovacao.EM_DIA",
  VENCIDA: "creche.comprovacao.VENCIDA",
  SEM_REGISTRO: "creche.comprovacao.SEM_REGISTRO",
};

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Agendamento = components["schemas"]["ServiceAppointmentResponseDTO"];
export type LinhaDeSeguranca = components["schemas"]["ServiceSafetyNoteDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A agenda de banho e tosa (Tela 18), pelo lado da tela.
 *
 * <b>"O petshop e agenda, nao diaria de cuidado."</b> Tudo aqui depende da organizacao declarada no
 * cabecalho — quem trabalha em dois petshops tem de dizer em qual esta, senao o banho aparece na
 * agenda errada.
 *
 * <b>As linhas de seguranca vem do SERVIDOR ja recortadas pelo escopo.</b> A tela nao filtra nada:
 * "e tudo o que Marcelo compartilhou, e e tudo o que o banho exige".
 */

export function useAgendaDoDia(dia?: string) {
  return useQuery({
    queryKey: ["agenda", dia ?? "hoje"],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/group/appointments", {
          params: { query: dia === undefined ? {} : { day: dia } },
        }),
      ),
  });
}

export function useAgendarBanho() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: { animalId: string; quando: string; servico: string }) =>
      corpoDe(
        await cliente.POST("/group/appointments", {
          body: {
            animalId: pedido.animalId,
            scheduledAt: pedido.quando,
            service: pedido.servico,
          },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["agenda"] }),
  });
}

export function useMarcarEntrada() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (appointmentId: string) =>
      corpoDe(
        await cliente.POST("/group/appointments/{appointmentId}/check-in", {
          params: { path: { appointmentId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["agenda"] }),
  });
}

/**
 * "Entregar e avisar Marcelo."
 *
 * O texto, quando houver, vira OBSERVACAO na linha do tempo do animal — e nunca ato clinico.
 * Invalida a linha do tempo junto, porque ela acabou de ganhar um evento.
 */
export function useEntregarAnimal() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pedido: { appointmentId: string; nota?: string }) =>
      corpoDe(
        await cliente.POST("/group/appointments/{appointmentId}/deliver", {
          params: { path: { appointmentId: pedido.appointmentId } },
          body: { note: pedido.nota },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries(),
  });
}

export function useMarcarFalta() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (appointmentId: string) =>
      corpoDe(
        await cliente.POST("/group/appointments/{appointmentId}/no-show", {
          params: { path: { appointmentId } },
        }),
      ),
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["agenda"] }),
  });
}

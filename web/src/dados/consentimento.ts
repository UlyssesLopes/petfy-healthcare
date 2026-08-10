import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type EstadoDoConsentimento = components["schemas"]["ConsentStatusResponseDTO"];

/**
 * O que a pessoa aceitou, e o que mudou desde entao.
 *
 * <b>"Sem alarme e sem tom de erro: o texto mudou, nao a conta"</b> — e a nota do desenho ao
 * lado deste painel (Tela 07). Por isso o estado pendente nao bloqueia nada e nao vira
 * modal: e uma faixa que espera.
 */
export function useConsentimento() {
  return useQuery({
    queryKey: ["consentimento"],
    queryFn: async () => corpoDe(await cliente.GET("/consents/me", {})),
  });
}

/**
 * Aceitar o que esta vigente.
 *
 * A rota nao recebe corpo: ela aceita <b>os documentos vigentes</b>, e nao uma versao que o
 * cliente escolhe. E o certo — cliente que manda a versao pode aceitar uma que ja nao vale.
 */
export function useAceitarConsentimento() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const { error } = await cliente.POST("/consents/accept", {});

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["consentimento"] });
    },
  });
}

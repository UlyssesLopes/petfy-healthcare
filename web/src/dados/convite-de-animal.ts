import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type PreviaDoConviteDeAnimal =
  components["schemas"]["PetTutorInvitePreviewResponseDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O outro lado do convite de animal (Telas 19, 20 e 21).
 *
 * <b>Ele fechava um buraco que nenhuma tela mostrava:</b> a adocao da colonia (Tela 44) e a
 * transferencia de titularidade emitem convite, e ate aqui NAO HAVIA TELA PARA ACEITAR NENHUM DOS
 * DOIS. O endpoint existia no backend e o frontend nunca o chamava — dois fluxos entregues terminavam
 * num convite que ninguem conseguia aceitar.
 *
 * <b>Ler nao consome.</b> Abrir o link para entender o que esta sendo oferecido nao pode gastar o
 * direito de entrar — a mesma regra do convite de organizacao.
 */

export function usePreviaDoConviteDeAnimal(token: string) {
  return useQuery({
    queryKey: ["convite-de-animal", token],
    enabled: token !== "",
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/pet-tutor-invites/{token}", {
          params: { path: { token } },
        }),
      ),
    /*
     * Nao insiste: o servidor responde IGUAL para convite inexistente, expirado, revogado, ja usado
     * e enderecado a outra pessoa — e repetir tres vezes nao muda nenhum desses.
     */
    retry: false,
  });
}

export function useAceitarConviteDeAnimal() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (token: string) =>
      corpoDe(
        await cliente.POST("/pet-tutor-invites/{token}/accept", {
          params: { path: { token } },
        }),
      ),
    // aceitar muda quem responde pelo animal ou quem o alcanca: a lista de animais, a rede de
    // cuidado e a carteira passam a dizer outra coisa
    onSuccess: () => consultas.invalidateQueries(),
  });
}

export function useRecusarConviteDeAnimal() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (token: string) => {
      await cliente.POST("/pet-tutor-invites/{token}/reject", {
        params: { path: { token } },
      });
    },
    onSuccess: () => consultas.invalidateQueries({ queryKey: ["convite-de-animal"] }),
  });
}

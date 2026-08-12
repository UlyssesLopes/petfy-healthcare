import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type AnoDoAnimal = components["schemas"]["AnimalYearDTO"];
export type VacinaVencida = components["schemas"]["AnimalYearLapseDTO"];
export type NaoReavaliada = components["schemas"]["AnimalYearPendingDTO"];
export type QuemCuidou = components["schemas"]["AnimalYearCaregiverDTO"];

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O ano do animal (Tela 48), pelo lado da tela.
 *
 * <b>Uma leitura, e nenhuma escrita.</b> O resumo e montado na hora a partir da vida registrada —
 * guardar numa tabela congelaria numeros que mudam quando alguem corrige uma data, e corrigir data e
 * um direito que este produto garante desde a contestacao de registro.
 *
 * <b>E nao ha nada aqui para compartilhar</b>: sem imagem pronta, sem texto de story, sem marca
 * d'agua. Se o tutor quiser mostrar, ele imprime.
 */
export function useAnoDoAnimal(animalId: string, ate?: string) {
  return useQuery({
    queryKey: ["ano", animalId, ate],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/year", {
          params: { path: { animalId }, query: ate === undefined ? {} : { to: ate } },
        }),
      ),
  });
}

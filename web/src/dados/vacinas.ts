import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import { corpoDe } from "./resposta.ts";

/**
 * A dose anterior, lida para que a nova nasca na MESMA serie.
 *
 * <b>Por que isso importa.</b> O feed so para de cobrar quando a aplicacao mais recente
 * da serie esta em dia, e a serie e identificada pelo catalogo quando ele existe. Se a
 * dose nova fosse registrada como texto livre enquanto a anterior tem catalogo, as duas
 * cairiam em series diferentes — e a antiga continuaria cobrando para sempre, que e
 * exatamente o defeito que acabamos de corrigir.
 *
 * Por isso a tela le a dose que esta vencendo e reaproveita o catalogo e o nome dela.
 */
export function useDoseAnterior(vaccineId: string | undefined) {
  return useQuery({
    queryKey: ["vacina", vaccineId],
    enabled: vaccineId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/vaccines/{vaccineId}", {
          params: { path: { vaccineId: vaccineId! } },
        }),
      ),
  });
}

export function useRegistrarDose() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (dose: {
      animalId: string;
      vaccineName: string;
      vaccineCatalogId: string | undefined;
      applicationDate: string;
      nextDoseDate: string | undefined;
    }) => {
      const { error } = await cliente.POST("/vaccines", {
        body: {
          animalId: dose.animalId,
          vaccineName: dose.vaccineName,
          vaccineCatalogId: dose.vaccineCatalogId,
          applicationDate: dose.applicationDate,
          nextDoseDate: dose.nextDoseDate,
        },
      });

      if (error !== undefined) {
        throw error;
      }
    },
    onSuccess: async () => {
      /*
       * A pendencia sai do feed e o registro entra na linha do tempo — "o que sai do feed
       * entra na linha do tempo" (DESIGN.md 5.3), e por isso as duas leituras sao
       * invalidadas juntas. Quem diz o que sobrou e o servidor.
       */
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["pendencias"] }),
        consultas.invalidateQueries({ queryKey: ["linha-do-tempo"] }),
      ]);
    },
  });
}

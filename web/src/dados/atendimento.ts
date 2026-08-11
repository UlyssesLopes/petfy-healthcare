import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Observacao = components["schemas"]["ObservationResponseDTO"];
export type CategoriaDeAtendimento = NonNullable<
  components["schemas"]["HealthRecordRequestDTO"]["category"]
>;

/**
 * O loop central da Tela 31: registrar o atendimento e prescrever.
 *
 * <b>SAO TRES ESCRITAS, e nao uma.</b> O atendimento, a pesagem do dia e cada medicamento moram
 * em recursos diferentes no servidor — e e por isso que eles existem separados: a pesagem entra
 * na serie de peso do animal, e a prescricao vira pendencia diaria na casa do tutor. Amarra-los
 * num endpoint so faria a prescricao virar texto dentro do prontuario, e o feed do tutor nao
 * teria como saber que ha remedio para dar hoje.
 */

/**
 * As observacoes que outros registraram — as "evidencias" do desenho.
 *
 * <b>Referenciar nao transforma observacao em diagnostico.</b> Ela continua sendo o que e,
 * assinada por quem escreveu: por isso a tela mostra quem disse e quando, e o texto vai para o
 * prontuario entre aspas, atribuido. O que a veterinaria constata e o campo dela.
 */
export function useObservacoes(animalId: string | undefined) {
  return useQuery({
    queryKey: ["observacoes", animalId],
    enabled: animalId !== undefined,
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/animals/{animalId}/observations", {
          params: { path: { animalId: animalId! } },
        }),
      ),
  });
}

/**
 * Registrar o atendimento.
 *
 * <b>A rota e a `/professional/...`</b>, e nao a `/health-records`: registrar ato clinico exige
 * credencial profissional ativa, conferida no banco a cada requisicao. A autoria e a organizacao
 * saem do contexto no servidor — quem assina nao vem do corpo, senao seria o cliente escolhendo
 * em nome de quem registrar.
 */
export function useRegistrarAtendimento() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (atendimento: {
      animalId: string;
      categoria: CategoriaDeAtendimento;
      rotulo: string;
      constatacao: string;
      diagnostico?: string;
      quando: string;
    }) => {
      const { data, error } = await cliente.POST("/professional/animals/{animalId}/health-records", {
        params: { path: { animalId: atendimento.animalId } },
        body: {
          animalId: atendimento.animalId,
          category: atendimento.categoria,
          eventType: atendimento.rotulo,
          description: atendimento.constatacao,
          ...(atendimento.diagnostico === undefined || atendimento.diagnostico === ""
            ? {}
            : { diagnosis: atendimento.diagnostico }),
          eventDate: atendimento.quando,
        },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async (_dados, atendimento) => {
      await invalidarOAnimal(consultas, atendimento.animalId);
    },
  });
}

/**
 * A prescricao, que vira pendencia diaria na casa do tutor.
 *
 * <b>Ela sai SEM DONO, e o desenho e explicito:</b> "voce prescreve o que o animal precisa. Quem
 * da cada dose — o tutor, a co-tutora ou a creche — e o tutor quem decide, porque so ele sabe
 * quem estara em casa". Por isso a orientacao aponta para o ANIMAL e nao para uma pessoa: ela
 * segue a custodia, e um tratamento de 21 dias nao morre quando o animal troca de mao.
 *
 * <b>Prescrever nao concede acesso a ninguem.</b> A creche so recebe a dose se ja tiver acesso e
 * se o tutor atribuir — sao duas coisas, e nenhuma delas acontece aqui.
 */
export function usePrescrever() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (receita: {
      animalId: string;
      /* O "como dar" do desenho entra AQUI, e nao num campo proprio: o contrato tem `description`
         e nada mais para texto, e quem le a pendencia precisa ver "junto com a comida" junto com
         o nome do remedio — separa-los faria a instrucao chegar pela metade. */
      descricao: string;
      intervaloEmDias: number;
      comecaEm: string;
      terminaEm: string;
    }) => {
      const { data, error } = await cliente.POST("/animals/{animalId}/care-instructions", {
        params: { path: { animalId: receita.animalId } },
        body: {
          description: receita.descricao,
          intervalDays: receita.intervaloEmDias,
          startsOn: receita.comecaEm,
          endsOn: receita.terminaEm,
        },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async (_dados, receita) => {
      await invalidarOAnimal(consultas, receita.animalId);
    },
  });
}

/** A pesagem do dia, que entra na serie de peso e nao no texto do prontuario. */
export function useRegistrarPeso() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (pesagem: { animalId: string; peso: number; medidoEm: string }) => {
      const { data, error } = await cliente.POST("/animals/{animalId}/weights", {
        params: { path: { animalId: pesagem.animalId } },
        body: { weight: pesagem.peso, measuredAt: pesagem.medidoEm },
      });

      if (error !== undefined) {
        throw error;
      }

      return data;
    },
    onSuccess: async (_dados, pesagem) => {
      await invalidarOAnimal(consultas, pesagem.animalId);
    },
  });
}

/**
 * O que envelhece quando algo e registrado neste animal.
 *
 * Num lugar so porque as tres escritas da Tela 31 envelhecem quase o mesmo, e a lista divergente
 * seria a causa de a tela mostrar o prontuario novo com a carteira velha ao lado.
 */
async function invalidarOAnimal(
  consultas: ReturnType<typeof useQueryClient>,
  animalId: string,
): Promise<void> {
  await Promise.all([
    consultas.invalidateQueries({ queryKey: ["historico-clinico", animalId] }),
    consultas.invalidateQueries({ queryKey: ["linha-do-tempo", animalId] }),
    consultas.invalidateQueries({ queryKey: ["orientacoes", animalId] }),
    consultas.invalidateQueries({ queryKey: ["pesagens", animalId] }),
    consultas.invalidateQueries({ queryKey: ["condicoes", animalId] }),
    /* A agenda e as doses nao sao por animal: a chave e global, e o feed do tutor le dela. */
    consultas.invalidateQueries({ queryKey: ["agenda-de-vacinas"] }),
    consultas.invalidateQueries({ queryKey: ["pendencias"] }),
  ]);
}

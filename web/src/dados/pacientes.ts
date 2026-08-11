import { useQuery } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Paciente = components["schemas"]["VetPetDTO"];
export type ResumoDosPacientes = components["schemas"]["OrganizationPatientsSummaryDTO"];

/**
 * Os quatro numeros do cabecalho, sobre a organizacao INTEIRA.
 *
 * <b>Nao depende da busca nem da pagina, e isso e proposital.</b> Um numero que muda ao filtrar
 * a lista nao e um resumo — a decisao que ele apoia, a quem ligar hoje, e sobre todo mundo. Por
 * isso a `queryKey` nao leva o termo de busca.
 */
export function useResumoDosPacientes() {
  return useQuery({
    queryKey: ["resumo-pacientes"],
    queryFn: async () => corpoDe(await cliente.GET("/professional/animals/summary", {})),
  });
}

/**
 * Os animais que o contexto ativo alcanca — a lista da veterinaria (Tela 03).
 *
 * <b>A busca e do SERVIDOR aqui</b>, ao contrario da lista de organizacoes da Tela 09: a rota
 * tem `q`, e uma clinica com 318 pacientes nao caberia numa pagina para filtrar no cliente.
 *
 * <b>A saude entrou no DTO</b>: `healthStatus` (a dose mais urgente da carteira), `lastVisitAt`
 * e `underTreatment`. Ate entao o `VetPetDTO` tinha nome, tutor, raca, sexo, nascimento e peso —
 * nada sobre saude —, e a coluna mais importante do desenho nao tinha de onde sair. O servidor
 * responde em lote; montar isso no cliente seria uma leitura por animal.
 */
export function usePacientes(busca: string) {
  const alvo = busca.trim();

  return useQuery({
    queryKey: ["pacientes", alvo],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/professional/animals", {
          params: { query: { size: 50, ...(alvo === "" ? {} : { q: alvo }) } },
        }),
      ),
  });
}

/**
 * Os animais sob custodia da organizacao — o abrigo, e nao a clinica.
 *
 * <b>E outra lista, e nao um filtro da de cima.</b> A de cima e "quem eu alcanco porque alguem
 * me concedeu"; esta e "por quem eu respondo". Sao perguntas diferentes, e o abrigo precisa da
 * segunda para decidir uma adocao — o animal resgatado nao tem tutor humano nenhum atras.
 *
 * <b>O `personName` vem vazio de proposito</b>: "sem tutor humano desde o resgate". A tela diz
 * isso em vez de preencher com o nome do abrigo, que faria a coluna de tutor mentir.
 */
export function useSobCustodia(busca: string) {
  const alvo = busca.trim();

  return useQuery({
    queryKey: ["sob-custodia", alvo],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/professional/animals/in-custody", {
          params: { query: { size: 50, ...(alvo === "" ? {} : { q: alvo }) } },
        }),
      ),
  });
}

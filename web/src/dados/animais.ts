import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Animal = components["schemas"]["AnimalResponseDTO"];
export type Especie = NonNullable<components["schemas"]["AnimalRequestDTO"]["species"]>;

/**
 * Os animais que eu alcanco.
 *
 * A listagem e paginada (`Page`, nao array) desde as dividas operacionais de 2026-08-05.
 * A area do tutor responde por um a tres animais (PRODUTO.md 9.2), entao a primeira
 * pagina e a lista inteira na pratica — e quando deixar de ser, quem pagina e a area de
 * organizacao, que le centenas e tem busca e recorte proprios.
 */
export function useAnimais() {
  return useQuery({
    queryKey: ["animais"],
    queryFn: async () =>
      corpoDe(
        // Sem `page` nem `size`: valem os defaults do servidor, e agora o contrato os
        // declara — 20 itens, ordenados por nome. Ate 2026-08-08 era preciso mandar um
        // `pageable` vazio aqui para driblar o contrato, que descrevia a paginacao de um
        // jeito que o Spring nao le.
        await cliente.GET("/animals"),
      ).content ?? [],
  });
}

/**
 * Cadastrar o animal — o passo 1 do onboarding, e o unico obrigatorio dele.
 *
 * <b>Nome e especie bastam</b>, e isso e regra de dominio e nao economia de tela: o
 * `AnimalRequestDTO` exige exatamente esses dois. O desenho diz a mesma coisa com outras
 * palavras — "o resto pode entrar a qualquer momento, inclusive anos depois".
 *
 * O nascimento chega como `03/2019` e vai como data: <b>dia 1</b>, porque o campo do
 * contrato e `date` e nao existe "mes e ano". Guardar o dia 1 e mentir menos que guardar
 * hoje, e o desenho ja avisa que estimativa serve.
 */
export function useCriarAnimal() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (animal: { nome: string; especie: Especie; nascimento?: string }) =>
      corpoDe(
        await cliente.POST("/animals", {
          body: {
            name: animal.nome,
            species: animal.especie,
            ...(animal.nascimento === undefined || animal.nascimento === ""
              ? {}
              : { bornDate: `${animal.nascimento}-01` }),
          },
        }),
      ),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["animais"] });
    },
  });
}

/**
 * O passo 2 do onboarding: a identificacao.
 *
 * <b>So o microchip existe.</b> O desenho pede tres campos — microchip, RGA e tatuagem — e o
 * `AnimalRequestDTO` tem `microchipNumber` e mais nada parecido. Os outros dois nao entram
 * com nome trocado: RGA e tatuagem sao numeros de origem diferente, e enfia-los no campo do
 * chip faria o produto afirmar que o animal tem chip quando ele tem uma tatuagem.
 *
 * O PUT exige `name` e `species` porque o DTO de escrita e o mesmo da criacao — quem chama
 * manda os dois de volta, senao apaga.
 */
export function useIdentificarAnimal() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (dados: {
      animalId: string;
      nome: string;
      especie: Especie;
      microchip: string;
    }) =>
      corpoDe(
        await cliente.PUT("/animals/{animalId}", {
          params: { path: { animalId: dados.animalId } },
          body: {
            name: dados.nome,
            species: dados.especie,
            microchipNumber: dados.microchip,
          },
        }),
      ),
    onSuccess: async (_dado, variaveis) => {
      await Promise.all([
        consultas.invalidateQueries({ queryKey: ["animais"] }),
        consultas.invalidateQueries({ queryKey: ["animal", variaveis.animalId] }),
      ]);
    },
  });
}

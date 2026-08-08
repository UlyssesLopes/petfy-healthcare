/**
 * A fronteira onde a resposta da API vira dado da aplicacao.
 *
 * <b>O que este arquivo deixou de fazer, e por que.</b> Ate aqui ele removia as chaves de
 * valor nulo, em profundidade, de toda resposta. O motivo era real: o contrato nao declara
 * `required` em nenhum schema de resposta — sao 19 de 84, e os 19 sao `*RequestDTO`, porque
 * o `@NotNull` da validacao de entrada e quem faz o springdoc emitir `required`. Todo campo
 * de resposta chega opcional no tipo gerado, `campo?: string`; e enquanto o backend mandava
 * `null` em vez de omitir, o tipo mentia e o TypeScript aprovava `campo !== undefined` logo
 * antes de a tela quebrar.
 *
 * <b>A correcao de raiz chegou, e ela e do outro lado:</b>
 * `spring.jackson.default-property-inclusion=non_null`. O campo nulo nao viaja mais, entao
 * `campo?: T` passou a ser verdade literal — ou vem com valor, ou nao vem. Normalizar aqui
 * virou remendo sobre conserto: custo em toda resposta para desfazer algo que nao acontece
 * mais, e — pior — o remendo escondia a volta do defeito, caso alguem apagasse a linha.
 *
 * <b>Quem cobra agora e o build do backend</b>, em `RespostaOmiteNulosTest`: ele afirma
 * sobre o `ObjectMapper` injetado, o mesmo que serializa toda resposta desta API, e quebra
 * se a configuracao sair. A garantia saiu do runtime do navegador e virou teste, que e o
 * lugar onde ela falha cedo e para todo mundo.
 */

/**
 * O corpo de uma resposta do `openapi-fetch`, ou uma excecao.
 *
 * Existe para que nenhum hook repita o mesmo par — lancar o erro e recusar corpo vazio.
 * Repetido, um dos dois acaba esquecido em algum hook novo.
 */
export function corpoDe<T>(resultado: { data?: T; error?: unknown }): T {
  if (resultado.error !== undefined) {
    throw resultado.error;
  }

  if (resultado.data === undefined) {
    throw new Error("A API respondeu sem corpo onde havia corpo esperado.");
  }

  return resultado.data;
}

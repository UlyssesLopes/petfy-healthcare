/**
 * A fronteira onde a resposta da API vira dado da aplicacao.
 *
 * <b>O problema que este arquivo existe para resolver.</b> O contrato nao declara
 * `required` em nenhum schema de resposta — sao 19 de 84, e os 19 sao `*RequestDTO`,
 * porque o `@NotNull` da validacao de entrada e quem faz o springdoc emitir `required`.
 * Entao todo campo de resposta chega opcional no tipo gerado: `campo?: string`.
 *
 * So que o backend <b>nao omite</b> o campo — ele manda `null`. E o schema tambem nao diz
 * `nullable`, entao o tipo gerado nao tem `| null`. <b>O tipo mente sobre o valor</b>, e o
 * TypeScript aprova `pendencia.lastFulfilledAt !== undefined` logo antes de a tela quebrar
 * com "Cannot read properties of null". Foi exatamente o que aconteceu na primeira vez que
 * a home abriu com dados de verdade.
 *
 * <b>Por que normalizar aqui, e nao checar em cada tela.</b> Trocar `!== undefined` por
 * `!= null` em cada ponto conserta os pontos de hoje e nao conserta o proximo: a armadilha
 * continua armada, e o tipo continua mentindo. Removendo a chave nula na entrada, o valor
 * passa a ser mesmo `undefined`, e o tipo gerado volta a ser verdadeiro para a aplicacao
 * inteira.
 *
 * A correcao de raiz e do outro lado — `non_null` no Jackson, ou `required` declarado nos
 * DTOs de resposta — e esta registrada como divida. Enquanto ela nao vem, a mentira para
 * nesta funcao.
 */

/** Remove as chaves de valor nulo, em profundidade. Ler uma chave que sumiu da `undefined`. */
export function semNulos<T>(valor: T): T {
  return normalizar(valor) as T;
}

function normalizar(valor: unknown): unknown {
  if (Array.isArray(valor)) {
    return valor.map(normalizar);
  }

  if (typeof valor === "object" && valor !== null) {
    const saida: Record<string, unknown> = {};

    for (const [chave, conteudo] of Object.entries(valor)) {
      if (conteudo === null) {
        continue;
      }
      saida[chave] = normalizar(conteudo);
    }

    return saida;
  }

  return valor;
}

/**
 * O corpo de uma resposta do `openapi-fetch`, ou uma excecao.
 *
 * Existe para que nenhum hook repita o mesmo trio — lancar o erro, recusar corpo vazio,
 * normalizar os nulos. Repetido, um dos tres acaba esquecido em algum hook novo.
 */
export function corpoDe<T>(resultado: { data?: T; error?: unknown }): T {
  if (resultado.error !== undefined) {
    throw resultado.error;
  }

  if (resultado.data === undefined) {
    throw new Error("A API respondeu sem corpo onde havia corpo esperado.");
  }

  return semNulos(resultado.data);
}

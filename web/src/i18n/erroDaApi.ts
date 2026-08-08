import type { IntlShape } from "react-intl";

import { ehRespostaDeErro } from "../dados/RespostaDeErro.ts";
import { mensagens } from "./mensagens/pt-BR.ts";

/**
 * O unico lugar onde um erro da API vira texto de tela.
 *
 * <b>A regra que este arquivo carrega:</b> a `message` que o servidor manda nunca chega
 * a ser lida. Ela esta em ingles no `ErrorMessageEnum`, e em portugues sem acento com
 * nome de campo em ingles quando vem da validacao — os dois quebrariam a secao 2 do
 * DESIGN.md. A traducao e por `code`, e so por `code`.
 *
 * Nao usa `status`: 404 e 409 tem varios codigos cada um, e traduzir por status daria a
 * mesma frase para falhas diferentes.
 */
export function textoDoErro(intl: IntlShape, erro: unknown): string {
  const chave = chaveDoErro(erro);
  return intl.formatMessage({ id: chave });
}

/**
 * Separado do `textoDoErro` para poder ser testado sem montar um `IntlShape`, e porque
 * a decisao interessante e esta: qual chave, e quando cair no generico.
 */
export function chaveDoErro(erro: unknown): keyof typeof mensagens {
  if (!ehRespostaDeErro(erro)) {
    return "erro.desconhecido";
  }

  const chave = `erro.${erro.code}`;

  return chave in mensagens ? (chave as keyof typeof mensagens) : "erro.desconhecido";
}

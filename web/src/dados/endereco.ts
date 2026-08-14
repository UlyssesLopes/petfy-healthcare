/**
 * Onde a API mora, e por que isto e um arquivo.
 *
 * <b>Dois lugares precisam do endereco, e um nao pode importar o outro.</b> O `cliente.ts` monta o
 * `openapi-fetch` com ele; o `sessao.ts` chama `/auth/refresh` com `fetch` cru — de proposito,
 * porque usar o cliente ali fecharia um ciclo: o middleware dele pede o token a sessao, e a sessao
 * estaria no meio de renovar o token.
 *
 * Deixar a constante em qualquer um dos dois faria o outro importa-lo, e o ciclo voltaria pela
 * porta dos fundos. Uma constante nao importa ninguem.
 */

/** O `local` do backend libera exatamente esta origem no CORS. */
const BASE_PADRAO = "http://localhost:8080";

export const API: string = import.meta.env.VITE_API_URL ?? BASE_PADRAO;

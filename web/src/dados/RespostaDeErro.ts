/**
 * A forma do erro da API, escrita a mao — e a mao aqui e um sintoma, nao uma escolha.
 *
 * O `contract/openapi.json` descreve so o caminho feliz: `ErrorResponse` nao esta entre
 * os 84 schemas, e nenhuma rota declara resposta de erro. Entao a guarda que o contrato
 * da ao corpo de sucesso — mudou o controller, quebrou o build do front — NAO alcanca o
 * erro. Enquanto for assim, este tipo e uma copia que pode envelhecer em silencio.
 *
 * O que cobre o buraco por ora e a tabela `contract/error-codes.json`, versionada e com
 * guarda dos dois lados: no backend o `ErrorCodesContractTest`, aqui o teste ao lado do
 * `erroDaApi.ts`.
 */
export interface RespostaDeErro {
  /**
   * A mensagem do servidor, em ingles no `ErrorMessageEnum` e em portugues sem acento
   * quando vem da validacao de campo.
   *
   * **Ela nunca vai para a tela.** Existe neste tipo porque chega na resposta, e some
   * no `textoDoErro`, que traduz por `code`. Exibi-la quebraria a secao 2 do DESIGN.md
   * duas vezes: o idioma, e a voz.
   */
  message: string;

  /** O codigo do `ErrorMessageEnum`. E por ele, e so por ele, que a traducao acontece. */
  code: number;

  /** O status HTTP. Nao serve para traduzir: 404 e 409 tem varios codigos cada um. */
  status: number;

  timestamp: string;
}

/**
 * Nem todo erro que chega ao cliente tem esta forma: queda de rede, HTML de proxy e
 * resposta cortada no meio nao passam pelo `GlobalExceptionHandler`. Por isso a
 * verificacao e de forma, e nao um `as`.
 */
export function ehRespostaDeErro(valor: unknown): valor is RespostaDeErro {
  if (typeof valor !== "object" || valor === null) {
    return false;
  }

  const candidato = valor as Record<string, unknown>;

  return typeof candidato["code"] === "number" && typeof candidato["status"] === "number";
}

import { useSyncExternalStore } from "react";

import { assinarSessao, lerSessao, type EstadoDaSessao } from "./sessao.ts";

/**
 * O que a tela pode saber sobre a sessao: se ha alguem autenticado, quem, e por que a
 * sessao anterior acabou. <b>O token nao esta aqui</b>, e a ausencia e a regra.
 */
export function useSessao(): EstadoDaSessao {
  return useSyncExternalStore(assinarSessao, lerSessao, lerSessao);
}

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";

export type Aparelho = components["schemas"]["PersonSessionResponseDTO"];

/**
 * Os aparelhos conectados a conta (Tela 36).
 *
 * <b>A tela dizia que o produto nao sabia</b>, e a justificativa era o JWT sem estado. Ela caiu na
 * V50, e nao por mudanca de arquitetura: o filtro de autenticacao ja consultava o banco em toda
 * requisicao, desde o P1, para derrubar token anterior a uma troca de senha. Faltava uma linha por
 * sessao.
 */
export function useAparelhos() {
  return useQuery({
    queryKey: ["aparelhos"],
    queryFn: async () => corpoDe(await cliente.GET("/persons/me/sessions")) ?? [],
  });
}

/**
 * Encerra uma entrada.
 *
 * <b>Encerrar a atual e sair</b>, e o servidor permite: quem esta lendo isto num aparelho
 * emprestado quer exatamente isso. Quem avisa antes e a tela.
 */
export function useEncerrarAparelho() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (personSessionId: string) =>
      corpoDe(
        await cliente.DELETE("/persons/me/sessions/{personSessionId}", {
          params: { path: { personSessionId } },
        }),
      ),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["aparelhos"] });
    },
  });
}

/**
 * O nome que a pessoa reconhece, tirado do user agent.
 *
 * <b>Ela nao le "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."</b>, e mostrar a
 * string crua seria o mesmo que nao mostrar nada. O que identifica um aparelho para quem o usa e
 * "Chrome no Windows".
 *
 * <b>E quando nao da para reconhecer, a funcao devolve `undefined`</b> em vez de chutar: a tela
 * escreve "aparelho nao identificado", que e verdade, no lugar de um palpite que faria a pessoa
 * encerrar a sessao errada.
 */
export function apelidoDoAparelho(userAgent: string | undefined): string | undefined {
  if (userAgent === undefined || userAgent.trim() === "") {
    return undefined;
  }

  /* A ordem importa: Edge se diz Chrome, e Chrome se diz Safari. O mais especifico vence. */
  const navegador =
    /Edg\//.test(userAgent) ? "Edge"
    : /OPR\//.test(userAgent) ? "Opera"
    : /Firefox\//.test(userAgent) ? "Firefox"
    : /Chrome\//.test(userAgent) ? "Chrome"
    : /Safari\//.test(userAgent) ? "Safari"
    : undefined;

  const sistema =
    /iPhone|iPad/.test(userAgent) ? "iPhone"
    : /Android/.test(userAgent) ? "Android"
    : /Windows/.test(userAgent) ? "Windows"
    : /Mac OS X|Macintosh/.test(userAgent) ? "Mac"
    : /Linux/.test(userAgent) ? "Linux"
    : undefined;

  if (navegador === undefined && sistema === undefined) {
    return undefined;
  }

  return [navegador, sistema].filter(Boolean).join(" · ");
}

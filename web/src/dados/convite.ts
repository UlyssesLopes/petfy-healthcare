import { useMutation } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";

/**
 * De que tipo e este codigo de convite.
 *
 * <b>Quem recebe um codigo nao sabe se ele e de animal ou de organizacao</b>, e nao deveria
 * precisar saber: os dois nomes sao vocabulario de quem escreveu o backend. Sao duas rotas de
 * aceite, e obrigar a pessoa a escolher entre elas seria pedir que ela adivinhe a nossa
 * arquitetura antes de aceitar um convite.
 *
 * Entao a pergunta vai ao servidor. A previa de animal vem primeiro por ser o caso comum — e o
 * unico dos dois que chega a quem ainda nao tem conta nenhuma; se ela recusa, tenta a de
 * organizacao.
 *
 * <b>LER NAO CONSOME.</b> As duas previas sao rotas de leitura: nenhuma delas aceita o convite, e e
 * isso que permite tentar as duas sem risco. Aceitar continua sendo um gesto so, na tela que
 * explica o que muda.
 *
 * <b>E o nao-encontrado e um estado so</b>, como nas duas telas de aceite: o servidor responde
 * IGUAL para convite inexistente, expirado, revogado, ja usado e enderecado a outra pessoa.
 * Distinguir aqui diria a quem tenta adivinhar qual parte errou — e aqui seria pior, porque este
 * caminho aceita qualquer texto que a pessoa colar.
 */
export type TipoDeConvite = "ANIMAL" | "ORGANIZACAO" | "NENHUM";

export function useProcurarConvite() {
  return useMutation({
    mutationFn: async (token: string): Promise<TipoDeConvite> => {
      const doAnimal = await cliente.GET("/pet-tutor-invites/{token}", {
        params: { path: { token } },
      });

      if (doAnimal.data !== undefined) {
        return "ANIMAL";
      }

      const daOrganizacao = await cliente.GET("/organizations/invites/preview", {
        params: { query: { token } },
      });

      return daOrganizacao.data !== undefined ? "ORGANIZACAO" : "NENHUM";
    },
  });
}

import { useMutation } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import { corpoDe } from "./resposta.ts";

/**
 * Reenviar a confirmacao de e-mail — o "Reenviar" da faixa do cabecalho.
 *
 * <b>O e-mail e buscado no clique, e nao mantido em cache pela moldura.</b> O
 * `/auth/email-verification/resend` pede o endereco no corpo, e o `/me/context` — que o
 * cabecalho ja le em toda tela — nao o traz. Guardar uma consulta ao `/persons/me` viva em
 * todas as telas para um botao que quase ninguem aperta seria cobrar de todo mundo o preco do
 * caso raro. Aqui a leitura acontece uma vez, quando a pessoa decide reenviar.
 *
 * <b>Nao invalida o contexto depois.</b> Reenviar nao confirma nada: quem confirma e o clique
 * no link do e-mail, noutra aba e talvez noutro dia. Recarregar o contexto aqui faria a faixa
 * piscar e voltar igual, sugerindo que algo mudou quando nada mudou.
 */
export function useReenviarVerificacao() {
  return useMutation({
    mutationFn: async () => {
      const eu = corpoDe(await cliente.GET("/persons/me", {}));

      if (eu.email === undefined) {
        return;
      }

      const { error } = await cliente.POST("/auth/email-verification/resend", {
        body: { email: eu.email },
      });

      if (error !== undefined) {
        throw error;
      }
    },
  });
}

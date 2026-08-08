import { useMutation, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import { corpoDe } from "./resposta.ts";
import { encerrarSessao, iniciarSessao } from "./sessao.ts";

/**
 * Entrar e sair, vistos pela tela.
 *
 * A tela nao conhece `/auth/login`, nao monta corpo e nao toca no token: ela chama
 * `entrar({ email, senha })` e observa o estado. E a mesma fronteira do resto da camada.
 */

export function useEntrar() {
  return useMutation({
    mutationFn: async (credenciais: { email: string; senha: string }) => {
      return corpoDe(
        await cliente.POST("/auth/login", {
          body: { email: credenciais.email, password: credenciais.senha },
        }),
      );
    },
    onSuccess: (resposta) => {
      iniciarSessao(resposta);
    },
  });
}

export function useSair() {
  const consultas = useQueryClient();

  return () => {
    encerrarSessao("saida");
    /*
     * O cache guarda resposta de quem acabou de sair - inclusive nome de animal e de
     * pessoa. Sem limpar, a proxima conta a entrar neste navegador veria, por um
     * instante, os dados da anterior.
     */
    void consultas.clear();
  };
}

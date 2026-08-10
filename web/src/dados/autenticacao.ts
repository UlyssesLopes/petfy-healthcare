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

/**
 * Criar conta — uma so, para todo mundo (Tela 07).
 *
 * <b>Nao existe conta de tutor e conta de veterinario</b>, e a tela diz isso na primeira
 * frase: "uma conta serve para tudo". Quem responde pela area nao e um campo de cadastro, e
 * o que a pessoa TEM — custodia de animal, vinculo com organizacao (PRODUTO.md 9.3).
 *
 * <b>Entra logo depois de criar</b>, com as credenciais que a pessoa acabou de digitar. Sem
 * isso a tela seguinte seria o login, pedindo de novo o que ela escreveu dez segundos antes
 * — e o desenho da Tela 08 e explicito em vir "depois do login".
 *
 * O `crmv` vai junto quando declarado: o `PersonRequestDTO` aceita, e o painel do desenho
 * diz "em qualquer momento" — criar a conta e um desses momentos.
 */
export function useCriarConta() {
  const entrar = useEntrar();

  return useMutation({
    mutationFn: async (conta: {
      nome: string;
      email: string;
      senha: string;
      crmv?: string;
      crmvUf?: string;
    }) => {
      const criada = corpoDe(
        await cliente.POST("/persons", {
          body: {
            name: conta.nome,
            email: conta.email,
            password: conta.senha,
            acceptedTerms: true,
            ...(conta.crmv === undefined || conta.crmv === ""
              ? {}
              : { crmv: conta.crmv, crmvUf: conta.crmvUf }),
          },
        }),
      );

      await entrar.mutateAsync({ email: conta.email, senha: conta.senha });

      return criada;
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

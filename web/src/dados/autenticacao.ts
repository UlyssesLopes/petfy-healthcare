import { useMutation, useQueryClient } from "@tanstack/react-query";

import { cliente } from "./cliente.ts";
import { corpoDe } from "./resposta.ts";
import { encerrarNoServidor, encerrarSessao, iniciarSessao } from "./sessao.ts";

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
      /**
       * O convite de organizacao, quando a pessoa chegou por um.
       *
       * <b>O backend aceita isso desde sempre, e o front nunca mandou.</b> O campo existia no
       * `PersonRequestDTO` e nenhuma tela o preenchia — entao quem recebia um convite sem ter
       * conta dependia de alguem montar a URL na mao. Era a outra metade do buraco que o
       * aceite com conta existente fechou.
       */
      convite?: string;
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
            ...(conta.convite === undefined || conta.convite === ""
              ? {}
              : { inviteToken: conta.convite }),
          },
        }),
      );

      await entrar.mutateAsync({ email: conta.email, senha: conta.senha });

      return criada;
    },
  });
}

/**
 * "Esqueci" — e ele resolve ali mesmo, sem trocar de tela.
 *
 * A Tela 29 escreve a regra: <b>"o erro de senha oferece a saida junto: link por e-mail, ali
 * mesmo, sem trocar de tela"</b>. O link sem senha nao existe na API, mas a recuperacao
 * existe e e publica — entao a saida real e esta, e ela cabe na propria porta.
 *
 * <b>A resposta e sempre a mesma, e isso e proposital do lado do servidor:</b> dizer "nao
 * achamos esse e-mail" entregaria quais contas existem. A tela repete a postura e confirma o
 * envio sem afirmar que a conta existe.
 */
export function usePedirNovaSenha() {
  return useMutation({
    mutationFn: async (email: string) => {
      const { error } = await cliente.POST("/auth/password-reset", { body: { email } });

      if (error !== undefined) {
        throw error;
      }
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

    /*
     * SAIR PASSOU A TER EFEITO NO SERVIDOR (V51), e antes nao tinha.
     *
     * Esquecer o token localmente bastava enquanto ele era a unica coisa que existia — o JWT
     * seguia valido ate expirar, e nao havia nada a invalidar. Com o refresh no cookie, nao
     * avisar o servidor deixaria o navegador capaz de pegar um token novo depois de a pessoa
     * ter clicado em sair. Num computador emprestado, isso e a diferenca entre sair e parecer
     * que saiu.
     *
     * <b>Nao esperamos a resposta</b>: a sessao local ja acabou, e a rede nao pode segurar
     * quem pediu para sair.
     */
    void encerrarNoServidor();
  };
}

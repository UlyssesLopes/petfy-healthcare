import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

import { cliente } from "./cliente.ts";
import { API } from "./endereco.ts";
import type { components } from "./gerado/api";
import { corpoDe } from "./resposta.ts";
import { ensureFresh, lerSessao } from "./sessao.ts";

export type Aviso = components["schemas"]["PersonNotificationResponseDTO"];

/**
 * O aviso que fica dentro do produto.
 *
 * <b>O texto vem PRONTO do servidor</b>, e a tela nao o remonta a partir de tipo de evento e ids.
 * Isso e decisao do modelo, e nao economia: "Ana passou a cuidar do Code" continua sendo o que a
 * pessoa leu no dia em que Ana apagar a conta. Aviso e um FATO — vale depois de o fato deixar de
 * valer —, e uma tela que reconstruisse a frase a partir do estado de agora faria o passado mudar.
 *
 * <b>E e o mesmo texto que foi por e-mail</b>, montado da mesma `Notification` no servidor: duas
 * redacoes do mesmo evento divergiriam no primeiro ajuste de frase, e quem recebe os dois leria
 * coisas diferentes sobre o mesmo fato.
 */
export function useAvisos() {
  return useQuery({
    queryKey: ["avisos"],
    queryFn: async () =>
      corpoDe(
        await cliente.GET("/persons/me/notifications", {
          params: { query: { page: 0, size: 50 } },
        }),
      ).content ?? [],
  });
}

/**
 * Quantos ainda nao foram lidos — a marca no sino.
 *
 * <b>Consulta propria, e nao um `length` do feed.</b> O sino vive na moldura, entao ele e
 * perguntado de TODA tela; carregar cinquenta avisos em cada uma para mostrar um numero seria
 * pagar a leitura inteira pelo enfeite. O servidor tem rota so para isto.
 */
export function useAvisosNaoLidos() {
  const consultas = useQueryClient();

  /*
   * O servidor empurra; a espera de 5 min e so a rede de seguranca de quando a conexao cai sem
   * avisar. Antes do stream isto era 60s, e o aviso chegava com ate um minuto de atraso.
   */
  const consulta = useQuery({
    queryKey: ["avisos", "nao-lidos"],
    queryFn: async () => corpoDe(await cliente.GET("/persons/me/notifications/unread-count")) ?? 0,
    refetchInterval: 300_000,
  });

  useEffect(() => {
    if (!lerSessao().autenticada) {
      return;
    }

    const abortar = new AbortController();
    let tentativas = 0;
    let reconectar: number | undefined;

    const ouvir = async () => {
      try {
        const token = await ensureFresh();

        if (token === null) {
          return;
        }

        /*
         * `fetch` e nao `EventSource`: o EventSource nao manda header, e o unico jeito de
         * autenticar com ele seria por cookie ou por credencial na URL.
         */
        const resposta = await fetch(`${API}/persons/me/notifications/stream`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: abortar.signal,
        });

        if (!resposta.ok || resposta.body === null) {
          throw new Error(String(resposta.status));
        }

        tentativas = 0;
        const leitor = resposta.body.getReader();
        const decodificador = new TextDecoder();

        for (;;) {
          const { done, value } = await leitor.read();

          if (done) {
            break;
          }

          // o evento nao carrega dado: a chegada e o sinal, e a contagem vem da rota propria
          if (decodificador.decode(value, { stream: true }).includes("event:aviso")) {
            void consultas.invalidateQueries({ queryKey: ["avisos"] });
          }
        }
      } catch {
        // queda de rede, deploy, timeout do servidor: reconectar e o caminho normal
      }

      if (!abortar.signal.aborted) {
        // recuo exponencial ate 30s, para o servidor que caiu nao levar uma enxurrada na volta
        tentativas += 1;
        reconectar = window.setTimeout(ouvir, Math.min(1000 * 2 ** tentativas, 30_000));
      }
    };

    void ouvir();

    return () => {
      abortar.abort();
      window.clearTimeout(reconectar);
    };
  }, [consultas]);

  return consulta;
}

export function useMarcarAvisoLido() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async (personNotificationId: string) =>
      corpoDe(
        await cliente.POST("/persons/me/notifications/{personNotificationId}/read", {
          params: { path: { personNotificationId } },
        }),
      ),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["avisos"] });
    },
  });
}

export function useMarcarTodosLidos() {
  const consultas = useQueryClient();

  return useMutation({
    mutationFn: async () => corpoDe(await cliente.POST("/persons/me/notifications/read", {})),
    onSuccess: async () => {
      await consultas.invalidateQueries({ queryKey: ["avisos"] });
    },
  });
}

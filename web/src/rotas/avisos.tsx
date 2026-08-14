import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useAvisos,
  useMarcarAvisoLido,
  useMarcarTodosLidos,
  type Aviso,
} from "../dados/avisos.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O QUE O PETFY TE CONTOU, e ate a V48 ele nao contava nada dentro de si.
 *
 * <b>O produto avisa por e-mail desde o P4 e nunca avisou aqui dentro.</b> A Tela 03 lista quem
 * esta vencendo e nao avisa ninguem; a 47 escreve "voce e avisado na hora" e o produto nao tinha
 * como cumprir. E o e-mail nao alcanca quem ainda nao confirmou o endereco — politica correta la,
 * porque nome de animal e de tutor nao vao para a caixa de um estranho — nem quem simplesmente nao
 * abre a caixa.
 *
 * O sino da moldura existia DESABILITADO, com a frase "nao ha canal de aviso" escrita ao lado. Esta
 * tela e o destino dele.
 *
 * ------------------------------------------------------------ o texto vem pronto, e nao se remonta
 *
 * Cada linha e assunto e corpo como o servidor os gravou — os MESMOS que foram por e-mail. A tela
 * nao reconstroi frase a partir de tipo de evento e ids, e a razao esta no modelo: <b>aviso e um
 * FATO</b>, e "Ana passou a cuidar do Code" continua sendo o que a pessoa leu no dia em que Ana
 * apagar a conta. Remontar faria o passado mudar junto com o presente.
 *
 * ------------------------------------------------------------------- por que nao ha filtro
 *
 * Nem por tipo, nem por animal, nem "so os nao lidos". <b>A lista e curta por natureza</b> — o
 * produto avisa quando alguem entra no animal, quando a titularidade muda, quando um convite e
 * recusado —, e um filtro aqui seria mobilia para uma sala vazia. Quando houver volume que peca
 * filtro, o filtro nasce do volume, e nao da suposicao dele.
 */

export const Route = createFileRoute("/avisos")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Avisos,
});

function Avisos() {
  const intl = useIntl();

  const avisos = useAvisos();
  const marcarLido = useMarcarAvisoLido();
  const marcarTodos = useMarcarTodosLidos();

  if (avisos.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "avisos.oQueE" })} />;
  }

  if (avisos.isError) {
    return <ErroDeCarga erro={avisos.error} oQue={intl.formatMessage({ id: "avisos.oQueE" })} />;
  }

  const lista = avisos.data ?? [];
  const naoLidos = lista.filter((aviso) => aviso.readAt === undefined).length;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "720px", margin: "0 auto" }}>
        <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: "16px", flexWrap: "wrap", marginBottom: "6px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, margin: 0, letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "avisos.titulo" })}
          </h1>

          {naoLidos > 0 && (
            <button
              type="button"
              disabled={marcarTodos.isPending}
              onClick={() => marcarTodos.mutate()}
              style={{ fontFamily: "inherit", fontSize: "14px", color: VERDE, background: "transparent", border: "none", cursor: "pointer", minHeight: "44px", padding: "0" }}
            >
              {intl.formatMessage({ id: "avisos.marcarTodos" })}
            </button>
          )}
        </div>

        <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 24px" }}>
          {intl.formatMessage({ id: "avisos.apoio" })}
        </p>

        {lista.length === 0 ? (
          /*
           * O VAZIO AQUI E BOA NOTICIA, e a frase diz isso. "Nenhum aviso" num produto de saude
           * animal significa que ninguem entrou no seu animal e nada mudou de mao — e um vazio
           * escrito como falha ("nada encontrado") ensinaria a pessoa a temer a tela.
           */
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "28px 30px", fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "avisos.vazio" })}
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {lista.map((aviso) => (
              <Linha
                key={aviso.personNotificationId}
                aviso={aviso}
                aoLer={() => {
                  if (aviso.readAt === undefined && aviso.personNotificationId !== undefined) {
                    marcarLido.mutate(aviso.personNotificationId);
                  }
                }}
              />
            ))}
          </div>
        )}

        <div style={{ marginTop: "26px" }}>
          <Link to="/" style={{ color: VERDE, fontSize: "15px" }}>
            {intl.formatMessage({ id: "avisos.inicio" })}
          </Link>
        </div>
      </div>
    </div>
  );
}

function Linha({ aviso, aoLer }: { aviso: Aviso; aoLer: () => void }) {
  const intl = useIntl();
  const lido = aviso.readAt !== undefined;

  return (
    <div
      style={{
        border: "1px solid oklch(0.90 0.008 150)",
        borderRadius: "12px",
        // o nao lido tem fundo branco e o lido tem o fundo da pagina: a diferenca e de PESO, e nao
        // de cor — uma etiqueta colorida em cada linha faria a lista inteira parecer alarme
        background: lido ? "oklch(0.985 0.004 120)" : "oklch(1 0 0)",
        padding: "18px 20px",
      }}
    >
      <div style={{ display: "flex", alignItems: "baseline", gap: "12px", justifyContent: "space-between" }}>
        <div style={{ fontSize: "16px", fontWeight: lido ? 400 : 500, lineHeight: 1.45 }}>
          {aviso.subject}
        </div>

        {aviso.createdAt !== undefined && (
          <div style={{ fontSize: "13px", color: CINZA, flex: "none", fontFamily: "'DM Mono', monospace" }}>
            {intl.formatDate(aviso.createdAt, { day: "2-digit", month: "2-digit" })}
          </div>
        )}
      </div>

      {/*
       * O corpo vem do servidor com quebras de linha — e o mesmo texto do e-mail, que e escrito em
       * linhas. `pre-wrap` preserva o que ja foi decidido la, em vez de a tela reformatar.
       */}
      <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", marginTop: "8px", whiteSpace: "pre-wrap" }}>
        {aviso.body}
      </div>

      {!lido && (
        <button
          type="button"
          onClick={aoLer}
          style={{ fontFamily: "inherit", fontSize: "14px", color: VERDE, background: "transparent", border: "none", cursor: "pointer", minHeight: "44px", padding: "0", marginTop: "6px" }}
        >
          {intl.formatMessage({ id: "avisos.marcarLido" })}
        </button>
      )}
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

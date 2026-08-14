import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { useProcurarConvite } from "../dados/convite.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * ONDE SE COLA O CODIGO DE UM CONVITE.
 *
 * <b>Ele existe porque o e-mail de convite manda um CODIGO, e nao um link.</b> Nenhuma notificacao
 * deste produto carrega link — a confirmacao de e-mail manda "Codigo: ..." e a pessoa cola —, e um
 * link exigiria uma URL publica configurada por ambiente, que nao existe em lugar nenhum. Sem esta
 * tela o codigo nao teria onde ser colado, e o e-mail mandaria a pessoa a lugar nenhum.
 *
 * ------------------------------------------------------------ uma porta, e nao duas
 *
 * <b>Quem recebe um codigo nao sabe de que tipo ele e</b>, e nao deveria precisar saber: "convite de
 * animal" e "convite de organizacao" e vocabulario de quem escreveu o backend. Sao duas rotas de
 * aceite — `/convites/animal` e `/convites/aceitar` — e obrigar a pessoa a escolher entre elas seria
 * pedir que ela adivinhe a nossa arquitetura antes de aceitar um convite.
 *
 * Entao esta tela PERGUNTA AO SERVIDOR de que tipo e o codigo, e leva a pessoa a tela certa. A
 * previa de animal vem primeiro por ser o caso comum; se ela recusa, tenta a de organizacao.
 *
 * <b>E as duas previas nao consomem nada:</b> ler um convite nao o aceita, e por isso da para
 * tentar as duas sem risco. Aceitar continua sendo um gesto so, na tela que explica o que muda.
 *
 * ------------------------------------------------------------ o erro e um estado so
 *
 * Como nas duas telas de aceite: o servidor responde IGUAL para convite inexistente, expirado,
 * revogado, ja usado e enderecado a outra pessoa. Distinguir aqui diria a quem tenta adivinhar qual
 * parte errou — e aqui seria pior ainda, porque esta tela aceita qualquer texto.
 */

export const Route = createFileRoute("/convites/")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: ColarConvite,
});

function ColarConvite() {
  const intl = useIntl();
  const navegar = useNavigate();

  const [codigo, setCodigo] = useState("");
  const [naoAchou, setNaoAchou] = useState(false);

  const procura = useProcurarConvite();
  const procurando = procura.isPending;

  const procurar = async () => {
    const token = codigo.trim();

    if (token === "" || procurando) {
      return;
    }

    setNaoAchou(false);

    const tipo = await procura.mutateAsync(token);

    if (tipo === "ANIMAL") {
      await navegar({ to: "/convites/animal", search: { token } });
      return;
    }

    if (tipo === "ORGANIZACAO") {
      await navegar({ to: "/convites/aceitar", search: { token } });
      return;
    }

    setNaoAchou(true);
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "520px", margin: "0 auto", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "30px 32px 34px" }}>
        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "colarConvite.titulo" })}
        </h1>
        <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 22px" }}>
          {intl.formatMessage({ id: "colarConvite.apoio" })}
        </p>

        <label htmlFor="codigo" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
          {intl.formatMessage({ id: "colarConvite.rotulo" })}
        </label>
        <input
          id="codigo"
          value={codigo}
          onChange={(evento) => {
            setCodigo(evento.target.value);
            setNaoAchou(false);
          }}
          onKeyDown={(evento) => {
            if (evento.key === "Enter") {
              void procurar();
            }
          }}
          placeholder={intl.formatMessage({ id: "colarConvite.exemplo" })}
          style={{ border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", width: "100%", background: "oklch(1 0 0)", fontFamily: "'DM Mono', monospace" }}
        />

        {naoAchou && (
          <div role="alert" style={{ marginTop: "14px", border: "1px solid oklch(0.86 0.03 30)", background: "oklch(0.985 0.008 30)", borderRadius: "8px", padding: "14px 16px", fontSize: "14px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
            {intl.formatMessage({ id: "colarConvite.naoAchou" })}
          </div>
        )}

        <button
          type="button"
          disabled={codigo.trim() === "" || procurando}
          onClick={() => void procurar()}
          style={{
            fontFamily: "inherit",
            fontSize: "16px",
            fontWeight: 500,
            color: "oklch(1 0 0)",
            background: codigo.trim() === "" || procurando ? "oklch(0.72 0.02 150)" : VERDE,
            border: "none",
            borderRadius: "8px",
            padding: "15px 26px",
            minHeight: "52px",
            width: "100%",
            marginTop: "16px",
            cursor: codigo.trim() === "" || procurando ? "not-allowed" : "pointer",
          }}
        >
          {intl.formatMessage({ id: procurando ? "colarConvite.procurando" : "colarConvite.abrir" })}
        </button>

        {/*
         * "Ler nao aceita" precisa estar dito ANTES do gesto, e nao depois: quem chega aqui com um
         * codigo de titularidade nao pode temer que colar ja transfira o animal.
         */}
        <div style={{ fontSize: "13px", color: CINZA, marginTop: "12px", lineHeight: 1.55 }}>
          {intl.formatMessage({ id: "colarConvite.lerNaoAceita" })}
        </div>

        <div style={{ marginTop: "22px" }}>
          <Link to="/" style={{ color: VERDE, fontSize: "15px" }}>
            {intl.formatMessage({ id: "colarConvite.inicio" })}
          </Link>
        </div>
      </div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

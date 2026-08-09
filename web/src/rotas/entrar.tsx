import { createFileRoute, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useEntrar } from "../dados/autenticacao.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 28 · a porta — Entrar no Petfy", de `design/IdentidadeVisual/Login Petfy.dc.html`,
 * com o backend ligado nela.
 *
 * O MARKUP VEM DO ARQUIVO: os estilos foram convertidos por script — `style="..."` vira
 * `style={{...}}` e nada mais. O que muda e o conteudo e o comportamento.
 *
 * <b>QUATRO AFORDANCIAS DO DESENHO NAO TEM BACKEND</b>, e elas ficam na tela desabilitadas
 * em vez de sumirem ou de fingirem funcionar. A propria secao 06 da identidade manda:
 * "o desabilitado nunca aparece mudo — ao lado dele, sempre a frase que diz por que". Sao
 * elas: o link por e-mail (nao ha login sem senha na API), o "continuar conectado" (o token
 * vive em memoria e o refresh em cookie foi adiado com gatilho escrito), o cartao de
 * emergencia (o `/share/{token}` exige um token que so quem tem custodia gera) e as duas
 * navegacoes para telas que ainda nao existem.
 */

export const Route = createFileRoute("/entrar")({
  beforeLoad: () => {
    if (lerSessao().autenticada) {
      throw redirect({ to: "/" });
    }
  },
  component: Entrar,
});

/** Erros por campo, e a chave e a mensagem — nunca a frase montada na hora. */
type ErrosDeCampo = { email?: string; senha?: string };

function validar(email: string, senha: string): ErrosDeCampo {
  const erros: ErrosDeCampo = {};

  if (email.trim() === "") {
    erros.email = "entrar.email.faltando";
  } else if (!email.includes("@")) {
    // Deliberadamente frouxo: a checagem existe para pegar o esquecimento obvio, e nao
    // para decidir se um endereco existe - quem decide isso e o servidor de e-mail.
    erros.email = "entrar.email.incompleto";
  }

  if (senha === "") {
    erros.senha = "entrar.senha.faltando";
  }

  return erros;
}

function useHover() {
  const [sobre, setSobre] = useState(false);

  return {
    sobre,
    props: {
      onMouseEnter: () => setSobre(true),
      onMouseLeave: () => setSobre(false),
      onFocus: () => setSobre(true),
      onBlur: () => setSobre(false),
    },
  };
}

function Entrar() {
  const intl = useIntl();
  const navegar = useNavigate();
  const entrar = useEntrar();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [erros, setErros] = useState<ErrosDeCampo>({});

  const principal = useHover();
  const secundario = useHover();
  const cartao = useHover();

  /*
   * Lido uma vez, na montagem: depois de entrar a sessao passa a existir, e o aviso
   * some sozinho. Se fosse lido a cada render, ele piscaria durante a navegacao.
   */
  const [encerramentoAnterior] = useState(() => lerSessao().ultimoEncerramento);

  function enviar(evento: FormEvent) {
    evento.preventDefault();

    const encontrados = validar(email, senha);
    setErros(encontrados);

    if (encontrados.email !== undefined || encontrados.senha !== undefined) {
      return;
    }

    entrar.mutate({ email: email.trim(), senha }, { onSuccess: () => void navegar({ to: "/" }) });
  }

  return (
    <main style={{ minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center", padding: "40px 24px" }}>
      <div style={{ width: "100%", maxWidth: "1180px", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden", display: "grid", gridTemplateColumns: "1.05fr 1fr" }}>

        {/* -------------------------------------------------- a esquerda: por que a conta existe */}
        <div style={{ background: "oklch(0.975 0.008 150)", padding: "56px 56px 52px", display: "flex", flexDirection: "column", justifyContent: "space-between", borderRight: "1px solid oklch(0.92 0.006 150)" }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "56px" }}>
              {/* O simbolo: um anel e um ponto. O que esta no centro e o animal; o que o
                  cerca e a rede de quem cuida (secao 01 da identidade). */}
              <div style={{ width: "30px", height: "30px", borderRadius: "999px", border: "3px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "9px", height: "9px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 600 }}>Petfy</span>
            </div>

            <h2 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "34px", lineHeight: 1.2, fontWeight: 500, margin: "0 0 18px", letterSpacing: "-0.02em", maxWidth: "16ch", textWrap: "pretty" }}>
              {intl.formatMessage({ id: "entrar.tese.titulo" })}
            </h2>
            <p style={{ fontSize: "16px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", margin: 0, maxWidth: "40ch" }}>
              {intl.formatMessage({ id: "entrar.tese.apoio" })}
            </p>
          </div>

          <div style={{ borderTop: "1px solid oklch(0.90 0.008 150)", paddingTop: "28px" }}>
            <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "16px" }}>
              {intl.formatMessage({ id: "entrar.agora.rotulo" })}
            </div>
            <div style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "16px", maxWidth: "42ch" }}>
              {intl.formatMessage({ id: "entrar.agora.texto" })}
            </div>

            <button
              type="button"
              disabled
              {...cartao.props}
              style={{ fontFamily: "inherit", display: "inline-flex", alignItems: "center", fontSize: "16px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "14px 20px", minHeight: "48px", cursor: "not-allowed", opacity: 0.55 }}
            >
              {intl.formatMessage({ id: "entrar.cartao.acao" })}
            </button>
            <PorQueDesabilitado>{intl.formatMessage({ id: "entrar.cartao.porque" })}</PorQueDesabilitado>
          </div>
        </div>

        {/* ------------------------------------------------------------------ a direita: o form */}
        <div style={{ padding: "56px 56px 52px", display: "flex", flexDirection: "column", justifyContent: "center" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "entrar.titulo" })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px" }}>
            {intl.formatMessage({ id: "entrar.subtitulo" })}
          </p>

          {encerramentoAnterior === "expirada" || encerramentoAnterior === "invalidada" ? (
            <p style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)", background: "oklch(0.96 0.006 150)", borderRadius: "8px", padding: "12px 14px", margin: "0 0 20px" }}>
              {intl.formatMessage({
                id: encerramentoAnterior === "expirada" ? "entrar.sessaoExpirada" : "entrar.sessaoInvalidada",
              })}
            </p>
          ) : null}

          <form onSubmit={enviar} noValidate style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div>
              <label htmlFor="email" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
                {intl.formatMessage({ id: "entrar.email" })}
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                autoComplete="email"
                aria-invalid={erros.email !== undefined}
                aria-describedby={erros.email !== undefined ? "email-erro" : undefined}
                style={{ fontFamily: "inherit", width: "100%", border: `1px solid ${erros.email !== undefined ? "oklch(0.55 0.14 30)" : "oklch(0.82 0.012 150)"}`, borderRadius: "4px", padding: "14px", fontSize: "16px", minHeight: "52px", background: "oklch(1 0 0)", color: "oklch(0.25 0.02 150)" }}
              />
              {erros.email !== undefined && (
                <p id="email-erro" style={{ fontSize: "14px", color: "oklch(0.45 0.13 30)", margin: "7px 0 0" }}>
                  {intl.formatMessage({ id: erros.email })}
                </p>
              )}
            </div>

            <div>
              <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", marginBottom: "7px" }}>
                <label htmlFor="senha" style={{ fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)" }}>
                  {intl.formatMessage({ id: "entrar.senha" })}
                </label>
                <button type="button" disabled style={{ fontFamily: "inherit", background: "none", border: "none", padding: 0, fontSize: "14px", color: "oklch(0.46 0.085 150)", cursor: "not-allowed", opacity: 0.55 }}>
                  {intl.formatMessage({ id: "entrar.esqueci" })}
                </button>
              </div>

              <div style={{ border: `1px solid ${erros.senha !== undefined ? "oklch(0.55 0.14 30)" : "oklch(0.82 0.012 150)"}`, borderRadius: "4px", padding: "14px", fontSize: "16px", minHeight: "52px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "12px", background: "oklch(1 0 0)" }}>
                <input
                  id="senha"
                  name="senha"
                  type={mostrarSenha ? "text" : "password"}
                  value={senha}
                  onChange={(evento) => setSenha(evento.target.value)}
                  autoComplete="current-password"
                  aria-invalid={erros.senha !== undefined}
                  aria-describedby={erros.senha !== undefined ? "senha-erro" : undefined}
                  style={{ fontFamily: "inherit", flex: 1, border: "none", outline: "none", fontSize: "16px", background: "transparent", color: "oklch(0.25 0.02 150)", letterSpacing: mostrarSenha || senha === "" ? "normal" : "0.2em" }}
                />
                {/* "Mostrar" existe porque digitar as cegas num teclado de celular, com
                    pressa, e a causa mais comum do erro (o proprio arquivo diz isso). */}
                <button
                  type="button"
                  onClick={() => setMostrarSenha((atual) => !atual)}
                  style={{ fontFamily: "inherit", background: "none", border: "none", padding: 0, fontSize: "14px", color: "oklch(0.46 0.085 150)", cursor: "pointer", flex: "none" }}
                >
                  {intl.formatMessage({ id: mostrarSenha ? "entrar.ocultar" : "entrar.mostrar" })}
                </button>
              </div>

              {erros.senha !== undefined && (
                <p id="senha-erro" style={{ fontSize: "14px", color: "oklch(0.45 0.13 30)", margin: "7px 0 0" }}>
                  {intl.formatMessage({ id: erros.senha })}
                </p>
              )}
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "12px", opacity: 0.55 }}>
              <div style={{ width: "20px", height: "20px", borderRadius: "4px", background: "oklch(0.72 0.012 150)", flex: "none", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "2px", background: "oklch(1 0 0)" }}></div>
              </div>
              <span style={{ fontSize: "15px" }}>{intl.formatMessage({ id: "entrar.continuarConectado" })}</span>
            </div>
            <PorQueDesabilitado semRecuo>{intl.formatMessage({ id: "entrar.continuarConectado.porque" })}</PorQueDesabilitado>

            {/*
              O erro do servidor vive fora dos campos: ele nao pertence a nenhum deles — o
              401 e sobre o PAR. O desenho pede "Essa senha nao confere", e essa frase
              confirma que a conta existe; o backend responde igual para e-mail inexistente
              e senha errada exatamente para nao entregar quais e-mails estao cadastrados.
              A frase segura ganha, e a divergencia esta registrada.
            */}
            {entrar.isError ? (
              <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: 0 }}>
                {intl.formatMessage({ id: chaveDoErro(entrar.error) })}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={entrar.isPending}
              {...principal.props}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: principal.sobre && !entrar.isPending ? "oklch(0.39 0.085 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "16px", minHeight: "56px", cursor: entrar.isPending ? "wait" : "pointer", opacity: entrar.isPending ? 0.7 : 1 }}
            >
              {intl.formatMessage({ id: entrar.isPending ? "entrar.enviando" : "entrar.acao" })}
            </button>

            <div style={{ display: "flex", alignItems: "center", gap: "16px", padding: "4px 0" }}>
              <div style={{ flex: 1, height: "1px", background: "oklch(0.92 0.006 150)" }}></div>
              <span style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>{intl.formatMessage({ id: "entrar.ou" })}</span>
              <div style={{ flex: 1, height: "1px", background: "oklch(0.92 0.006 150)" }}></div>
            </div>

            <button
              type="button"
              disabled
              {...secundario.props}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "16px", minHeight: "56px", cursor: "not-allowed", opacity: 0.55 }}
            >
              {intl.formatMessage({ id: "entrar.link.acao" })}
            </button>
            <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.55, marginTop: "-8px" }}>
              {intl.formatMessage({ id: "entrar.link.apoio" })}
            </div>
            <PorQueDesabilitado semRecuo>{intl.formatMessage({ id: "entrar.link.porque" })}</PorQueDesabilitado>

            <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "24px", fontSize: "16px", color: "oklch(0.45 0.015 150)" }}>
              {intl.formatMessage(
                { id: "entrar.criarConta" },
                {
                  acao: (
                    <button type="button" disabled style={{ fontFamily: "inherit", background: "none", border: "none", padding: 0, fontSize: "16px", color: "oklch(0.46 0.085 150)", cursor: "not-allowed", opacity: 0.55 }}>
                      {intl.formatMessage({ id: "entrar.criarConta.acao" })}
                    </button>
                  ),
                },
              )}
            </div>
          </form>
        </div>
      </div>
    </main>
  );
}

/**
 * A frase que acompanha todo controle desabilitado.
 *
 * A secao 06 da identidade e explicita: "o desabilitado nunca aparece mudo — ao lado dele,
 * sempre a frase que diz por que". Dito ANTES do gesto, e nao depois.
 */
function PorQueDesabilitado({ children, semRecuo }: { children: ReactNode; semRecuo?: boolean }) {
  return (
    <div style={{ fontSize: "13px", lineHeight: 1.5, color: "oklch(0.5 0.015 150)", marginTop: semRecuo ? "-12px" : "10px" }}>
      {children}
    </div>
  );
}

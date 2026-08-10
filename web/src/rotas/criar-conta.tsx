import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useCriarConta } from "../dados/autenticacao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 07 · a porta — Cadastro e login, uma so para todo mundo", de
 * `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * O markup vem do arquivo. A tese esta na primeira frase dela: <b>uma conta serve para
 * tudo</b> — cuidar dos seus animais e atender os de outras pessoas. Nao existe escolher
 * "sou tutor" ou "sou veterinario" aqui, e nao e simplificacao: quem responde pela area e o
 * que a pessoa TEM, nao um campo de cadastro (PRODUTO.md 9.3).
 *
 * <b>A SENHA PEDE 10 CARACTERES E O SERVIDOR ACEITA 8.</b> O desenho escreve "ao menos 10
 * caracteres", o `PersonRequestDTO` declara `minLength: 8`. O cliente fica mais rigoroso que
 * o servidor de proposito — a validacao de campo e do cliente porque a do servidor nao e
 * exibivel (DESIGN.md, emenda da secao 6) —, e a divergencia fica registrada: alinhar o
 * servidor em 10 e mudanca de contrato, e nao de tela.
 *
 * <b>O PAINEL "E-MAIL NAO CONFIRMADO" DO DESENHO NAO EXISTE AQUI, e nao da para fingir.</b>
 * Ele pede uma faixa que aparece enquanto o e-mail nao foi confirmado, e nenhuma rota conta
 * se ele foi: o `/me/context` traz pessoa, papeis e capacidades, e o `/persons/me` nao tem o
 * campo. Mostrar a faixa sempre mentiria para quem ja confirmou. Falta um booleano no
 * backend, e esta registrado.
 *
 * O painel de consentimento vive no `__root.tsx`, porque nao e desta tela: ele aparece em
 * qualquer lugar do produto quando o texto muda.
 */

export const Route = createFileRoute("/criar-conta")({
  component: CriarConta,
});

/** O desenho escreve o minimo em texto, e o texto e a regra. */
const MINIMO_DA_SENHA = 10;

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

function CriarConta() {
  const intl = useIntl();
  const navegar = useNavigate();
  const criar = useCriarConta();
  const acao = useHover();

  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [aceitou, setAceitou] = useState(false);
  const [crmv, setCrmv] = useState("");
  const [crmvUf, setCrmvUf] = useState("");
  const [tocado, setTocado] = useState(false);

  const senhaCurta = senha.length > 0 && senha.length < MINIMO_DA_SENHA;
  const podeEnviar =
    nome.trim() !== "" && email.trim() !== "" && senha.length >= MINIMO_DA_SENHA && aceitou;

  const enviar = async () => {
    setTocado(true);

    if (!podeEnviar) {
      return;
    }

    await criar.mutateAsync({
      nome: nome.trim(),
      email: email.trim(),
      senha,
      crmv: crmv.trim() === "" ? undefined : crmv.trim(),
      crmvUf: crmv.trim() === "" ? undefined : crmvUf.trim().toUpperCase(),
    });

    // A Tela 08 e literalmente "depois do login": e para la que a conta nova vai.
    await navegar({ to: "/comecar" });
  };

  return (
    <div style={{ padding: "40px 24px", display: "flex", gap: "24px", alignItems: "flex-start", justifyContent: "center", flexWrap: "wrap" }}>
      <div style={{ width: "660px", maxWidth: "100%", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ padding: "44px 56px 48px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "40px" }}>
            <div aria-hidden style={{ width: "30px", height: "30px", borderRadius: "999px", border: "3px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <div style={{ width: "9px", height: "9px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
            </div>
            <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 600 }}>Petfy</span>
          </div>

          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "34px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "criarConta.titulo" })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 30px", maxWidth: "46ch" }}>
            {intl.formatMessage({ id: "criarConta.apoio" })}
          </p>

          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              void enviar();
            }}
            style={{ display: "flex", flexDirection: "column", gap: "18px", maxWidth: "420px" }}
          >
            <Campo rotulo={intl.formatMessage({ id: "criarConta.nome" })} para="criar-nome">
              <input
                id="criar-nome"
                type="text"
                value={nome}
                onChange={(evento) => setNome(evento.target.value)}
                autoComplete="name"
                style={estiloDoCampo}
              />
            </Campo>

            <Campo rotulo={intl.formatMessage({ id: "criarConta.email" })} para="criar-email">
              <input
                id="criar-email"
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                autoComplete="email"
                style={estiloDoCampo}
              />
            </Campo>

            <Campo rotulo={intl.formatMessage({ id: "criarConta.senha" })} para="criar-senha">
              <div style={{ display: "flex", gap: "8px" }}>
                <input
                  id="criar-senha"
                  type={mostrarSenha ? "text" : "password"}
                  value={senha}
                  onChange={(evento) => setSenha(evento.target.value)}
                  autoComplete="new-password"
                  aria-describedby="criar-senha-minimo"
                  style={{ ...estiloDoCampo, letterSpacing: mostrarSenha ? "normal" : "0.2em" }}
                />
                {/*
                 * O "Mostrar" e o mesmo da Tela 28, e pelo motivo que ela escreve: digitar
                 * as cegas num teclado de celular, com pressa, e a causa mais comum do erro.
                 */}
                <button
                  type="button"
                  onClick={() => setMostrarSenha((antes) => !antes)}
                  style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "4px", padding: "0 14px", minHeight: "48px", cursor: "pointer", flex: "none" }}
                >
                  {intl.formatMessage({ id: mostrarSenha ? "criarConta.senha.esconder" : "criarConta.senha.mostrar" })}
                </button>
              </div>
              <div
                id="criar-senha-minimo"
                style={{ fontSize: "13px", color: senhaCurta ? "oklch(0.45 0.13 30)" : "oklch(0.5 0.015 150)", marginTop: "7px" }}
              >
                {intl.formatMessage({ id: "criarConta.senha.minimo" }, { minimo: MINIMO_DA_SENHA })}
              </div>
            </Campo>

            <div style={{ display: "flex", gap: "12px", alignItems: "flex-start", paddingTop: "4px" }}>
              <button
                type="button"
                role="checkbox"
                aria-checked={aceitou}
                onClick={() => setAceitou((antes) => !antes)}
                style={{ background: "transparent", border: "none", padding: 0, cursor: "pointer", flex: "none", marginTop: "2px" }}
              >
                <div
                  aria-hidden
                  style={
                    aceitou
                      ? { width: "20px", height: "20px", borderRadius: "4px", background: "oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }
                      : { width: "20px", height: "20px", borderRadius: "4px", border: "1px solid oklch(0.78 0.012 150)" }
                  }
                >
                  {aceitou && <div style={{ width: "8px", height: "8px", borderRadius: "2px", background: "oklch(1 0 0)" }}></div>}
                </div>
              </button>
              <div style={{ fontSize: "15px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)" }}>
                {intl.formatMessage({ id: "criarConta.termos" })}
              </div>
            </div>

            {/*
             * O painel "declarar credencial · em qualquer momento" do desenho. Criar a conta
             * e um desses momentos, e o `PersonRequestDTO` aceita `crmv` e `crmvUf` — entao
             * o campo mora aqui em vez de esperar por uma tela de conta que nao existe.
             */}
            <details style={{ borderTop: "1px solid oklch(0.90 0.008 150)", paddingTop: "18px" }}>
              <summary style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)", cursor: "pointer" }}>
                {intl.formatMessage({ id: "criarConta.credencial.abrir" })}
              </summary>

              <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 92px", gap: "10px" }}>
                  <Campo rotulo={intl.formatMessage({ id: "criarConta.credencial.registro" })} para="criar-crmv">
                    <input
                      id="criar-crmv"
                      type="text"
                      value={crmv}
                      onChange={(evento) => setCrmv(evento.target.value)}
                      placeholder="41.882"
                      style={{ ...estiloDoCampo, fontFamily: "'DM Mono', monospace", minHeight: "44px" }}
                    />
                  </Campo>
                  <Campo rotulo={intl.formatMessage({ id: "criarConta.credencial.uf" })} para="criar-crmv-uf">
                    <input
                      id="criar-crmv-uf"
                      type="text"
                      value={crmvUf}
                      onChange={(evento) => setCrmvUf(evento.target.value.slice(0, 2))}
                      maxLength={2}
                      placeholder="SP"
                      style={{ ...estiloDoCampo, fontFamily: "'DM Mono', monospace", minHeight: "44px", textTransform: "uppercase" }}
                    />
                  </Campo>
                </div>
                <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)" }}>
                  {intl.formatMessage({ id: "criarConta.credencial.informado" })}
                </div>
                <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.5 0.015 150)" }}>
                  {intl.formatMessage({ id: "criarConta.credencial.semClinica" })}
                </div>
              </div>
            </details>

            {criar.isError && (
              <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: 0 }}>
                {intl.formatMessage({ id: chaveDoErro(criar.error) })}
              </p>
            )}

            {tocado && !podeEnviar && !criar.isError && (
              <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", margin: 0 }}>
                {intl.formatMessage({ id: aceitou ? "criarConta.faltaCampo" : "criarConta.faltaAceite" })}
              </p>
            )}

            <button
              type="submit"
              disabled={criar.isPending}
              {...acao.props}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: criar.isPending ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: "pointer" }}
            >
              {intl.formatMessage({ id: criar.isPending ? "criarConta.acao.criando" : "criarConta.acao" })}
            </button>

            <div style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)", textAlign: "center" }}>
              {intl.formatMessage({ id: "criarConta.jaTem" })}{" "}
              <Link to="/entrar" style={{ color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "criarConta.entrar" })}
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

const estiloDoCampo = {
  fontFamily: "inherit",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
} as const;

function Campo({ rotulo, para, children }: { rotulo: string; para: string; children: ReactNode }) {
  return (
    <div>
      <label htmlFor={para} style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
        {rotulo}
      </label>
      {children}
    </div>
  );
}

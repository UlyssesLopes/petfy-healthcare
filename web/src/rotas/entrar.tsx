import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { useEntrar, usePedirNovaSenha } from "../dados/autenticacao.ts";
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
 * <b>Nao ha mais nada desabilitado aqui.</b> O "continuar conectado" virou escolha de verdade
 * (V52), o cartao de emergencia abre pelo codigo em `/cartao/{token}`, e o login por link saiu
 * da tela — sem URL publica ele nao seria link, e um controle cinza com uma frase pedindo
 * desculpa ocupa a porta sem servir a ninguem. Fica registrado no `ROADMAP.md`.
 */

/**
 * O convite que trouxe a pessoa ate aqui, quando foi ele que a trouxe.
 *
 * <b>Viaja o TOKEN, e nao um caminho de destino.</b> Um `?destino=/qualquer/coisa` seria um
 * redirecionamento aberto disfarcado de conveniencia — a tela de entrar mandaria a pessoa
 * autenticada para onde o link mandasse. Com o token, o unico destino possivel e o aceite de
 * convite, e ele esta escrito aqui no codigo.
 */
export const Route = createFileRoute("/entrar")({
  validateSearch: (search: Record<string, unknown>): { convite?: string } =>
    typeof search.convite === "string" && search.convite !== ""
      ? { convite: search.convite }
      : {},
  beforeLoad: ({ search }) => {
    if (lerSessao().autenticada) {
      throw search.convite === undefined
        ? redirect({ to: "/" })
        : redirect({ to: "/convites/aceitar", search: { token: search.convite } });
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

/**
 * A Tela 29 e a MESMA porta, na mao — nao uma segunda tela.
 *
 * O desenho dela repete o formulario a 390 px com alvos maiores: campos de 54 px, principal
 * de 58 px, secundarios de 56 px, "a pressa e parte do contexto de uso". E o espelho da
 * esquerda vira cabecalho centrado em cima, porque a coluna nao caberia ao lado.
 *
 * <b>Por que `matchMedia` e nao media query:</b> o estilo aqui e inline, convertido do
 * arquivo do desenho, e `style` nao aceita `@media`. Traduzir os dois layouts para classe
 * seria reabrir a distancia entre o desenho aprovado e o que sobe — a mesma que fez a Tela 02
 * ser reescrita.
 */
function useEstreito() {
  const [estreito, setEstreito] = useState(
    () => typeof window !== "undefined" && window.matchMedia("(max-width: 720px)").matches,
  );

  useEffect(() => {
    const consulta = window.matchMedia("(max-width: 720px)");
    const ouvir = (evento: MediaQueryListEvent) => setEstreito(evento.matches);

    consulta.addEventListener("change", ouvir);
    return () => consulta.removeEventListener("change", ouvir);
  }, []);

  return estreito;
}

function Entrar() {
  const intl = useIntl();
  const navegar = useNavigate();
  const { convite } = Route.useSearch();
  const entrar = useEntrar();
  const novaSenha = usePedirNovaSenha();
  const estreito = useEstreito();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [manterConectado, setManterConectado] = useState(true);
  const [erros, setErros] = useState<ErrosDeCampo>({});

  const principal = useHover();

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

    /* Quem chegou por um convite volta para ele, e nao para a home: o link veio de fora do
       produto e a sessao fechada e o normal, entao perder o token aqui faria o clique morrer
       na porta — que era exatamente o buraco que o aceite com conta existente veio fechar. */
    entrar.mutate(
      { email: email.trim(), senha, manterConectado },
      {
        onSuccess: () =>
          void (convite === undefined
            ? navegar({ to: "/" })
            : navegar({ to: "/convites/aceitar", search: { token: convite } })),
      },
    );
  }

  return (
    <main style={{ minHeight: "100dvh", display: "flex", alignItems: "center", justifyContent: "center", padding: estreito ? "0" : "40px 24px" }}>
      <div style={{ width: "100%", maxWidth: estreito ? "390px" : "1180px", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden", display: "grid", gridTemplateColumns: estreito ? "1fr" : "1.05fr 1fr" }}>

        {/* -------------------------------------------------- a esquerda: por que a conta existe
         *
         * Na mao (Tela 29) ela nao fica ao lado: vira cabecalho centrado, com o simbolo em
         * 56 px e a frase embaixo. A frase e a mesma nos dois — "nao vende o produto, lembra
         * por que a conta existe".
         */}
        {estreito ? (
          <div style={{ background: "oklch(0.975 0.008 150)", padding: "44px 26px 32px", display: "flex", flexDirection: "column", alignItems: "center", borderBottom: "1px solid oklch(0.92 0.006 150)" }}>
            <div aria-hidden style={{ width: "56px", height: "56px", borderRadius: "999px", border: "4px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center", marginBottom: "20px" }}>
              <div style={{ width: "17px", height: "17px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
            </div>
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 600, marginBottom: "10px" }}>Petfy</div>
            <div style={{ fontSize: "16px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", textAlign: "center", maxWidth: "30ch" }}>
              {intl.formatMessage({ id: "entrar.tese.apoio" })}
            </div>
          </div>
        ) : (
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

              <AbrirCartao />
            </div>
          </div>
        )}

        {/* ------------------------------------------------------------------ a direita: o form */}
        <div style={{ padding: estreito ? "28px 24px 32px" : "56px 56px 52px", display: "flex", flexDirection: "column", justifyContent: "center" }}>
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
                style={{ fontFamily: "inherit", width: "100%", border: `1px solid ${erros.email !== undefined ? "oklch(0.55 0.14 30)" : "oklch(0.82 0.012 150)"}`, borderRadius: "4px", padding: "14px", fontSize: "16px", minHeight: estreito ? "54px" : "52px", background: "oklch(1 0 0)", color: "oklch(0.25 0.02 150)" }}
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
                {/*
                 * "Esqueci" DEIXOU DE SER DESABILITADO, e resolve sem trocar de tela — que e
                 * a regra escrita na Tela 29: "o erro de senha oferece a saida junto, ali
                 * mesmo". O link sem senha nao existe na API; a recuperacao existe e e
                 * publica, e e ela que serve de saida.
                 *
                 * Precisa do e-mail preenchido, e diz isso em vez de falhar calado.
                 */}
                <button
                  type="button"
                  disabled={novaSenha.isPending || novaSenha.isSuccess}
                  onClick={() => {
                    if (email.trim() === "") {
                      setErros((atuais) => ({ ...atuais, email: "entrar.esqueci.precisaEmail" }));
                      return;
                    }

                    novaSenha.mutate(email.trim());
                  }}
                  style={{ fontFamily: "inherit", background: "none", border: "none", padding: 0, fontSize: "14px", color: "oklch(0.46 0.085 150)", cursor: novaSenha.isSuccess ? "default" : "pointer", textDecoration: "underline", textUnderlineOffset: "3px" }}
                >
                  {intl.formatMessage({
                    id: novaSenha.isPending ? "entrar.esqueci.enviando" : "entrar.esqueci",
                  })}
                </button>
              </div>

              {novaSenha.isSuccess && (
                <p style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)", background: "oklch(0.96 0.006 150)", borderRadius: "8px", padding: "12px 14px", margin: "0 0 7px" }}>
                  {intl.formatMessage({ id: "entrar.esqueci.enviado" }, { email: email.trim() })}
                </p>
              )}

              <div style={{ border: `1px solid ${erros.senha !== undefined ? "oklch(0.55 0.14 30)" : "oklch(0.82 0.012 150)"}`, borderRadius: "4px", padding: "14px", fontSize: "16px", minHeight: estreito ? "54px" : "52px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "12px", background: "oklch(1 0 0)" }}>
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

            {/* Marcada por padrao: e o que o login sempre fez. Desmarcar entrega um cookie de
                sessao, que morre ao fechar o navegador — a saida de quem esta num computador
                emprestado. */}
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <input
                id="manter-conectado"
                type="checkbox"
                checked={manterConectado}
                onChange={(evento) => setManterConectado(evento.target.checked)}
                style={{ width: "20px", height: "20px", flex: "none", accentColor: "oklch(0.46 0.085 150)", cursor: "pointer" }}
              />
              <label htmlFor="manter-conectado" style={{ fontSize: "15px", cursor: "pointer" }}>
                {intl.formatMessage({ id: "entrar.continuarConectado" })}
              </label>
            </div>
            <div style={{ fontSize: "13px", lineHeight: 1.5, color: "oklch(0.5 0.015 150)", marginTop: "-12px" }}>
              {intl.formatMessage({
                id: manterConectado
                  ? "entrar.continuarConectado.marcado"
                  : "entrar.continuarConectado.desmarcado",
              })}
            </div>

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
              style={{ fontFamily: "inherit", fontSize: estreito ? "17px" : "16px", fontWeight: 500, color: "oklch(1 0 0)", background: principal.sobre && !entrar.isPending ? "oklch(0.39 0.085 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: estreito ? "17px" : "16px", minHeight: estreito ? "58px" : "56px", cursor: entrar.isPending ? "wait" : "pointer", opacity: entrar.isPending ? 0.7 : 1 }}
            >
              {intl.formatMessage({ id: entrar.isPending ? "entrar.enviando" : "entrar.acao" })}
            </button>

            {/*
             * Na mao, o cartao de emergencia vem AQUI e nao no espelho: a Tela 29 poe ele
             * embaixo do formulario, "a um polegar de distancia", porque quem chega as 3h da
             * manha com o animal passando mal nao deveria enfrentar um formulario.
             */}
            {estreito && (
              <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "18px", display: "flex", flexDirection: "column", gap: "12px" }}>
                <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", lineHeight: 1.55, textAlign: "center" }}>
                  {intl.formatMessage({ id: "entrar.cartao.semSenha" })}
                </div>
                <AbrirCartao />
              </div>
            )}

            <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "24px", fontSize: "16px", color: "oklch(0.45 0.015 150)" }}>
              {intl.formatMessage(
                { id: "entrar.criarConta" },
                {
                  // Deixou de ser desabilitado quando a Tela 07 passou a existir.
                  //
                  // O convite atravessa junto: quem chegou aqui por um convite e descobre que
                  // ainda nao tem conta esta a um clique de perde-lo, e o cadastro sabe usa-lo
                  // (o `inviteToken` do PersonRequestDTO).
                  acao: (
                    <Link
                      to="/criar-conta"
                      search={convite === undefined ? {} : { convite }}
                      style={{ fontSize: "16px", color: "oklch(0.46 0.085 150)" }}
                    >
                      {intl.formatMessage({ id: "entrar.criarConta.acao" })}
                    </Link>
                  ),
                },
              )}
            </div>

            {/*
             * ================================================ o caminho de quem NAO vem entrar
             *
             * A Tela 34 e a unica tela do produto que responde a quem nao tem conta, e ela precisa
             * ser alcancavel de fora — quem acha um animal na rua e abre o Petfy cai aqui.
             *
             * <b>Fica DEPOIS de "criar conta", e nao ao lado do botao de entrar</b>, porque quem
             * chega nesta tela quase sempre vem entrar. Mas fica na porta, e nao escondido: a
             * pessoa que precisa dele esta com um animal no colo e nao vai procurar num menu.
             */}
            <div style={{ paddingTop: "16px", fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
              <Link to="/encontrado" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "entrar.achouUmAnimal" })}
              </Link>
            </div>
          </form>
        </div>
      </div>
    </main>
  );
}

/**
 * A saida de emergencia da porta, e ela abre sem senha.
 *
 * <b>Pede o codigo, e nao um clique.</b> O cartao vive num token que so quem responde pelo animal
 * gera — e como nao ha URL publica configurada, o que chega a quem precisa abrir e o codigo, nao
 * um link clicavel. Colar aqui e a unica forma que existe hoje, e a tela diz isso em vez de
 * oferecer um botao que nao teria para onde ir.
 */
function AbrirCartao() {
  const intl = useIntl();
  const navegar = useNavigate();
  const [codigo, setCodigo] = useState("");

  const limpo = codigo.trim();

  return (
    <form
      onSubmit={(evento) => {
        evento.preventDefault();
        if (limpo !== "") {
          void navegar({ to: "/cartao/$token", params: { token: limpo } });
        }
      }}
      style={{ display: "flex", flexDirection: "column", gap: "10px" }}
    >
      <label htmlFor="codigo-do-cartao" style={{ fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)" }}>
        {intl.formatMessage({ id: "entrar.cartao.codigo" })}
      </label>
      <div style={{ display: "flex", gap: "8px" }}>
        <input
          id="codigo-do-cartao"
          type="text"
          autoComplete="off"
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value)}
          style={{ fontFamily: "'DM Mono', monospace", flex: 1, minWidth: 0, border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "13px 14px", fontSize: "15px", minHeight: "48px", background: "oklch(1 0 0)", color: "oklch(0.25 0.02 150)" }}
        />
        <button
          type="submit"
          disabled={limpo === ""}
          style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", flex: "none", cursor: limpo === "" ? "not-allowed" : "pointer", opacity: limpo === "" ? 0.55 : 1 }}
        >
          {intl.formatMessage({ id: "entrar.cartao.abrir" })}
        </button>
      </div>
      <div style={{ fontSize: "13px", lineHeight: 1.5, color: "oklch(0.5 0.015 150)" }}>
        {intl.formatMessage({ id: "entrar.cartao.ajuda" })}
      </div>
    </form>
  );
}


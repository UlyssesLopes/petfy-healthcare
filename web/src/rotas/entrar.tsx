import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { Plaquinha } from "../componentes/novo/Plaquinha.tsx";
import { useTenhoAlgumAnimal } from "../dados/animais.ts";
import { useEntrar, usePedirNovaSenha } from "../dados/autenticacao.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/porta.css";

/**
 * A porta de quem volta. O painel cobalto é a saída de quem não veio entrar: o cartão de
 * emergência abre sem senha, e quem achou um animal tem caminho sem conta.
 *
 * Viaja o token do convite, e não um destino: `?destino=` seria redirecionamento aberto.
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

/** Erros por campo: a chave da mensagem, nunca a frase montada na hora. */
type ErrosDeCampo = { email?: string; senha?: string };

function validar(email: string, senha: string): ErrosDeCampo {
  const erros: ErrosDeCampo = {};
  if (email.trim() === "") {
    erros.email = "entrar.email.faltando";
  } else if (!email.includes("@")) {
    // Frouxo de propósito: pega o esquecimento óbvio; quem decide se o endereço existe é o servidor.
    erros.email = "entrar.email.incompleto";
  }
  if (senha === "") {
    erros.senha = "entrar.senha.faltando";
  }
  return erros;
}

function Entrar() {
  const intl = useIntl();
  const t = (id: string, valores?: Record<string, string>) => intl.formatMessage({ id }, valores);
  const navegar = useNavigate();
  const { convite } = Route.useSearch();
  const entrar = useEntrar();
  const tenhoAlgumAnimal = useTenhoAlgumAnimal();
  const novaSenha = usePedirNovaSenha();

  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [manterConectado, setManterConectado] = useState(true);
  const [erros, setErros] = useState<ErrosDeCampo>({});

  // Lido na montagem: depois de entrar a sessão existe e o aviso some sozinho.
  const [encerramentoAnterior] = useState(() => lerSessao().ultimoEncerramento);

  // Convite volta para o aceite; sem animal vai para o começo; se a pergunta falhar, para a home.
  async function depoisDeEntrar() {
    if (convite !== undefined) {
      await navegar({ to: "/convites/aceitar", search: { token: convite } });
      return;
    }
    try {
      await navegar({ to: (await tenhoAlgumAnimal()) ? "/" : "/comecar" });
    } catch {
      await navegar({ to: "/" });
    }
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const encontrados = validar(email, senha);
    setErros(encontrados);

    if (encontrados.email !== undefined) {
      document.getElementById("email")?.focus();
      return;
    }
    if (encontrados.senha !== undefined) {
      document.getElementById("senha")?.focus();
      return;
    }

    entrar.mutate(
      { email: email.trim(), senha, manterConectado },
      { onSuccess: () => void depoisDeEntrar() },
    );
  }

  function pedirNovaSenha() {
    if (email.trim() === "") {
      setErros((atuais) => ({ ...atuais, email: "entrar.esqueci.precisaEmail" }));
      document.getElementById("email")?.focus();
      return;
    }
    novaSenha.mutate(email.trim());
  }

  return (
    <div className="pf pf-pagina pf-porta pf-porta--entrar">
      <a className="pf-pular" href="#pf-porta-form">
        {t("entrar.pular")}
      </a>

      <header className="pf-porta__topo">
        <Link to="/conheca" className="pf-porta__marca" translate="no">
          Petfy
        </Link>
      </header>

      <main className="pf-porta__folha">
        <div className="pf-porta__corpo">
          <h1 className="pf-titulo pf-porta__titulo">{t("entrar.titulo")}</h1>
          <p className="pf-porta__apoio">{t("entrar.subtitulo")}</p>

          {(encerramentoAnterior === "expirada" || encerramentoAnterior === "invalidada") && (
            <p role="status" className="pf-porta__nota">
              {t(encerramentoAnterior === "expirada" ? "entrar.sessaoExpirada" : "entrar.sessaoInvalidada")}
            </p>
          )}

          <form id="pf-porta-form" className="pf-form" onSubmit={enviar} noValidate>
            <div className="pf-campo">
              <label htmlFor="email">{t("entrar.email")}</label>
              <input
                id="email"
                name="email"
                type="email"
                inputMode="email"
                spellCheck={false}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                aria-invalid={erros.email !== undefined || undefined}
                aria-describedby={erros.email !== undefined ? "email-erro" : undefined}
              />
              {erros.email !== undefined && (
                <p id="email-erro" className="pf-campo__erro">
                  {t(erros.email)}
                </p>
              )}
            </div>

            <div className="pf-campo">
              <div className="pf-campo__cabeca">
                <label htmlFor="senha">{t("entrar.senha")}</label>
                {/* A saída do erro de senha fica ali mesmo, sem trocar de tela. */}
                <button
                  type="button"
                  className="pf-campo__link"
                  disabled={novaSenha.isPending || novaSenha.isSuccess}
                  onClick={pedirNovaSenha}
                >
                  {t(novaSenha.isPending ? "entrar.esqueci.enviando" : "entrar.esqueci")}
                </button>
              </div>
              <div className="pf-campo__senha">
                <input
                  id="senha"
                  name="senha"
                  type={mostrarSenha ? "text" : "password"}
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  autoComplete="current-password"
                  aria-invalid={erros.senha !== undefined || undefined}
                  aria-describedby={erros.senha !== undefined ? "senha-erro" : undefined}
                />
                <button
                  type="button"
                  className="pf-campo__mostrar"
                  aria-pressed={mostrarSenha}
                  onClick={() => setMostrarSenha((atual) => !atual)}
                >
                  {t(mostrarSenha ? "entrar.ocultar" : "entrar.mostrar")}
                </button>
              </div>
              {erros.senha !== undefined && (
                <p id="senha-erro" className="pf-campo__erro">
                  {t(erros.senha)}
                </p>
              )}
              {novaSenha.isSuccess && (
                <p role="status" className="pf-porta__nota">
                  {t("entrar.esqueci.enviado", { email: email.trim() })}
                </p>
              )}
            </div>

            {/* Marcada por padrão; desmarcar é a saída de quem está num computador emprestado. */}
            <div className="pf-aceite">
              <span className="pf-aceite__caixa">
                <input
                  id="manter-conectado"
                  type="checkbox"
                  checked={manterConectado}
                  onChange={(e) => setManterConectado(e.target.checked)}
                  aria-describedby="manter-conectado-dica"
                />
                <span className="pf-aceite__marca" aria-hidden="true" />
              </span>
              <div>
                <label htmlFor="manter-conectado" className="pf-aceite__rotulo">
                  {t("entrar.continuarConectado")}
                </label>
                <p id="manter-conectado-dica" className="pf-aceite__dica">
                  {t(
                    manterConectado
                      ? "entrar.continuarConectado.marcado"
                      : "entrar.continuarConectado.desmarcado",
                  )}
                </p>
              </div>
            </div>

            {/* O 401 é sobre o par, não sobre um campo, e não diz se o e-mail existe. */}
            <div aria-live="polite">
              {entrar.isError && (
                <p role="alert" className="pf-porta__erro">
                  {t(chaveDoErro(entrar.error))}
                </p>
              )}
            </div>

            <button
              type="submit"
              className="pf-botao pf-porta__acao"
              disabled={entrar.isPending}
              aria-busy={entrar.isPending || undefined}
            >
              {t(entrar.isPending ? "entrar.enviando" : "entrar.acao")}
            </button>
          </form>

          {/* O convite atravessa junto: o cadastro sabe usá-lo. */}
          <p className="pf-porta__ja-tem">
            {intl.formatMessage(
              { id: "entrar.criarConta" },
              {
                acao: (
                  <Link to="/criar-conta" search={convite === undefined ? {} : { convite }}>
                    {t("entrar.criarConta.acao")}
                  </Link>
                ),
              },
            )}
          </p>
        </div>
      </main>

      <aside className="pf-porta__painel pf-porta__painel--agora" aria-labelledby="pf-agora-titulo">
        <Plaquinha
          className="pf-plaquinha--pequena"
          frente={
            <>
              <span className="pf-plaquinha__aviso">{t("entrar.medalha.antes")}</span>
              <span className="pf-plaquinha__nome">{t("entrar.medalha.destaque")}</span>
              <span className="pf-plaquinha__aviso">{t("entrar.medalha.depois")}</span>
            </>
          }
        />
        <div className="pf-agora">
          <h2 id="pf-agora-titulo" className="pf-agora__titulo">
            {t("entrar.agora.rotulo")}
          </h2>
          <p>{t("entrar.agora.texto")}</p>
          <AbrirCartao />
          <Link to="/encontrado" className="pf-agora__achei">
            {t("entrar.achouUmAnimal")}
          </Link>
        </div>
      </aside>
    </div>
  );
}

/**
 * O cartão de emergência, sem senha. Pede o código porque não há URL pública configurada: o que
 * chega a quem precisa é o código, não um link.
 */
function AbrirCartao() {
  const intl = useIntl();
  const navegar = useNavigate();
  const [codigo, setCodigo] = useState("");
  const limpo = codigo.trim();

  return (
    <form
      className="pf-agora__cartao"
      onSubmit={(evento) => {
        evento.preventDefault();
        if (limpo !== "") {
          void navegar({ to: "/cartao/$token", params: { token: limpo } });
        }
      }}
    >
      <label htmlFor="codigo-do-cartao">{intl.formatMessage({ id: "entrar.cartao.codigo" })}</label>
      <div className="pf-agora__linha">
        <input
          id="codigo-do-cartao"
          type="text"
          autoComplete="off"
          spellCheck={false}
          value={codigo}
          onChange={(evento) => setCodigo(evento.target.value)}
          aria-describedby="codigo-do-cartao-ajuda"
        />
        <button type="submit" className="pf-agora__abrir" disabled={limpo === ""}>
          {intl.formatMessage({ id: "entrar.cartao.abrir" })}
        </button>
      </div>
      <p id="codigo-do-cartao-ajuda" className="pf-agora__ajuda">
        {intl.formatMessage({ id: "entrar.cartao.ajuda" })}
      </p>
    </form>
  );
}

import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";
import { useIntl } from "react-intl";

import { Plaquinha } from "../componentes/novo/Plaquinha.tsx";
import { useCriarConta } from "../dados/autenticacao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";
import "../estilos/novo/tokens.css";
import "../estilos/novo/base.css";
import "../estilos/novo/porta.css";

/**
 * A porta de quem ainda não tem conta. Uma conta serve para tudo (PRODUTO.md 9.3): não há
 * escolha de perfil aqui. A medalha ao lado é o verso da plaquinha da coleira, gravado
 * enquanto a pessoa digita; a frente leva o nome do animal que ela digitou na `/conheca`.
 *
 * O convite não tem prévia: a rota de prévia exige sessão (docs/reconstrucao-do-front.md).
 */
export const Route = createFileRoute("/criar-conta")({
  validateSearch: (search: Record<string, unknown>): { convite?: string; animal?: string } => ({
    ...(typeof search.convite === "string" && search.convite !== ""
      ? { convite: search.convite }
      : {}),
    ...(typeof search.animal === "string" && search.animal.trim() !== ""
      ? { animal: search.animal.trim().slice(0, 24) }
      : {}),
  }),
  component: CriarConta,
});

/** Mais rigoroso que o contrato (8) de propósito; alinhar o servidor é mudança de contrato. */
const MINIMO_DA_SENHA = 10;

type Campo = "nome" | "email" | "senha" | "aceite";

function CriarConta() {
  const intl = useIntl();
  const t = (id: string, valores?: Record<string, string>) => intl.formatMessage({ id }, valores);
  const navegar = useNavigate();
  const { convite, animal } = Route.useSearch();
  const criar = useCriarConta();

  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [aceitou, setAceitou] = useState(false);
  const [crmv, setCrmv] = useState("");
  const [crmvUf, setCrmvUf] = useState("");
  const [tocado, setTocado] = useState(false);
  const [face, setFace] = useState<"frente" | "verso">("frente");
  const gravarVerso = () => setFace("verso");

  const faltando: Campo[] = [
    ...(nome.trim() === "" ? (["nome"] as const) : []),
    ...(email.trim() === "" ? (["email"] as const) : []),
    ...(senha.length < MINIMO_DA_SENHA ? (["senha"] as const) : []),
    ...(!aceitou ? (["aceite"] as const) : []),
  ];
  const invalido = (campo: Campo) => tocado && faltando.includes(campo);
  const faltamNaSenha = MINIMO_DA_SENHA - senha.length;

  const enviar = async (evento: FormEvent) => {
    evento.preventDefault();
    setTocado(true);

    const primeiro = faltando[0];
    if (primeiro !== undefined) {
      document.getElementById(`criar-${primeiro}`)?.focus();
      return;
    }

    await criar.mutateAsync({
      nome: nome.trim(),
      email: email.trim(),
      senha,
      crmv: crmv.trim() === "" ? undefined : crmv.trim(),
      crmvUf: crmv.trim() === "" ? undefined : crmvUf.trim().toUpperCase(),
      convite,
    });

    // Quem já disse qual animal quer registrar vai direto para o cadastro dele.
    if (animal !== undefined) {
      await navegar({ to: "/animais/novo", search: { nome: animal } });
    } else {
      await navegar({ to: "/comecar" });
    }
  };

  return (
    <div className="pf pf-pagina pf-porta">
      <a className="pf-pular" href="#pf-porta-form">
        {t("criarConta.pular")}
      </a>

      <header className="pf-porta__topo">
        <Link to="/conheca" className="pf-porta__marca" translate="no">
          Petfy
        </Link>
      </header>

      <aside className="pf-porta__painel" aria-label={t("criarConta.medalha.rotulo")}>
        <PlaquinhaDoCadastro animal={animal} nome={nome.trim()} email={email.trim()} face={face} />
        <button
          type="button"
          className="pf-porta__virar"
          onClick={() => setFace((antes) => (antes === "frente" ? "verso" : "frente"))}
        >
          <span className="pf-porta__virar-seta" aria-hidden="true">
            ↻
          </span>
          {t(face === "frente" ? "criarConta.medalha.verVerso" : "criarConta.medalha.verFrente")}
        </button>
        <p className="pf-porta__legenda">{t("criarConta.medalha.legenda")}</p>
      </aside>

      <main className="pf-porta__folha">
        <div className="pf-porta__corpo">
          <h1 className="pf-titulo pf-porta__titulo">
            {animal === undefined
              ? t("criarConta.titulo")
              : t("criarConta.titulo.comAnimal", { animal })}
          </h1>
          <p className="pf-porta__apoio">{t("criarConta.apoio")}</p>

          {convite !== undefined && (
            <div role="status" className="pf-porta__convite">
              <p className="pf-porta__convite-titulo">{t("criarConta.convite.titulo")}</p>
              <p>{t("criarConta.convite.texto")}</p>
            </div>
          )}

          <form id="pf-porta-form" className="pf-form" onSubmit={(e) => void enviar(e)} noValidate>
            <div className="pf-campo">
              <label htmlFor="criar-nome">{t("criarConta.nome")}</label>
              <input
                id="criar-nome"
                name="name"
                type="text"
                value={nome}
                onChange={(e) => setNome(e.target.value)}
                onFocus={gravarVerso}
                autoComplete="name"
                aria-invalid={invalido("nome") || undefined}
              />
            </div>

            <div className="pf-campo">
              <label htmlFor="criar-email">{t("criarConta.email")}</label>
              <input
                id="criar-email"
                name="email"
                type="email"
                inputMode="email"
                spellCheck={false}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onFocus={gravarVerso}
                autoComplete="email"
                aria-invalid={invalido("email") || undefined}
              />
            </div>

            <div className="pf-campo">
              <label htmlFor="criar-senha">{t("criarConta.senha")}</label>
              <div className="pf-campo__senha">
                <input
                  id="criar-senha"
                  name="new-password"
                  type={mostrarSenha ? "text" : "password"}
                  value={senha}
                  onChange={(e) => setSenha(e.target.value)}
                  autoComplete="new-password"
                  aria-describedby="criar-senha-minimo"
                  aria-invalid={invalido("senha") || undefined}
                />
                <button
                  type="button"
                  className="pf-campo__mostrar"
                  aria-pressed={mostrarSenha}
                  onClick={() => setMostrarSenha((antes) => !antes)}
                >
                  {t(mostrarSenha ? "criarConta.senha.esconder" : "criarConta.senha.mostrar")}
                </button>
              </div>
              <p
                id="criar-senha-minimo"
                className="pf-campo__dica"
                data-falta={senha.length > 0 && faltamNaSenha > 0 ? "" : undefined}
              >
                {senha.length > 0 && faltamNaSenha > 0
                  ? intl.formatMessage({ id: "criarConta.senha.faltam" }, { n: faltamNaSenha })
                  : intl.formatMessage({ id: "criarConta.senha.minimo" }, { minimo: MINIMO_DA_SENHA })}
              </p>
            </div>

            <div className="pf-aceite" data-invalido={invalido("aceite") ? "" : undefined}>
              <span className="pf-aceite__caixa">
                <input
                  id="criar-aceite"
                  type="checkbox"
                  checked={aceitou}
                  onChange={(e) => setAceitou(e.target.checked)}
                  aria-invalid={invalido("aceite") || undefined}
                />
                <span className="pf-aceite__marca" aria-hidden="true" />
              </span>
              <label htmlFor="criar-aceite">
                {intl.formatMessage(
                  { id: "criarConta.termos" },
                  {
                    termos: (
                      <Link to="/termos" target="_blank">
                        {t("criarConta.termos.oQue")}
                      </Link>
                    ),
                    privacidade: (
                      <Link to="/privacidade" target="_blank">
                        {t("criarConta.privacidade.oQue")}
                      </Link>
                    ),
                  },
                )}
              </label>
            </div>

            <details className="pf-credencial">
              <summary>
                <span className="pf-credencial__mais" aria-hidden="true">
                  +
                </span>
                {t("criarConta.credencial.abrir")}
              </summary>
              <div className="pf-credencial__corpo">
                <div className="pf-credencial__linha">
                  <div className="pf-campo">
                    <label htmlFor="criar-crmv">{t("criarConta.credencial.registro")}</label>
                    <input
                      id="criar-crmv"
                      type="text"
                      inputMode="numeric"
                      autoComplete="off"
                      value={crmv}
                      onChange={(e) => setCrmv(e.target.value)}
                      placeholder="41.882…"
                    />
                  </div>
                  <div className="pf-campo">
                    <label htmlFor="criar-crmv-uf">{t("criarConta.credencial.uf")}</label>
                    <input
                      id="criar-crmv-uf"
                      type="text"
                      autoComplete="off"
                      value={crmvUf}
                      onChange={(e) => setCrmvUf(e.target.value.slice(0, 2).toUpperCase())}
                      maxLength={2}
                      placeholder="SP"
                    />
                  </div>
                </div>
                <p>{t("criarConta.credencial.informado")}</p>
                <p>{t("criarConta.credencial.semClinica")}</p>
              </div>
            </details>

            <div aria-live="polite">
              {criar.isError && (
                <p role="alert" className="pf-porta__erro">
                  {t(chaveDoErro(criar.error))}
                </p>
              )}
              {tocado && faltando.length > 0 && !criar.isError && (
                <p role="alert" className="pf-porta__erro">
                  {t(
                    faltando.length === 1 && faltando[0] === "aceite"
                      ? "criarConta.faltaAceite"
                      : "criarConta.faltaCampo",
                  )}
                </p>
              )}
            </div>

            <button
              type="submit"
              className="pf-botao pf-porta__acao"
              disabled={criar.isPending}
              aria-busy={criar.isPending || undefined}
            >
              {t(
                criar.isPending
                  ? "criarConta.acao.criando"
                  : convite === undefined
                    ? "criarConta.acao"
                    : "criarConta.acao.comConvite",
              )}
            </button>

            {animal !== undefined && (
              <p className="pf-porta__seguinte">{t("criarConta.seguinte", { animal })}</p>
            )}
          </form>

          {/* O convite atravessa junto: quem descobre aqui que já tinha conta não o perde. */}
          <p className="pf-porta__ja-tem">
            {t("criarConta.jaTem")}{" "}
            <Link to="/entrar" search={convite === undefined ? {} : { convite }}>
              {t("criarConta.entrar")}
            </Link>
          </p>
        </div>
      </main>
    </div>
  );
}

/** As duas faces do cadastro: o nome do animal na frente, como achar quem cuida no verso. */
function PlaquinhaDoCadastro({
  animal,
  nome,
  email,
  face,
}: {
  animal?: string;
  nome: string;
  email: string;
  face: "frente" | "verso";
}) {
  const intl = useIntl();
  const t = (id: string) => intl.formatMessage({ id });
  const nomeDoAnimal = animal ?? t("criarConta.medalha.semAnimal");
  const primeiroNome = nome.split(" ")[0] ?? "";
  const [local, ...dominio] = email.split("@");

  return (
    <Plaquinha
      face={face}
      frente={
        <>
          <span
            className="pf-plaquinha__animal"
            data-longo={nomeDoAnimal.length > 9 ? "" : undefined}
            data-vazio={animal === undefined ? "" : undefined}
            translate="no"
          >
            {nomeDoAnimal}
          </span>
          <span className="pf-plaquinha__marca" translate="no">
            Petfy
          </span>
        </>
      }
      verso={
        <>
          <span className="pf-plaquinha__aviso">{t("criarConta.medalha.seMePerder")}</span>
          <span className="pf-plaquinha__nome" data-vazio={nome === "" ? "" : undefined} translate="no">
            {primeiroNome === "" ? t("criarConta.medalha.semNome") : primeiroNome}
          </span>
          <span
            className="pf-plaquinha__email"
            data-vazio={email === "" ? "" : undefined}
            data-longo={email.length > 22 ? "" : undefined}
            translate="no"
          >
            {email === "" ? (
              t("criarConta.medalha.semEmail")
            ) : (
              <>
                {local}
                {dominio.length > 0 && (
                  <>
                    <wbr />@{dominio.join("@")}
                  </>
                )}
              </>
            )}
          </span>
        </>
      }
    />
  );
}

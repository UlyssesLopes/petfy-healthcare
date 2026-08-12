import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useConta, useDeclararCredencial, useEncerrarConta } from "../dados/conta.ts";
import { encerrarSessao, lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 36 · rotina — Sua conta", de
 * `design/IdentidadeVisual/Telas Petfy - Fim, reencontro e conta.dc.html`.
 *
 * <b>Uma tela de conta que diz o que cada coisa SIGNIFICA</b>, e nao uma lista de campos: "nome e
 * telefone — aparecem para quem cuida dos seus animais, e no cartao de emergencia"; "registro
 * profissional — sem ele, voce nao registra diagnostico nem prescricao". Cada linha explica a
 * consequencia antes de oferecer o botao.
 *
 * ------------------------------------------------------------ a regra que esta tela trouxe
 *
 * <b>"O Code e o Bartolomeu precisam de alguem antes que voce saia."</b> Ate aqui, encerrar a conta
 * resolvia sozinho: o animal sem outro tutor MORRIA com ela, e o que tinha co-tutor passava para o
 * mais antigo deles — os dois em silencio, sem ninguem escolher. O primeiro destroi anos de registro
 * de um animal vivo; o segundo entrega a responsabilidade a quem nunca disse sim.
 *
 * Agora o produto recusa e devolve a lista, e a tela oferece o caminho de cada um.
 *
 * ------------------------------------------------------------ o que esta tela NAO mostra
 *
 * <b>"Aparelhos conectados · 3 sessoes abertas."</b> Este produto autentica com JWT sem estado: o
 * servidor nao sabe quantos tokens validos existem, e nao teria como invalidar um deles. Entregar a
 * linha exigiria persistir sessao, emitir refresh token e mexer no filtro de autenticacao inteiro — e
 * um numero estimado seria PIOR que a ausencia, porque a pessoa clicaria em "encerrar" acreditando ter
 * encerrado. <b>A tela diz que nao sabe.</b>
 */

export const Route = createFileRoute("/conta")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Conta,
});

function Conta() {
  const intl = useIntl();
  const navegar = useNavigate();

  const conta = useConta();
  const declarar = useDeclararCredencial();
  const encerrar = useEncerrarConta();

  const [declarando, setDeclarando] = useState(false);
  const [crmv, setCrmv] = useState("");
  const [uf, setUf] = useState("");
  const [especialidade, setEspecialidade] = useState("");
  const [confirmandoEncerramento, setConfirmandoEncerramento] = useState(false);

  if (conta.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "conta.oQueE" })} />;
  }

  if (conta.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "conta.oQueE" })}
        erro={conta.error}
        aoTentarDeNovo={() => void conta.refetch()}
        carregando={conta.isFetching}
      />
    );
  }

  const dados = conta.data;
  const pendentes = dados?.animalsUnderMyResponsibility ?? [];
  const podeEncerrar = dados?.canDeleteAccount === true;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "640px", margin: "0 auto" }}>
        <h1 style={titulo}>{intl.formatMessage({ id: "conta.titulo" })}</h1>

        <p style={{ fontSize: "15px", color: CINZA, margin: "0 0 26px" }}>
          {dados?.name} · {dados?.email}
        </p>

        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
          <Linha
            titulo={intl.formatMessage({ id: "conta.nomeETelefone" })}
            explicacao={intl.formatMessage({ id: "conta.nomeETelefone.nota" })}
            acao={
              <Link to="/" style={acaoComoLink}>
                {intl.formatMessage({ id: "conta.alterar" })}
              </Link>
            }
          />

          <Linha
            titulo={intl.formatMessage({ id: "conta.senha" })}
            explicacao={
              dados?.passwordChangedAt === null || dados?.passwordChangedAt === undefined
                ? /* Nulo significa "nunca desde que a conta existe", e a tela diz isso em vez de
                     mostrar a data do cadastro como se fosse troca. */
                  intl.formatMessage({ id: "conta.senha.nuncaTrocada" })
                : intl.formatMessage(
                    { id: "conta.senha.alteradaEm" },
                    { quando: intl.formatDate(dados.passwordChangedAt) },
                  )
            }
            acao={
              <Link to="/" style={acaoComoLink}>
                {intl.formatMessage({ id: "conta.alterar" })}
              </Link>
            }
          />

          <Linha
            titulo={intl.formatMessage({ id: "conta.registroProfissional" })}
            explicacao={
              dados?.professionalCredential === null || dados?.professionalCredential === undefined
                ? intl.formatMessage({ id: "conta.registro.semNenhum" })
                : dados.professionalCredential
            }
            acao={
              dados?.professionalCredential === null ||
              dados?.professionalCredential === undefined ? (
                <button type="button" onClick={() => setDeclarando(!declarando)} style={botaoSecundario}>
                  {intl.formatMessage({ id: "conta.declarar" })}
                </button>
              ) : null
            }
          />

          {declarando && (
            <div style={{ padding: "18px 22px", borderBottom: "1px solid oklch(0.94 0.006 150)", background: "oklch(0.985 0.004 150)" }}>
              <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginBottom: "10px" }}>
                <input
                  value={crmv}
                  onChange={(evento) => setCrmv(evento.target.value)}
                  placeholder={intl.formatMessage({ id: "conta.registro.numero" })}
                  aria-label={intl.formatMessage({ id: "conta.registro.numero" })}
                  style={{ ...campo, flex: "2 1 160px" }}
                />
                <input
                  value={uf}
                  maxLength={2}
                  onChange={(evento) => setUf(evento.target.value)}
                  placeholder={intl.formatMessage({ id: "conta.registro.uf" })}
                  aria-label={intl.formatMessage({ id: "conta.registro.uf" })}
                  style={{ ...campo, flex: "0 1 80px" }}
                />
                <input
                  value={especialidade}
                  onChange={(evento) => setEspecialidade(evento.target.value)}
                  placeholder={intl.formatMessage({ id: "conta.registro.especialidade" })}
                  aria-label={intl.formatMessage({ id: "conta.registro.especialidade" })}
                  style={{ ...campo, flex: "2 1 160px" }}
                />
              </div>

              {declarar.error !== null && declarar.error !== undefined && (
                <div style={{ marginBottom: "10px" }}>
                  <ErroAoGravar erro={declarar.error} oQue={intl.formatMessage({ id: "conta.oQueE" })} />
                </div>
              )}

              <button
                type="button"
                disabled={crmv.trim() === "" || uf.trim().length !== 2 || declarar.isPending}
                onClick={() =>
                  declarar.mutate(
                    {
                      crmv: crmv.trim(),
                      uf: uf.trim(),
                      especialidade: especialidade.trim() === "" ? undefined : especialidade.trim(),
                    },
                    { onSuccess: () => setDeclarando(false) },
                  )
                }
                style={botaoPrincipal}
              >
                {intl.formatMessage({ id: "conta.registro.salvar" })}
              </button>

              <div style={nota}>{intl.formatMessage({ id: "conta.registro.informado" })}</div>
            </div>
          )}

          {/*
           * "Aparelhos conectados" não existe, e a tela diz por quê em vez de mostrar um número
           * inventado — clicar em "encerrar" acreditando ter encerrado seria pior que a ausência.
           */}
          <Linha
            titulo={intl.formatMessage({ id: "conta.aparelhos" })}
            explicacao={intl.formatMessage({ id: "conta.aparelhos.naoSabemos" })}
            acao={null}
          />

          <Linha
            titulo={intl.formatMessage({ id: "conta.levarDados" })}
            explicacao={intl.formatMessage({ id: "conta.levarDados.nota" })}
            acao={
              <a
                href="/persons/me/export"
                style={acaoComoLink}
                onClick={(evento) => {
                  // a exportacao exige o token no cabecalho, e um href simples nao o leva
                  evento.preventDefault();
                  window.open("/persons/me/export", "_blank", "noopener");
                }}
              >
                {intl.formatMessage({ id: "conta.baixar" })}
              </a>
            }
          />
        </div>

        {/* ------------------------------------------------------------ encerrar a conta */}
        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "22px 24px", marginTop: "26px" }}>
          <h2 style={{ ...titulo, fontSize: "20px", margin: "0 0 12px" }}>
            {intl.formatMessage({ id: "conta.encerrar" })}
          </h2>

          {pendentes.length > 0 ? (
            <>
              <p style={{ fontSize: "15px", lineHeight: 1.65, margin: "0 0 14px" }}>
                {intl.formatMessage(
                  { id: "conta.encerrar.precisamDeAlguem" },
                  {
                    animais: pendentes.map((a) => a.name).join(", "),
                    quantos: pendentes.length,
                  },
                )}
              </p>

              <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                {pendentes.map((animal) => (
                  <Link
                    key={animal.animalId}
                    to="/animais/$animalId/transferir"
                    params={{ animalId: animal.animalId ?? "" }}
                    style={{ ...acaoComoLink, fontSize: "15px" }}
                  >
                    {intl.formatMessage({ id: "conta.encerrar.transferir" }, { animal: animal.name })}
                  </Link>
                ))}
              </div>
            </>
          ) : (
            <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 14px" }}>
              {intl.formatMessage({ id: "conta.encerrar.nadaPendente" })}
            </p>
          )}

          {encerrar.error !== null && encerrar.error !== undefined && (
            <div style={{ marginTop: "14px" }}>
              <ErroAoGravar erro={encerrar.error} oQue={intl.formatMessage({ id: "conta.oQueE" })} />
            </div>
          )}

          <button
            type="button"
            disabled={!podeEncerrar || encerrar.isPending}
            onClick={() => {
              if (!confirmandoEncerramento) {
                setConfirmandoEncerramento(true);
                return;
              }

              encerrar.mutate(undefined, {
                onSuccess: () => {
                  // "saida", e nao "invalidada": a conta acabou de deixar de existir por decisao da
                  // propria pessoa, e a tela de entrar nao deve sugerir que algo deu errado
                  encerrarSessao("saida");
                  void navegar({ to: "/" });
                },
              });
            }}
            style={{
              ...botaoPrincipal,
              marginTop: "16px",
              background: podeEncerrar ? "oklch(0.55 0.14 30)" : "oklch(0.72 0.02 150)",
              cursor: podeEncerrar ? "pointer" : "not-allowed",
            }}
          >
            {intl.formatMessage({
              id: confirmandoEncerramento ? "conta.encerrar.confirmar" : "conta.encerrar.botao",
            })}
          </button>

          {/* O desabilitado nunca aparece mudo: a frase diz o que falta. */}
          {!podeEncerrar && (
            <div style={nota}>{intl.formatMessage({ id: "conta.encerrar.disponivelQuando" })}</div>
          )}
        </div>
      </div>
    </div>
  );
}

function Linha({
  titulo: rotulo,
  explicacao,
  acao,
}: {
  titulo: string;
  explicacao: string;
  acao: React.ReactNode;
}) {
  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px", alignItems: "center", padding: "18px 22px", borderBottom: "1px solid oklch(0.94 0.006 150)" }}>
      <div>
        <div style={{ fontSize: "16px", fontWeight: 500 }}>{rotulo}</div>
        <div style={{ fontSize: "14px", color: CINZA, marginTop: "3px", lineHeight: 1.5 }}>
          {explicacao}
        </div>
      </div>
      <div>{acao}</div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "30px",
  fontWeight: 500,
  margin: "0 0 6px",
  letterSpacing: "-0.02em",
} as const;

const nota = {
  fontSize: "13px",
  color: CINZA,
  marginTop: "10px",
  lineHeight: 1.55,
} as const;

const campo = {
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "12px 14px",
  fontSize: "16px",
  minHeight: "48px",
  background: "oklch(1 0 0)",
  fontFamily: "inherit",
} as const;

const botaoPrincipal = {
  fontFamily: "inherit",
  fontSize: "15px",
  fontWeight: 500,
  color: "oklch(1 0 0)",
  background: VERDE,
  border: "none",
  borderRadius: "8px",
  padding: "12px 20px",
  minHeight: "48px",
  cursor: "pointer",
} as const;

const botaoSecundario = {
  fontFamily: "inherit",
  fontSize: "15px",
  fontWeight: 500,
  color: "oklch(0.25 0.02 150)",
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "8px",
  padding: "10px 16px",
  minHeight: "44px",
  cursor: "pointer",
} as const;

const acaoComoLink = {
  color: VERDE,
  fontSize: "15px",
  fontWeight: 500,
} as const;

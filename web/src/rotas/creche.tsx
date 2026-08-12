import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useDiaDaTurma,
  useMarcarEntrada,
  useMarcarFalta,
  useMarcarSaida,
  useTurmas,
  type Presenca,
} from "../dados/creche.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 17 · a tela mais usada da creche — Segunda-feira, 7h30, a operacao do dia", de
 * `design/IdentidadeVisual/Telas Petfy - Organizacao.dc.html`.
 *
 * <b>Ela e a unica tela do produto em que a pressa e parte do contexto.</b> Sao 7h34, ha catorze
 * animais esperados e seis chegaram, e quem opera tem um cachorro em cada mao. Por isso a linha e
 * de 56 px com um gesto so por animal, e por isso o topo conta em vez de listar.
 *
 * <b>A RECUSA NAO E DESTA TELA, e isso e o mais importante daqui.</b> "V10 venceu ontem — nao pode
 * entrar. A turma inteira depende disso": o servidor recusa a entrada, e a tela mostra o motivo. Uma
 * tela que apenas avisasse deixaria a decisao para quem esta com quinze cachorros na porta — e a lei
 * nao cobra a tela.
 *
 * <b>ESPERADO nao e um registro.</b> Quem tem matricula ativa e nao tem nada gravado do dia nasce
 * esperado, derivado da matricula. E o que faz a contagem do topo existir as 7h34, antes de ninguem
 * ter tocado em nada.
 */

export const Route = createFileRoute("/creche")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Creche,
});

function Creche() {
  const intl = useIntl();
  const turmas = useTurmas();

  const [turmaEscolhida, setTurmaEscolhida] = useState<string | undefined>(undefined);
  const lista = turmas.data ?? [];
  const turmaAtual = turmaEscolhida ?? lista[0]?.classGroupId;
  const dia = useDiaDaTurma(turmaAtual);

  const doDia = dia.data ?? [];
  const esperados = doDia.filter((linha) => linha.status !== "FALTA").length;
  const chegaram = doDia.filter(
    (linha) => linha.status === "PRESENTE" || linha.status === "SAIU",
  ).length;


  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>

        <div style={{ padding: "28px 32px 36px" }}>
          {/* O topo CONTA, e nao lista: as 7h34 o numero e a informacao. */}
          <div style={{ display: "flex", alignItems: "baseline", justifyContent: "space-between", gap: "16px", marginBottom: "18px", flexWrap: "wrap" }}>
            <div>
              <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, margin: "0 0 4px" }}>
                {intl.formatDate(new Date(), { weekday: "long", day: "2-digit", month: "long" })}
              </h1>
              <div style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)" }}>
                {intl.formatMessage({ id: "creche.contagem" }, { esperados, chegaram })}
              </div>
            </div>

            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatTime(new Date(), { hour: "2-digit", minute: "2-digit" })}
            </div>
          </div>

          {/* As turmas como abas — "Turma Tarde", "Turma Manha", "Hospedagem". */}
          {turmas.isError ? (
            <ErroDeCarga
              oQue={intl.formatMessage({ id: "creche.oQue.turmas" })}
              erro={turmas.error}
              aoTentarDeNovo={() => void turmas.refetch()}
              carregando={turmas.isFetching}
            />
          ) : turmas.isPending ? (
            <Carregando oQue={intl.formatMessage({ id: "creche.oQue.turmas" })} />
          ) : lista.length === 0 ? (
            <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "creche.semTurma" })}
            </div>
          ) : (
            <>
              <div style={{ display: "flex", gap: "8px", marginBottom: "18px", flexWrap: "wrap" }}>
                {lista.map((turma) => (
                  <button
                    key={turma.classGroupId}
                    type="button"
                    aria-pressed={turma.classGroupId === turmaAtual}
                    onClick={() => setTurmaEscolhida(turma.classGroupId)}
                    style={{ fontFamily: "inherit", fontSize: "14px", padding: "8px 14px", minHeight: "40px", display: "flex", alignItems: "center", border: `1px solid ${turma.classGroupId === turmaAtual ? "oklch(0.46 0.085 150)" : "oklch(0.84 0.012 150)"}`, color: turma.classGroupId === turmaAtual ? "oklch(0.46 0.085 150)" : "oklch(0.42 0.015 150)", background: "oklch(1 0 0)", borderRadius: "8px", cursor: "pointer" }}
                  >
                    {turma.capacity === undefined
                      ? intl.formatMessage({ id: "creche.turma.semLimite" }, { nome: turma.name, ocupadas: turma.occupied ?? 0 })
                      : intl.formatMessage(
                          { id: "creche.turma.comLimite" },
                          { nome: turma.name, ocupadas: turma.occupied ?? 0, vagas: turma.capacity },
                        )}
                  </button>
                ))}
              </div>

              {dia.isError ? (
                <ErroDeCarga
                  oQue={intl.formatMessage({ id: "creche.oQue.dia" })}
                  erro={dia.error}
                  aoTentarDeNovo={() => void dia.refetch()}
                  carregando={dia.isFetching}
                />
              ) : dia.isPending ? (
                <Carregando oQue={intl.formatMessage({ id: "creche.oQue.dia" })} quantos={doDia.length} />
              ) : doDia.length === 0 ? (
                <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                  {intl.formatMessage({ id: "creche.turmaVazia" })}
                </div>
              ) : (
                <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1.1fr 1.6fr auto auto", gap: "16px", padding: "12px 22px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)" }}>
                    <div>{intl.formatMessage({ id: "creche.coluna.animal" })}</div>
                    <div>{intl.formatMessage({ id: "creche.coluna.hojePrecisa" })}</div>
                    <div>{intl.formatMessage({ id: "creche.coluna.entrada" })}</div>
                    <div></div>
                  </div>

                  {doDia.map((linha) => (
                    <LinhaDoDia key={linha.enrollmentId} linha={linha} turma={turmaAtual} />
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/** Linha de 56 px, um gesto por animal. A pressa e parte do contexto de uso. */
function LinhaDoDia({ linha, turma }: { linha: Presenca; turma: string | undefined }) {
  const intl = useIntl();
  const entrada = useMarcarEntrada();
  const saida = useMarcarSaida();
  const falta = useMarcarFalta();

  const pendente = entrada.isPending || saida.isPending || falta.isPending;
  const erro = entrada.error ?? saida.error ?? falta.error;
  const id = linha.enrollmentId;

  return (
    <div style={{ borderTop: "1px solid oklch(0.95 0.005 150)" }}>
      <div style={{ display: "grid", gridTemplateColumns: "1.1fr 1.6fr auto auto", gap: "16px", alignItems: "center", padding: "8px 22px", minHeight: "56px" }}>
        <div>
          {/*
           * O NOME E O CAMINHO PARA A MATRICULA, e nao um botao a mais.
           *
           * A Tela 17 e a unica do produto em que a pressa e parte do contexto — um gesto por
           * animal, e nada disputando com "marcar entrada". Um nome que leva ao combinado nao
           * compete com o gesto das 7h30: ninguem clica num nome por engano com um cachorro em
           * cada mao, e quem vai ajustar mensalidade nao esta na porta.
           */}
          {turma === undefined || linha.enrollmentId === undefined ? (
            <span style={{ fontSize: "16px", fontWeight: 500 }}>{linha.animalName}</span>
          ) : (
            <Link
              to="/creche/matricula/$classGroupId/$enrollmentId"
              params={{ classGroupId: turma, enrollmentId: linha.enrollmentId }}
              style={{ fontSize: "16px", fontWeight: 500, color: "oklch(0.25 0.02 150)", textDecoration: "none" }}
            >
              {linha.animalName}
            </Link>
          )}
          {linha.species !== undefined && (
            <span style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
              {" · "}
              {intl.formatMessage({ id: `animal.especie.${linha.species}` })}
            </span>
          )}
        </div>

        {/*
         * "Hoje precisa": o que a creche tem de saber sobre este animal hoje. Quando o animal esta
         * impedido, o motivo GANHA da lista — e a unica informacao que muda o que fazer agora.
         */}
        <div style={{ fontSize: "15px", lineHeight: 1.5, color: linha.blocked === true ? "oklch(0.45 0.13 30)" : "oklch(0.35 0.018 150)" }}>
          {linha.blocked === true
            ? intl.formatMessage({ id: "creche.impedido" }, { motivo: linha.blockedReason ?? "" })
            : (linha.todayNeeds ?? []).length === 0
              ? intl.formatMessage({ id: "creche.nada" })
              : (linha.todayNeeds ?? []).join(" · ")}
        </div>

        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
          {linha.checkedInAt === undefined
            ? "—"
            : intl.formatTime(new Date(linha.checkedInAt), { hour: "2-digit", minute: "2-digit" })}
        </div>

        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          {linha.status === "FALTA" ? (
            <Marca texto={intl.formatMessage({ id: "creche.estado.FALTA" })} />
          ) : linha.status === "SAIU" ? (
            <Marca
              texto={intl.formatMessage(
                { id: "creche.estado.SAIU" },
                {
                  hora:
                    linha.checkedOutAt === undefined
                      ? ""
                      : intl.formatTime(new Date(linha.checkedOutAt), { hour: "2-digit", minute: "2-digit" }),
                },
              )}
            />
          ) : linha.status === "PRESENTE" ? (
            <>
              <Marca texto={intl.formatMessage({ id: "creche.estado.PRESENTE" })} destacada />
              {id !== undefined && (
                <Acao pendente={pendente} aoClicar={() => saida.mutate(id)}>
                  {intl.formatMessage({ id: "creche.acao.saida" })}
                </Acao>
              )}
            </>
          ) : linha.blocked === true ? (
            /*
             * Nao ha "marcar entrada" para animal impedido, e nao e o botao desabilitado de sempre:
             * o gesto que resta e outro. O desenho poe "Avisar tutor" aqui — e avisar nao existe no
             * backend, entao o que fica e a frase e o caminho para a carteira, onde a dose entra.
             */
            <div style={{ fontSize: "14px", color: "oklch(0.42 0.015 150)", lineHeight: 1.5, maxWidth: "26ch" }}>
              {intl.formatMessage({ id: "creche.avisarNaoDa" })}
            </div>
          ) : (
            id !== undefined && (
              <>
                <Acao pendente={pendente} principal aoClicar={() => entrada.mutate(id)}>
                  {intl.formatMessage({ id: "creche.acao.entrada" })}
                </Acao>
                <Acao pendente={pendente} aoClicar={() => falta.mutate(id)}>
                  {intl.formatMessage({ id: "creche.acao.falta" })}
                </Acao>
              </>
            )
          )}
        </div>
      </div>

      {erro !== null && erro !== undefined && (
        <div style={{ padding: "0 22px 12px" }}>
          <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "10px 12px", margin: 0 }}>
            {intl.formatMessage({ id: chaveDoErro(erro) })}
          </p>
        </div>
      )}
    </div>
  );
}

function Marca({ texto, destacada = false }: { texto: string; destacada?: boolean }) {
  return (
    <span style={{ fontSize: "14px", fontWeight: destacada ? 500 : 400, color: destacada ? "oklch(0.38 0.07 150)" : "oklch(0.5 0.015 150)" }}>
      {texto}
    </span>
  );
}

function Acao({
  children,
  pendente,
  principal = false,
  aoClicar,
}: {
  children: ReactNode;
  pendente: boolean;
  principal?: boolean;
  aoClicar: () => void;
}) {
  return (
    <button
      type="button"
      disabled={pendente}
      onClick={aoClicar}
      style={{
        fontFamily: "inherit",
        fontSize: "14px",
        fontWeight: 500,
        borderRadius: "8px",
        padding: "11px 16px",
        minHeight: "44px",
        cursor: pendente ? "wait" : "pointer",
        ...(principal
          ? { color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none" }
          : { color: "oklch(0.42 0.015 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.84 0.012 150)" }),
      }}
    >
      {children}
    </button>
  );
}

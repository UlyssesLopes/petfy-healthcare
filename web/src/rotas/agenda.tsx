import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useAgendaDoDia,
  useEntregarAnimal,
  useMarcarEntrada,
  useMarcarFalta,
  type Agendamento,
} from "../dados/agenda.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 18 · petshop — Agenda de banho e tosa", de
 * `design/IdentidadeVisual/Telas Petfy - Petshop e convites.dc.html`.
 *
 * <b>"Se o sistema fica bom com um tosador vendo quatro linhas, o escopo funciona. E o teste mais
 * duro da tela 09."</b> E esta tela e esse teste — e o que ela mostra e que <b>nao ha nenhuma regra
 * de petshop em lugar nenhum</b>: ha um tutor que concedeu condicoes, observacoes e carteira, e nao
 * concedeu prontuario. O escopo faz o resto.
 *
 * ------------------------------------------------------------ o estado que a maioria esconderia
 *
 * <b>"Acesso ao Petfy venceu em 31/07 — voce esta no escuro."</b> Um cartao sem as quatro linhas
 * parece um animal sem restricao nenhuma, e essa e a diferenca entre tosar com cuidado e tosar as
 * cegas. A tela diz em voz alta que nao sabe.
 *
 * ------------------------------------------------------------ o que ela NAO faz
 *
 * <b>Nao tem foto.</b> O desenho mostra "foto do antes e depois", e entregar isso exigiria escopo de
 * ANEXOS — que e justamente o que o petshop nao tem e nao deveria ter. Pedir mais escopo para tirar
 * uma foto seria contrariar a frase que da titulo ao arquivo: "pedir mais que isso seria pedir o que
 * nao se usa". Fica declarado.
 */

export const Route = createFileRoute("/agenda")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Agenda,
});

function Agenda() {
  const intl = useIntl();

  const agenda = useAgendaDoDia();
  const marcarEntrada = useMarcarEntrada();
  const entregar = useEntregarAnimal();
  const faltou = useMarcarFalta();

  const [abertoId, setAbertoId] = useState<string | null>(null);
  const [nota, setNota] = useState("");

  if (agenda.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "agenda.oQueE" })} />;
  }

  if (agenda.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "agenda.oQueE" })}
        erro={agenda.error}
        aoTentarDeNovo={() => void agenda.refetch()}
        carregando={agenda.isFetching}
      />
    );
  }

  const lista = agenda.data ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "820px", margin: "0 auto" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "16px", flexWrap: "wrap", marginBottom: "24px" }}>
          <h1 style={titulo}>{intl.formatMessage({ id: "agenda.hoje" })}</h1>
          <span style={{ fontSize: "15px", color: CINZA }}>
            {intl.formatMessage({ id: "agenda.quantos" }, { quantos: lista.length })}
          </span>
        </div>

        {(marcarEntrada.error !== null && marcarEntrada.error !== undefined) && (
          <div style={{ marginBottom: "14px" }}>
            <ErroAoGravar erro={marcarEntrada.error} oQue={intl.formatMessage({ id: "agenda.oQueE" })} />
          </div>
        )}

        {(entregar.error !== null && entregar.error !== undefined) && (
          <div style={{ marginBottom: "14px" }}>
            <ErroAoGravar erro={entregar.error} oQue={intl.formatMessage({ id: "agenda.oQueE" })} />
          </div>
        )}

        {lista.length === 0 ? (
          /* O vazio dirige, e nao anuncia falha: um dia sem banho marcado e um dia normal. */
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: 0 }}>
            {intl.formatMessage({ id: "agenda.vazia" })}
          </p>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "76px 1fr", gap: 0 }}>
            {lista.map((item) => (
              <Linha
                key={item.serviceAppointmentId}
                item={item}
                aberto={abertoId === item.serviceAppointmentId}
                aoAbrir={() =>
                  setAbertoId(
                    abertoId === item.serviceAppointmentId
                      ? null
                      : (item.serviceAppointmentId ?? null),
                  )
                }
                nota={nota}
                aoMudarNota={setNota}
                aoMarcarEntrada={() => marcarEntrada.mutate(item.serviceAppointmentId ?? "")}
                aoEntregar={() => {
                  entregar.mutate(
                    {
                      appointmentId: item.serviceAppointmentId ?? "",
                      nota: nota.trim() === "" ? undefined : nota.trim(),
                    },
                    { onSuccess: () => setNota("") },
                  );
                }}
                aoMarcarFalta={() => faltou.mutate(item.serviceAppointmentId ?? "")}
                ocupado={marcarEntrada.isPending || entregar.isPending || faltou.isPending}
              />
            ))}
          </div>
        )}

        {/* "O que o Banho do Tiago nunca vê" — a tela diz o que ela não mostra, e por quê. */}
        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px", marginTop: "28px" }}>
          <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "12px" }}>
            {intl.formatMessage({ id: "agenda.oQueNuncaVe" })}
          </div>
          <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "agenda.oQueNuncaVe.lista" })}
          </div>
          <div style={{ fontSize: "14px", color: CINZA, lineHeight: 1.6, marginTop: "14px" }}>
            {intl.formatMessage({ id: "agenda.quatroLinhasBastam" })}
          </div>
        </div>
      </div>
    </div>
  );
}

function Linha({
  item,
  aberto,
  aoAbrir,
  nota,
  aoMudarNota,
  aoMarcarEntrada,
  aoEntregar,
  aoMarcarFalta,
  ocupado,
}: {
  item: Agendamento;
  aberto: boolean;
  aoAbrir: () => void;
  nota: string;
  aoMudarNota: (valor: string) => void;
  aoMarcarEntrada: () => void;
  aoEntregar: () => void;
  aoMarcarFalta: () => void;
  ocupado: boolean;
}) {
  const intl = useIntl();

  const emAtendimento = item.status === "EM_ATENDIMENTO";
  const concluido = item.status === "CONCLUIDO";

  return (
    <>
      <div style={{ padding: "4px 16px 20px 0", textAlign: "right", borderRight: "1px solid oklch(0.90 0.008 150)" }}>
        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", color: "oklch(0.42 0.015 150)" }}>
          {intl.formatDate(item.scheduledAt ?? "", { hour: "2-digit", minute: "2-digit" })}
        </div>
      </div>

      <div style={{ padding: "0 0 20px 20px" }}>
        <div
          style={{
            border: item.inTheDark === true
              ? "1px solid oklch(0.86 0.03 30)"
              : aberto
                ? "1px solid oklch(0.46 0.085 150)"
                : "1px solid oklch(0.90 0.008 150)",
            background: item.inTheDark === true
              ? "oklch(0.985 0.008 30)"
              : concluido
                ? "oklch(0.975 0.004 150)"
                : "oklch(1 0 0)",
            borderRadius: "12px",
            padding: aberto ? "20px 22px" : "16px 18px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: "14px", alignItems: "center", flexWrap: "wrap" }}>
            <div>
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: aberto ? "20px" : "16px", fontWeight: 500 }}>
                {item.animalName}
              </div>
              <div style={{ fontSize: "14px", color: CINZA, marginTop: "2px" }}>{item.service}</div>

              {/* "Acesso ao Petfy venceu — você está no escuro." */}
              {item.inTheDark === true && (
                <div style={{ fontSize: "14px", color: "oklch(0.45 0.13 30)", marginTop: "4px" }}>
                  {intl.formatMessage({ id: "agenda.noEscuro" })}
                </div>
              )}
            </div>

            <div style={{ display: "flex", gap: "9px", alignItems: "center", flexWrap: "wrap" }}>
              {concluido ? (
                <span style={{ fontSize: "14px", color: CINZA }}>
                  {intl.formatMessage(
                    { id: "agenda.entregue" },
                    { quando: intl.formatDate(item.completedAt ?? "", { hour: "2-digit", minute: "2-digit" }) },
                  )}
                </span>
              ) : item.status === "FALTOU" ? (
                <span style={{ fontSize: "14px", color: CINZA }}>
                  {intl.formatMessage({ id: "agenda.faltou" })}
                </span>
              ) : (
                <>
                  <button type="button" onClick={aoAbrir} style={botaoSecundario}>
                    {intl.formatMessage({ id: aberto ? "agenda.fechar" : "agenda.abrir" })}
                  </button>
                  {!emAtendimento && (
                    <button type="button" disabled={ocupado} onClick={aoMarcarEntrada} style={botaoPrincipal}>
                      {intl.formatMessage({ id: "agenda.marcarEntrada" })}
                    </button>
                  )}
                </>
              )}
            </div>
          </div>

          {aberto && (
            <div style={{ borderTop: "1px solid oklch(0.94 0.006 150)", paddingTop: "16px", marginTop: "16px" }}>
              <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "12px" }}>
                {intl.formatMessage({ id: "agenda.oQuePrecisaSaber" })}
              </div>

              {(item.safetyNotes ?? []).length === 0 ? (
                <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.45 0.13 30)" }}>
                  {intl.formatMessage({ id: "agenda.semLinhas" })}
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "10px", fontSize: "16px" }}>
                  {(item.safetyNotes ?? []).map((linha, indice) => (
                    <div key={`${linha.text}-${indice}`} style={{ display: "flex", alignItems: "flex-start", gap: "11px" }}>
                      {/*
                       * O losango vermelho e MANEJO: "displasia no quadril, nao erguer pelas patas
                       * traseiras". E o que o tosador precisa saber antes de encostar nele.
                       */}
                      <span
                        aria-hidden
                        style={{
                          width: "12px",
                          height: "12px",
                          flex: "none",
                          marginTop: "5px",
                          ...(linha.severity === "MANEJO"
                            ? { background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)" }
                            : linha.severity === "OK"
                              ? { background: VERDE, borderRadius: "999px" }
                              : { border: "3px solid oklch(0.62 0.11 70)", borderRadius: "999px" }),
                        }}
                      ></span>
                      <span>
                        {linha.text === "VACINACAO_EM_DIA"
                          ? intl.formatMessage({ id: "agenda.vacinacaoEmDia" })
                          : linha.text === "VACINACAO_ATRASADA"
                            ? intl.formatMessage({ id: "agenda.vacinacaoAtrasada" })
                            : linha.text}
                      </span>
                    </div>
                  ))}
                </div>
              )}

              <div style={{ fontSize: "14px", color: CINZA, lineHeight: 1.55, marginTop: "14px", borderTop: "1px solid oklch(0.95 0.005 150)", paddingTop: "14px" }}>
                {intl.formatMessage({ id: "agenda.eTudoQueOTutorCompartilhou" })}
              </div>

              {emAtendimento && (
                <div style={{ marginTop: "18px" }}>
                  <label htmlFor={`nota-${item.serviceAppointmentId}`} style={rotulo}>
                    {intl.formatMessage({ id: "agenda.algoQueNotou" })}
                  </label>
                  <textarea
                    id={`nota-${item.serviceAppointmentId}`}
                    rows={3}
                    value={nota}
                    onChange={(evento) => aoMudarNota(evento.target.value)}
                    style={{ ...campo, minHeight: "76px", lineHeight: 1.5, resize: "vertical" }}
                  />
                  {/*
                   * A frase mais importante da coluna direita do desenho, e ela e uma regra do
                   * produto: observacao nunca vira ato clinico sozinha.
                   */}
                  <div style={{ fontSize: "13px", color: CINZA, marginTop: "8px", lineHeight: 1.55 }}>
                    {intl.formatMessage({ id: "agenda.descrevaOQueViu" }, { animal: item.animalName ?? "" })}
                  </div>

                  <button
                    type="button"
                    disabled={ocupado}
                    onClick={aoEntregar}
                    style={{ ...botaoPrincipal, width: "100%", marginTop: "16px", minHeight: "48px" }}
                  >
                    {intl.formatMessage({ id: "agenda.entregarEAvisar" })}
                  </button>
                </div>
              )}

              {!emAtendimento && (
                <button
                  type="button"
                  disabled={ocupado}
                  onClick={aoMarcarFalta}
                  style={{ ...botaoSecundario, marginTop: "16px" }}
                >
                  {intl.formatMessage({ id: "agenda.naoVeio" })}
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "26px",
  fontWeight: 500,
  margin: 0,
  letterSpacing: "-0.02em",
} as const;

const rotulo = {
  display: "block",
  fontSize: "13px",
  fontWeight: 500,
  color: "oklch(0.42 0.015 150)",
  marginBottom: "7px",
} as const;

const campo = {
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "12px 14px",
  fontSize: "15px",
  width: "100%",
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
  padding: "12px 18px",
  minHeight: "44px",
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
  padding: "12px 18px",
  minHeight: "44px",
  cursor: "pointer",
} as const;

import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useEncerrarApadrinhamento,
  useEventosDoPadrinho,
  useMeusApadrinhamentos,
} from "../dados/apadrinhar.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * "O que voce passa a receber", da Tela 46 — o lado do padrinho depois do gesto.
 *
 * <b>E a linha do tempo dele, filtrada no que voce banca. Nenhum relatorio de marketing, nenhuma
 * newsletter de campanha.</b> Na pratica: sao os eventos de CUSTO do que ele banca, com data, valor e
 * quem comprou. Nenhum dado clinico chega aqui, e nao por falta de campo — e a promessa da tela.
 *
 * <b>Nao ha foto</b>, e o desenho mesmo avisa que ela nao e prometida: <i>"Uma foto, quando alguem do
 * abrigo tirar. Nao prometemos foto toda semana."</i> Anexo e dado do animal e vive sob escopo;
 * entregar um ao padrinho exigiria decidir quais anexos sao publicos, o que nao existe. Fica
 * declarado em vez de simulado.
 */

export const Route = createFileRoute("/apadrinhamentos")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Apadrinhamentos,
});

function Apadrinhamentos() {
  const intl = useIntl();

  const meus = useMeusApadrinhamentos();
  const encerrar = useEncerrarApadrinhamento();

  const [aberto, setAberto] = useState<string | null>(null);

  if (meus.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "apadrinhamentos.oQueE" })} />;
  }

  if (meus.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "apadrinhamentos.oQueE" })}
        erro={meus.error}
        aoTentarDeNovo={() => void meus.refetch()}
        carregando={meus.isFetching}
      />
    );
  }

  const lista = meus.data ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "780px", margin: "0 auto" }}>
        <h1 style={titulo}>{intl.formatMessage({ id: "apadrinhamentos.titulo" })}</h1>

        <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 28px" }}>
          {intl.formatMessage({ id: "apadrinhamentos.oQueEstaTelaE" })}
        </p>

        {encerrar.error !== null && encerrar.error !== undefined && (
          <div style={{ marginBottom: "16px" }}>
            <ErroAoGravar
              erro={encerrar.error}
              oQue={intl.formatMessage({ id: "apadrinhamentos.oQueE" })}
            />
          </div>
        )}

        {lista.length === 0 ? (
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: 0 }}>
            {intl.formatMessage({ id: "apadrinhamentos.vazio" })}
          </p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
            {lista.map((item) => (
              <article key={item.sponsorshipId} style={cartao}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: "16px", flexWrap: "wrap", alignItems: "baseline" }}>
                  <h2 style={{ ...titulo, fontSize: "20px", margin: 0 }}>
                    {intl.formatMessage(
                      { id: "apadrinhamentos.oQueVoceBanca" },
                      { oQue: item.description ?? "", animal: item.animalName ?? "" },
                    )}
                  </h2>
                  <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "16px" }}>
                    {intl.formatNumber(item.amount ?? 0, { style: "currency", currency: "BRL" })}
                    {intl.formatMessage({ id: "apadrinhamentos.porMes" })}
                  </div>
                </div>

                <div style={{ fontSize: "14px", color: CINZA, marginTop: "6px" }}>
                  {item.organizationName}
                </div>

                {/*
                 * O estado do encerramento e o que mais muda a leitura: quem pediu para parar
                 * continua bancando ate a data, e a tela diz a data em vez de so "encerrando".
                 */}
                <div style={{ fontSize: "14px", lineHeight: 1.6, marginTop: "10px" }}>
                  {item.status === "ENCERRAMENTO_PEDIDO" &&
                  item.endsOn !== null &&
                  item.endsOn !== undefined
                    ? intl.formatMessage(
                        { id: "apadrinhamentos.terminaEm" },
                        { quando: intl.formatDate(item.endsOn) },
                      )
                    : item.status === "ENCERRADO"
                      ? intl.formatMessage({ id: "apadrinhamentos.terminou" })
                      : intl.formatMessage(
                          { id: "apadrinhamentos.desde" },
                          { quando: intl.formatDate(item.startedOn ?? "") },
                        )}
                </div>

                <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap" }}>
                  <button
                    type="button"
                    onClick={() =>
                      setAberto(aberto === item.sponsorshipId ? null : (item.sponsorshipId ?? null))
                    }
                    style={botaoSecundario}
                  >
                    {intl.formatMessage({
                      id:
                        aberto === item.sponsorshipId
                          ? "apadrinhamentos.fechar"
                          : "apadrinhamentos.verOQueRecebo",
                    })}
                  </button>

                  {item.status === "ATIVO" && (
                    <button
                      type="button"
                      disabled={encerrar.isPending}
                      onClick={() => encerrar.mutate(item.sponsorshipId ?? "")}
                      style={botaoSecundario}
                    >
                      {intl.formatMessage({ id: "apadrinhamentos.parar" })}
                    </button>
                  )}
                </div>

                {aberto === item.sponsorshipId && (
                  <Eventos sponsorshipId={item.sponsorshipId ?? ""} />
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/** O feed: os eventos de custo do que este apadrinhamento banca. */
function Eventos({ sponsorshipId }: { sponsorshipId: string }) {
  const intl = useIntl();
  const eventos = useEventosDoPadrinho(sponsorshipId);

  if (eventos.isPending) {
    return (
      <div style={{ marginTop: "16px", fontSize: "14px", color: CINZA }}>
        {intl.formatMessage({ id: "apadrinhamentos.carregandoEventos" })}
      </div>
    );
  }

  const lista = eventos.data ?? [];

  return (
    <div style={{ marginTop: "16px", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "18px 20px" }}>
      {lista.length === 0 ? (
        /*
         * O vazio aqui e comum e nao e falha: o abrigo ainda nao lancou nada desde que a pessoa
         * comecou a bancar. A frase diz isso, em vez de deixar um espaco que parece defeito.
         */
        <div style={{ fontSize: "15px", lineHeight: 1.6, color: CINZA }}>
          {intl.formatMessage({ id: "apadrinhamentos.aindaSemEvento" })}
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {lista.map((evento) => (
            <div key={evento.costId} style={{ display: "flex", gap: "11px", fontSize: "15px", lineHeight: 1.5 }}>
              <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", background: VERDE, flex: "none", marginTop: "5px" }}></span>
              <span>
                {intl.formatMessage(
                  { id: "apadrinhamentos.evento" },
                  {
                    oQue: evento.description ?? "",
                    quando: intl.formatDate(evento.occurredAt ?? ""),
                    valor: intl.formatNumber(evento.amount ?? 0, {
                      style: "currency",
                      currency: "BRL",
                    }),
                  },
                )}
                {/*
                 * "assinado por quem comprou" e metade da promessa da tela. Quando nao ha pessoa
                 * atras do lancamento, o nome da organizacao ocupa o lugar — e sao duas mensagens
                 * porque o ICU nao aceita chave vazia em `select`.
                 */}
                {evento.recordedByName !== null && evento.recordedByName !== undefined
                  ? intl.formatMessage(
                      { id: "apadrinhamentos.evento.porQuem" },
                      { quem: evento.recordedByName },
                    )
                  : evento.organizationName !== null && evento.organizationName !== undefined
                    ? intl.formatMessage(
                        { id: "apadrinhamentos.evento.porQuem" },
                        { quem: evento.organizationName },
                      )
                    : ""}
              </span>
            </div>
          ))}
        </div>
      )}

      <div style={{ fontSize: "13px", color: CINZA, lineHeight: 1.6, marginTop: "14px" }}>
        {intl.formatMessage({ id: "apadrinhamentos.semFoto" })}
      </div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "30px",
  fontWeight: 500,
  margin: "0 0 8px",
  letterSpacing: "-0.02em",
} as const;

const cartao = {
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.86 0.008 150)",
  borderRadius: "12px",
  padding: "22px 24px",
} as const;

const botaoSecundario = {
  fontFamily: "inherit",
  fontSize: "15px",
  fontWeight: 500,
  color: "oklch(0.25 0.02 150)",
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "8px",
  padding: "12px 20px",
  minHeight: "48px",
  cursor: "pointer",
} as const;

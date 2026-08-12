import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar } from "../componentes/Estados.tsx";
import { useOrganizacoes } from "../dados/acessos.ts";
import {
  useDevolverDaHospedagem,
  useHospedagemEmCurso,
  useHospedar,
  useLinhaDoTempoDaEstadia,
} from "../dados/hospedagem.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 47 · o animal fora de casa — Uma semana de hospedagem", de
 * `design/IdentidadeVisual/Telas Petfy - Apadrinhar, hospedar, o ano.dc.html`.
 *
 * <b>"Voce esta em viagem. Isto e o que aconteceu com ele desde que saiu de casa."</b> E a tela
 * inteira numa frase: nao ha nada a fazer aqui, so a ler — e ler sem interromper ninguem. <i>"Hoje o
 * tutor manda mensagem perguntando se esta tudo bem, e a creche responde quando pode. Aqui ele abre e
 * ve — sem interromper ninguem, e sem depender da boa vontade de quem esta trabalhando."</i>
 *
 * ------------------------------------------------------------ uma so tela, dois estados
 *
 * O desenho mostra o animal JA hospedado. A entrega precisa existir em algum lugar, e ela mora aqui
 * mesmo: sem estadia em curso, a tela pergunta para onde e ate quando; com estadia, ela vira o que o
 * desenho desenhou. Duas rotas separariam o gesto do resultado, e quem entrega o animal e quem
 * acompanha sao a mesma pessoa, no mesmo dia.
 *
 * ------------------------------------------------------------ o que a tela NAO promete
 *
 * <b>"Se algo grave acontecer, voce e avisado na hora."</b> Isso nao existe: nao ha aviso in-app
 * neste produto, e a observacao urgente nao dispara e-mail. A tela nao repete a promessa — ela diz o
 * que faz, que e mostrar o que foi registrado. Prometer o aviso seria pior que nao ter: o tutor
 * pararia de olhar.
 */

export const Route = createFileRoute("/animais/$animalId_/hospedagem")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Hospedagem,
});

function Hospedagem() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const estadia = useHospedagemEmCurso(animalId);
  const hospedar = useHospedar(animalId);
  const devolver = useDevolverDaHospedagem(animalId);

  if (estadia.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "hospedagem.oQueE" })} />;
  }

  /*
   * O 404 aqui e a resposta NORMAL — "este animal esta em casa" —, e nao uma falha. Mostrar um
   * cartao de erro seria o defeito que a secao 06 nomeia: vazio nao e a mesma coisa que quebrado.
   */
  const emCasa = estadia.isError || estadia.data === undefined;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "820px", margin: "0 auto" }}>
        {emCasa ? (
          <Entregar
            animalId={animalId}
            aoEntregar={hospedar.mutate}
            enviando={hospedar.isPending}
            erro={hospedar.error}
          />
        ) : (
          <EmCurso
            animalId={animalId}
            estadia={estadia.data}
            aoDevolver={() => devolver.mutate()}
            devolvendo={devolver.isPending}
            erro={devolver.error}
          />
        )}
      </div>
    </div>
  );
}

/** O animal esta em casa: a tela pergunta para onde e ate quando. */
function Entregar({
  animalId,
  aoEntregar,
  enviando,
  erro,
}: {
  animalId: string;
  aoEntregar: (pedido: { organizacaoId: string; voltaEm: string }) => void;
  enviando: boolean;
  erro: unknown;
}) {
  const intl = useIntl();
  const organizacoes = useOrganizacoes();

  const [onde, setOnde] = useState("");
  const [volta, setVolta] = useState("");

  const pode = onde !== "" && volta !== "" && !enviando;

  return (
    <div style={cartao}>
      <h1 style={titulo}>{intl.formatMessage({ id: "hospedagem.entregar.titulo" })}</h1>

      <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 22px" }}>
        {intl.formatMessage({ id: "hospedagem.entregar.oQueE" })}
      </p>

      <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
        <div>
          <label htmlFor="onde" style={rotulo}>
            {intl.formatMessage({ id: "hospedagem.entregar.onde" })}
          </label>
          <select
            id="onde"
            value={onde}
            onChange={(evento) => setOnde(evento.target.value)}
            style={{ ...campo, cursor: "pointer" }}
          >
            <option value="">—</option>
            {(organizacoes.data ?? []).map((organizacao) => (
              <option key={organizacao.organizationId} value={organizacao.organizationId}>
                {organizacao.name}
              </option>
            ))}
          </select>
          {/*
           * O servidor recusa quem nao pode DETER CUSTODIA, e a tela avisa antes: uma clinica que
           * so registra ato clinico nao passa a responder pelo animal por uma semana.
           */}
          <div style={nota}>{intl.formatMessage({ id: "hospedagem.entregar.onde.nota" })}</div>
        </div>

        <div>
          <label htmlFor="volta" style={rotulo}>
            {intl.formatMessage({ id: "hospedagem.entregar.volta" })}
          </label>
          <input
            id="volta"
            type="date"
            value={volta}
            onChange={(evento) => setVolta(evento.target.value)}
            style={campo}
          />
          <div style={nota}>{intl.formatMessage({ id: "hospedagem.entregar.volta.nota" })}</div>
        </div>

        <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "18px 20px", display: "flex", gap: "12px", alignItems: "flex-start" }}>
          <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", border: "3px solid oklch(0.62 0.11 70)", flex: "none", marginTop: "4px" }}></span>
          <span style={{ fontSize: "15px", lineHeight: 1.6 }}>
            {intl.formatMessage({ id: "hospedagem.entregar.oQueMuda" })}
          </span>
        </div>

        {erro !== null && erro !== undefined && (
          <ErroAoGravar erro={erro} oQue={intl.formatMessage({ id: "hospedagem.oQueE" })} />
        )}

        <button
          type="button"
          disabled={!pode}
          onClick={() => aoEntregar({ organizacaoId: onde, voltaEm: volta })}
          style={{ ...botaoPrincipal, background: pode ? VERDE : "oklch(0.62 0.05 150)", cursor: pode ? "pointer" : "not-allowed" }}
        >
          {intl.formatMessage({ id: enviando ? "hospedagem.entregando" : "hospedagem.entregar" })}
        </button>

        <Link to="/animais/$animalId" params={{ animalId }} style={{ color: VERDE, fontSize: "15px" }}>
          {intl.formatMessage({ id: "hospedagem.voltarAoAnimal" })}
        </Link>
      </div>
    </div>
  );
}

/** O animal esta fora: a tela vira o que o desenho desenhou. */
function EmCurso({
  animalId,
  estadia,
  aoDevolver,
  devolvendo,
  erro,
}: {
  animalId: string;
  estadia: { [chave: string]: unknown } & {
    animalName?: string;
    organizationName?: string;
    startedAt?: string;
    expectedReturnOn?: string;
    dayOfStay?: number;
    totalDays?: number;
    returnsToName?: string;
    canEnd?: boolean;
  };
  aoDevolver: () => void;
  devolvendo: boolean;
  erro: unknown;
}) {
  const intl = useIntl();
  const eventos = useLinhaDoTempoDaEstadia(animalId, estadia.startedAt);

  return (
    <div style={{ ...cartao, padding: 0, overflow: "hidden" }}>
      <div style={{ padding: "28px 32px 20px", borderBottom: "1px solid oklch(0.92 0.006 150)" }}>
        <div style={{ display: "flex", justifyContent: "space-between", gap: "16px", flexWrap: "wrap", alignItems: "baseline" }}>
          <h1 style={{ ...titulo, margin: 0 }}>
            {intl.formatMessage(
              { id: "hospedagem.titulo" },
              { animal: estadia.animalName ?? "", onde: estadia.organizationName ?? "" },
            )}
          </h1>
          {/* "Dia 3 de 7": contado no servidor, e nao aqui — um calculo no cliente diria
              "dia 2" para quem abriu o Petfy em Lisboa. */}
          <span style={{ fontSize: "15px", color: CINZA }}>
            {intl.formatMessage(
              { id: "hospedagem.diaDe" },
              { dia: estadia.dayOfStay ?? 1, total: estadia.totalDays ?? 0 },
            )}
          </span>
        </div>

        <p style={{ fontSize: "15px", lineHeight: 1.6, color: CINZA, margin: "8px 0 0" }}>
          {intl.formatMessage(
            { id: "hospedagem.periodo" },
            {
              entrada: intl.formatDate(estadia.startedAt ?? ""),
              volta: intl.formatDate(estadia.expectedReturnOn ?? ""),
            },
          )}
        </p>
      </div>

      <div style={{ padding: "24px 32px 30px" }}>
        <div style={{ fontSize: "15px", lineHeight: 1.6, color: CINZA, marginBottom: "20px" }}>
          {intl.formatMessage({ id: "hospedagem.oQueAconteceu" })}
        </div>

        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", overflow: "hidden", marginBottom: "20px" }}>
          {eventos.isPending ? (
            <div style={{ padding: "18px 20px", fontSize: "15px", color: CINZA }}>
              {intl.formatMessage({ id: "hospedagem.carregando" })}
            </div>
          ) : (eventos.data ?? []).length === 0 ? (
            /* O vazio e comum e nao e falha: ninguem registrou nada desde a entrada. */
            <div style={{ padding: "18px 20px", fontSize: "15px", lineHeight: 1.6, color: CINZA }}>
              {intl.formatMessage(
                { id: "hospedagem.aindaNada" },
                { onde: estadia.organizationName ?? "" },
              )}
            </div>
          ) : (
            (eventos.data ?? []).map((evento) => (
              <div key={evento.eventId ?? `${evento.eventType}-${evento.occurredAt}`} style={{ display: "grid", gridTemplateColumns: "108px 1fr", borderBottom: "1px solid oklch(0.95 0.005 150)" }}>
                <div style={{ padding: "16px 16px 16px 20px", textAlign: "right", borderRight: "1px solid oklch(0.94 0.006 150)" }}>
                  <div style={{ fontSize: "14px", fontWeight: 500 }}>
                    {intl.formatDate(evento.occurredAt ?? "")}
                  </div>
                </div>
                <div style={{ padding: "16px 20px", display: "flex", gap: "12px", alignItems: "flex-start" }}>
                  <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", background: evento.visivel === false ? "oklch(0.85 0.008 150)" : VERDE, flex: "none", marginTop: "5px" }}></span>
                  <div>
                    <div style={{ fontSize: "16px", marginBottom: "3px" }}>
                      {/*
                       * Evento fora do escopo aparece OPACO em vez de sumir — sumir diria que o
                       * animal nao foi ao veterinario. E a mesma regra da linha do tempo de sempre,
                       * porque e a mesma linha do tempo.
                       */}
                      {evento.visivel === false
                        ? intl.formatMessage({ id: "hospedagem.evento.opaco" })
                        : (evento.summary ?? "")}
                    </div>
                    {evento.recordedByName !== null && evento.recordedByName !== undefined && (
                      <div style={{ fontSize: "14px", color: CINZA }}>
                        {evento.recordedByName}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px", marginBottom: "20px" }}>
          <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "12px" }}>
            {intl.formatMessage({ id: "hospedagem.enquantoEleEstaLa" })}
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: "11px", fontSize: "15px", lineHeight: 1.55 }}>
            <div>
              {intl.formatMessage(
                { id: "hospedagem.regra.responde" },
                {
                  onde: estadia.organizationName ?? "",
                  volta: intl.formatDate(estadia.expectedReturnOn ?? ""),
                },
              )}
            </div>
            <div>
              {intl.formatMessage(
                { id: "hospedagem.regra.voceContinuaLendo" },
                { quem: estadia.returnsToName ?? "" },
              )}
            </div>
          </div>
        </div>

        {erro !== null && erro !== undefined && (
          <div style={{ marginBottom: "16px" }}>
            <ErroAoGravar erro={erro} oQue={intl.formatMessage({ id: "hospedagem.oQueE" })} />
          </div>
        )}

        {estadia.canEnd === true && (
          <button
            type="button"
            disabled={devolvendo}
            onClick={aoDevolver}
            style={{ ...botaoPrincipal, background: VERDE }}
          >
            {intl.formatMessage({
              id: devolvendo ? "hospedagem.devolvendo" : "hospedagem.devolver",
            })}
          </button>
        )}
      </div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const cartao = {
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.86 0.008 150)",
  borderRadius: "12px",
  padding: "32px 34px 36px",
} as const;

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "26px",
  fontWeight: 500,
  margin: "0 0 8px",
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
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
  fontFamily: "inherit",
} as const;

const nota = {
  fontSize: "13px",
  color: CINZA,
  marginTop: "7px",
  lineHeight: 1.55,
} as const;

const botaoPrincipal = {
  fontFamily: "inherit",
  fontSize: "16px",
  fontWeight: 500,
  color: "oklch(1 0 0)",
  border: "none",
  borderRadius: "8px",
  padding: "15px 26px",
  minHeight: "52px",
  cursor: "pointer",
} as const;

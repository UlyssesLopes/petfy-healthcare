import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  FILTROS,
  useConcordar,
  useGatosDaColonia,
  useMarcarQueViu,
  usePedidosDeConcordancia,
  useRecusar,
  type Filtro,
  type GatoDaColonia,
  type PedidoDeConcordancia,
} from "../dados/colonia.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 43 · sem tutor — A colonia da Praca Benedito Calixto", de
 * `design/IdentidadeVisual/Telas Petfy - Animal comunitario e encaminhamento.dc.html`.
 *
 * <b>"Nenhum produto do mundo atende esse animal, porque todos assumem que existe um dono — e o
 * Petfy nunca assumiu isso."</b> O grupo e uma organizacao com custodia, informal e sem CNPJ; a
 * ONG que banca a castracao entra como uma segunda organizacao, com concessao. Nenhuma das duas
 * e dona.
 *
 * ------------------------------------------------------------ "visto por ultimo" e o sinal vital
 *
 * "Um gato de rua nao falta a creche nem deixa de comer em casa: ele some." Marcar que viu e o
 * gesto mais frequente desta tela, e por isso ele e UM TOQUE — sem formulario, sem confirmacao, e
 * idempotente no servidor: quem passa na praca de manha e a noite ja fez o que queria fazer.
 *
 * ----------------------------------------------------------- o que precisa de duas pessoas
 *
 * Adocao, encerramento e remocao de membro. "Sem dono, a protecao contra o gesto irreversivel de
 * uma pessoa so e o acordo de duas." O resto — marcar que viu, registrar ferida, levar ao
 * veterinario — e de qualquer um, e travar qualquer um deles mataria a tela: o valor da colonia
 * esta em o registro ser barato.
 */

export const Route = createFileRoute("/colonia")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Colonia,
});

function Colonia() {
  const intl = useIntl();
  const [filtro, setFiltro] = useState<Filtro>("TODOS");

  const gatos = useGatosDaColonia(filtro);
  const pedidos = usePedidosDeConcordancia();

  const lista = gatos.data ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>

        <div style={{ padding: "30px 36px 22px", background: "oklch(1 0 0)", borderBottom: "1px solid oklch(0.90 0.008 150)" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: 0, letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "colonia.titulo" })}
          </h1>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 400px", minHeight: "640px" }}>

          {/* ============================================================== a lista */}
          <div style={{ padding: "26px 32px" }}>
            <div role="group" style={{ display: "flex", gap: "8px", marginBottom: "18px", flexWrap: "wrap" }}>
              {FILTROS.map((opcao) => {
                const marcado = opcao === filtro;

                return (
                  <button
                    key={opcao}
                    type="button"
                    aria-pressed={marcado}
                    onClick={() => setFiltro(opcao)}
                    style={{ fontFamily: "inherit", fontSize: "14px", padding: "8px 14px", minHeight: "40px", display: "flex", alignItems: "center", borderRadius: "8px", cursor: "pointer", border: `1px solid ${marcado ? "oklch(0.46 0.085 150)" : "oklch(0.86 0.012 150)"}`, color: marcado ? "oklch(0.46 0.085 150)" : "oklch(0.42 0.015 150)", background: "oklch(1 0 0)" }}
                  >
                    {intl.formatMessage({ id: `colonia.filtro.${opcao}` })}
                  </button>
                );
              })}
            </div>

            {gatos.isPending ? (
              <Carregando oQue={intl.formatMessage({ id: "colonia.carregando" })} />
            ) : gatos.isError ? (
              <ErroDeCarga
                oQue={intl.formatMessage({ id: "colonia.carregando" })}
                erro={gatos.error}
                aoTentarDeNovo={() => void gatos.refetch()}
                carregando={gatos.isFetching}
              />
            ) : lista.length === 0 ? (
              <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)", border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px" }}>
                {intl.formatMessage({ id: "colonia.vazia" })}
              </div>
            ) : (
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
                <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1.3fr 1.2fr 120px", gap: "16px", padding: "11px 18px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", fontSize: "12px", letterSpacing: "0.04em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)" }}>
                  <div>{intl.formatMessage({ id: "colonia.coluna.gato" })}</div>
                  <div>{intl.formatMessage({ id: "colonia.coluna.situacao" })}</div>
                  <div>{intl.formatMessage({ id: "colonia.coluna.visto" })}</div>
                  <div></div>
                </div>

                {lista.map((gato, indice) => (
                  <Linha key={gato.animalId} gato={gato} ultimo={indice === lista.length - 1} />
                ))}
              </div>
            )}

            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", marginTop: "22px", padding: "22px 24px" }}>
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 500, marginBottom: "6px" }}>
                {intl.formatMessage({ id: "colonia.sinalVital.titulo" })}
              </div>
              <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage({ id: "colonia.sinalVital.texto" })}
              </div>
            </div>
          </div>

          {/* ====================================================== o painel do lado */}
          <div style={{ borderLeft: "1px solid oklch(0.90 0.008 150)", padding: "26px 24px", display: "flex", flexDirection: "column", gap: "24px" }}>

            <div>
              <Rotulo>{intl.formatMessage({ id: "colonia.pedidos" })}</Rotulo>

              {(pedidos.data ?? []).length === 0 ? (
                <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                  {intl.formatMessage({ id: "colonia.pedidos.vazio" })}
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {(pedidos.data ?? []).map((pedido) => (
                    <Pedido key={pedido.groupApprovalId} pedido={pedido} />
                  ))}
                </div>
              )}
            </div>

            <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "20px" }}>
              <Rotulo>{intl.formatMessage({ id: "colonia.qualquerUm" })}</Rotulo>
              <Itens
                chaves={["colonia.qualquerUm.registrar", "colonia.qualquerUm.veterinario"]}
                forma="circulo"
              />

              <div style={{ marginTop: "18px" }}>
                <Rotulo>{intl.formatMessage({ id: "colonia.duasPessoas" })}</Rotulo>
              </div>
              <Itens
                chaves={[
                  "colonia.duasPessoas.adocao",
                  "colonia.duasPessoas.obito",
                  "colonia.duasPessoas.remocao",
                ]}
                forma="losango"
              />

              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", lineHeight: 1.6, marginTop: "14px" }}>
                {intl.formatMessage({ id: "colonia.duasPessoas.porque" })}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Uma linha da lista.
 *
 * <b>"Vi hoje" e um botao, e nao um link nem um menu</b>: e o gesto mais frequente da tela, e cada
 * camada entre o dedo e o registro reduz o numero de marcacoes — que e a unica coisa que faz a
 * coluna do lado significar alguma coisa.
 */
function Linha({ gato, ultimo }: { gato: GatoDaColonia; ultimo: boolean }) {
  const intl = useIntl();
  const marcar = useMarcarQueViu();

  const sumido = gato.daysSinceLastSeen !== undefined && gato.daysSinceLastSeen > 15;

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "1.2fr 1.3fr 1.2fr 120px",
        gap: "16px",
        padding: "10px 18px",
        borderBottom: ultimo ? "none" : "1px solid oklch(0.95 0.005 150)",
        alignItems: "center",
        fontSize: "15px",
        minHeight: "58px",
        /* O fundo levemente quente marca o que pede acao hoje, como no desenho. Ele acompanha o
           losango, e nunca aparece sozinho: a cor e reforco, e a forma e a identidade. */
        background: sumido ? "oklch(0.985 0.008 30)" : "transparent",
      }}
    >
      <span style={{ fontFamily: "Bitter, Georgia, serif", fontWeight: 500 }}>{gato.name}</span>

      <Situacao gato={gato} />

      <span style={{ color: "oklch(0.42 0.015 150)" }}>{vistoPorUltimo(gato, intl)}</span>

      <div>
        {marcar.error !== null && marcar.error !== undefined ? (
          <ErroAoGravar erro={marcar.error} oQue={gato.name ?? ""} />
        ) : (
          <button
            type="button"
            disabled={marcar.isPending}
            onClick={() => marcar.mutate(gato.animalId!)}
            style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.46 0.085 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "8px 12px", minHeight: "40px", cursor: marcar.isPending ? "wait" : "pointer" }}
          >
            {intl.formatMessage({ id: marcar.isPending ? "colonia.marcando" : "colonia.marcarQueVi" })}
          </button>
        )}
      </div>
    </div>
  );
}

/**
 * A situacao, em tres estados.
 *
 * <b>O terceiro e tracejado de proposito</b>: "sem informacao" nao e um problema a resolver hoje,
 * e desenha-lo como os outros dois faria a lista parecer cheia de pendencia. A forma carrega o
 * estado — circulo cheio para o resolvido, anel para o marcado, tracejado para o que nao se sabe.
 */
function Situacao({ gato }: { gato: GatoDaColonia }) {
  const intl = useIntl();

  if (gato.neutered === true && gato.neuteredAt !== undefined) {
    return (
      <Estado forma="circulo">
        {intl.formatMessage(
          { id: "colonia.castrado" },
          { data: intl.formatDate(gato.neuteredAt, { month: "2-digit", year: "numeric", timeZone: "UTC" }) },
        )}
      </Estado>
    );
  }

  if (gato.ongoingCare !== undefined) {
    return <Estado forma="anel">{gato.ongoingCare}</Estado>;
  }

  if (gato.neuteringScheduledFor !== undefined) {
    return (
      <Estado forma="anel">
        {intl.formatMessage(
          { id: "colonia.castracaoMarcada" },
          { data: intl.formatDate(gato.neuteringScheduledFor, { day: "2-digit", month: "2-digit", timeZone: "UTC" }) },
        )}
      </Estado>
    );
  }

  return (
    <Estado forma="tracejado">{intl.formatMessage({ id: "colonia.semInformacao" })}</Estado>
  );
}

function Estado({ forma, children }: { forma: "circulo" | "anel" | "tracejado"; children: string }) {
  const marcador =
    forma === "circulo"
      ? { background: "oklch(0.46 0.085 150)", borderRadius: "999px" }
      : forma === "anel"
        ? { border: "3px solid oklch(0.62 0.11 70)", borderRadius: "999px" }
        : { border: "2px dashed oklch(0.6 0.015 150)", borderRadius: "999px" };

  return (
    <span style={{ display: "flex", alignItems: "center", gap: "8px", color: forma === "tracejado" ? "oklch(0.45 0.015 150)" : "inherit" }}>
      <span aria-hidden style={{ width: "11px", height: "11px", flex: "none", ...marcador }}></span>
      {children}
    </span>
  );
}

/**
 * "hoje, por Sandra" — e "ninguem marcou ainda", que e outra coisa.
 *
 * <b>Duas mensagens, e nao um `select` sobre campo vazio</b>: o ICU nao aceita chave vazia, e
 * aqui o caso raro e comum — o gato recem-cadastrado nao tem avistamento nenhum.
 */
function vistoPorUltimo(gato: GatoDaColonia, intl: ReturnType<typeof useIntl>): string {
  if (gato.daysSinceLastSeen === undefined || gato.lastSeenBy === undefined) {
    return intl.formatMessage({ id: "colonia.visto.nunca" });
  }

  const quem = gato.lastSeenBy;

  if (gato.daysSinceLastSeen === 0) {
    return intl.formatMessage({ id: "colonia.visto.hoje" }, { quem });
  }
  if (gato.daysSinceLastSeen === 1) {
    return intl.formatMessage({ id: "colonia.visto.ontem" }, { quem });
  }

  return intl.formatMessage({ id: "colonia.visto.dias" }, { dias: gato.daysSinceLastSeen, quem });
}

/**
 * Um pedido esperando decisao.
 *
 * <b>Quem pediu ve o proprio pedido, com a frase no lugar dos botoes.</b> O `canDecide` vem do
 * servidor — a tela nao recalcula quem pode decidir, do mesmo jeito que a Tela 32 nao recalcula
 * custodia. E a frase diz a REGRA, e nao "indisponivel": quem le precisa entender que nao e um
 * defeito da tela.
 */
function Pedido({ pedido }: { pedido: PedidoDeConcordancia }) {
  const intl = useIntl();
  const concordar = useConcordar();
  const recusar = useRecusar();

  const decidindo = concordar.isPending || recusar.isPending;

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "16px 18px" }}>
      <div style={{ fontSize: "15px", fontWeight: 500, marginBottom: "4px" }}>
        {intl.formatMessage(
          { id: `colonia.pedido.${pedido.kind}` },
          { animal: pedido.animalName ?? "", quem: pedido.toPersonName ?? pedido.targetPersonName ?? "" },
        )}
      </div>

      <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginBottom: "8px" }}>
        {intl.formatMessage({ id: "colonia.pedido.pedidoPor" }, { quem: pedido.requestedByName ?? "" })}
      </div>

      <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginBottom: "12px" }}>
        {pedido.reason ?? intl.formatMessage({ id: "colonia.pedido.semMotivo" })}
      </div>

      {(concordar.error ?? recusar.error) != null && (
        <div style={{ marginBottom: "10px" }}>
          <ErroAoGravar
            erro={concordar.error ?? recusar.error}
            oQue={pedido.animalName ?? ""}
          />
        </div>
      )}

      {pedido.canDecide === true ? (
        <div style={{ display: "flex", gap: "8px" }}>
          <button
            type="button"
            disabled={decidindo}
            onClick={() => concordar.mutate(pedido.groupApprovalId!)}
            style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "10px 16px", minHeight: "44px", cursor: decidindo ? "wait" : "pointer" }}
          >
            {intl.formatMessage({ id: "colonia.pedido.concordar" })}
          </button>
          <button
            type="button"
            disabled={decidindo}
            onClick={() => recusar.mutate(pedido.groupApprovalId!)}
            style={{ fontFamily: "inherit", fontSize: "14px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "10px 16px", minHeight: "44px", cursor: decidindo ? "wait" : "pointer" }}
          >
            {intl.formatMessage({ id: "colonia.pedido.recusar" })}
          </button>
        </div>
      ) : (
        <div style={{ fontSize: "13px", lineHeight: 1.55, color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "colonia.pedido.euPedi" })}
        </div>
      )}
    </div>
  );
}

function Itens({ chaves, forma }: { chaves: string[]; forma: "circulo" | "losango" }) {
  const intl = useIntl();

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "9px", fontSize: "15px", lineHeight: 1.5 }}>
      {chaves.map((chave) => (
        <div key={chave} style={{ display: "flex", gap: "10px" }}>
          <span
            aria-hidden
            style={{
              width: "11px",
              height: "11px",
              flex: "none",
              marginTop: "5px",
              background: forma === "circulo" ? "oklch(0.46 0.085 150)" : "oklch(0.55 0.14 30)",
              borderRadius: forma === "circulo" ? "999px" : 0,
              transform: forma === "circulo" ? "none" : "rotate(45deg)",
            }}
          ></span>
          <span>{intl.formatMessage({ id: chave })}</span>
        </div>
      ))}
    </div>
  );
}

function Rotulo({ children }: { children: string }) {
  return (
    <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "12px" }}>
      {children}
    </div>
  );
}

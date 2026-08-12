import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useApadrinhar, useOfertaDeApadrinhamento } from "../dados/apadrinhar.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 46 · publico, com conta — Apadrinhar o Teco", de
 * `design/IdentidadeVisual/Telas Petfy - Apadrinhar, hospedar, o ano.dc.html`.
 *
 * <b>ESTA TELA NAO COBRA NADA DE NINGUEM, e a ausencia e a decisao.</b> Nao ha meio de pagamento em
 * lugar nenhum deste produto, e inventar um aqui seria construir a metade menos interessante do
 * problema. O que o desenho promete e outra coisa: <i>"quando o remedio dele for comprado, voce vai
 * ver o evento — com data, valor e quem comprou."</i> O botao registra um compromisso; o valor corre
 * fora, entre o padrinho e o abrigo.
 *
 * ---------------------------------------------------- o que NAO aparece, e por que
 *
 * <b>Nenhum dado clinico.</b> <i>"Nenhum historico clinico aberto ao padrinho. Ele ve o que banca."</i>
 * O que parece diagnostico na tela — "Condroprotetor · uso continuo pela artrose" — e o texto que o
 * ABRIGO escreveu ao lancar o gasto, e nao algo lido do prontuario. Quem decide o que aparece e quem
 * escreve a linha de custo, e esse e exatamente o controle que o desenho promete.
 *
 * <b>Nenhuma barra de meta, nenhum ranking, nenhuma urgencia.</b> O desenho lista isso como o que
 * fica de fora, e a contagem de padrinhos existe so para quem chega saber que nao esta sozinho.
 */

export const Route = createFileRoute("/animais/$animalId_/apadrinhar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Apadrinhar,
});

function Apadrinhar() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const oferta = useOfertaDeApadrinhamento(animalId);
  const apadrinhar = useApadrinhar(animalId);

  const [escolhido, setEscolhido] = useState<string | null>(null);
  const [valorLivre, setValorLivre] = useState("");
  const [descricaoLivre, setDescricaoLivre] = useState("");
  const [enviado, setEnviado] = useState(false);

  if (oferta.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "apadrinhar.oQueE" })} />;
  }

  if (oferta.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "apadrinhar.oQueE" })}
        erro={oferta.error}
        aoTentarDeNovo={() => void oferta.refetch()}
        carregando={oferta.isFetching}
      />
    );
  }

  const linhas = oferta.data?.monthlyCosts ?? [];
  const nome = oferta.data?.name ?? "";
  const abrigo = oferta.data?.organizationName ?? "";

  const linhaEscolhida = linhas.find((linha) => linha.costId === escolhido);
  const ehLivre = escolhido === "LIVRE";

  const valor = ehLivre ? Number(valorLivre.replace(",", ".")) : (linhaEscolhida?.amount ?? 0);
  const descricao = ehLivre ? descricaoLivre.trim() : (linhaEscolhida?.description ?? "");

  const podeEnviar =
    escolhido !== null && descricao !== "" && valor > 0 && !apadrinhar.isPending;

  const enviar = () => {
    apadrinhar.mutate(
      {
        descricao,
        valor,
        custoId: ehLivre ? undefined : (linhaEscolhida?.costId ?? undefined),
      },
      { onSuccess: () => setEnviado(true) },
    );
  };

  if (enviado) {
    return (
      <div style={{ padding: "40px 24px" }}>
        <div style={{ ...cartao, maxWidth: "700px" }}>
          <h1 style={titulo}>
            {intl.formatMessage({ id: "apadrinhar.pronto.titulo" }, { nome })}
          </h1>
          <p style={{ fontSize: "15px", lineHeight: 1.65, margin: "0 0 12px" }}>
            {intl.formatMessage({ id: "apadrinhar.pronto.oQueAcontece" }, { abrigo })}
          </p>
          {/* O produto nao cobra, e a tela diz isso no momento em que a duvida aparece. */}
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 18px" }}>
            {intl.formatMessage({ id: "apadrinhar.pronto.oPetfyNaoCobra" }, { abrigo })}
          </p>
          <Link to="/apadrinhamentos" style={{ color: VERDE }}>
            {intl.formatMessage({ id: "apadrinhamentos.titulo" })}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ ...cartao, maxWidth: "760px", padding: 0, overflow: "hidden" }}>
        <div style={{ padding: "30px 34px 26px", borderBottom: "1px solid oklch(0.92 0.006 150)", background: "oklch(0.975 0.008 150)" }}>
          <h1 style={{ ...titulo, fontSize: "28px", marginBottom: "4px" }}>{nome}</h1>
          <div style={{ fontSize: "15px", color: CINZA }}>
            {intl.formatMessage(
              { id: "apadrinhar.identidade" },
              {
                especie: oferta.data?.species ?? "",
                raca: oferta.data?.breed ?? "",
                abrigo,
              },
            )}
          </div>
          {oferta.data?.atOrganizationSince !== null &&
            oferta.data?.atOrganizationSince !== undefined && (
              <div style={{ fontSize: "15px", color: CINZA, marginTop: "4px" }}>
                {intl.formatMessage(
                  { id: "apadrinhar.desdeQuando" },
                  { ano: new Date(oferta.data.atOrganizationSince).getFullYear() },
                )}
              </div>
            )}
        </div>

        <div style={{ padding: "26px 34px 32px" }}>
          {/* ------------------------------------------------ o que o abrigo gasta por mes */}
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", overflow: "hidden", marginBottom: "24px" }}>
            <div style={{ padding: "14px 20px", borderBottom: "1px solid oklch(0.94 0.006 150)", fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA }}>
              {intl.formatMessage({ id: "apadrinhar.oQueCusta" })}
            </div>

            {linhas.length === 0 ? (
              /* O vazio dirige, e nao anuncia falha: sem custo lancado nao ha o que bancar de
                 concreto, e a tela diz o que falta em vez de mostrar uma tabela vazia. */
              <div style={{ padding: "18px 20px", fontSize: "15px", lineHeight: 1.6, color: CINZA }}>
                {intl.formatMessage({ id: "apadrinhar.semCusto" }, { abrigo })}
              </div>
            ) : (
              linhas.map((linha) => (
                <div key={linha.costId} style={{ display: "grid", gridTemplateColumns: "1fr 110px", gap: "16px", padding: "13px 20px", borderBottom: "1px solid oklch(0.95 0.005 150)", alignItems: "center", fontSize: "15px", minHeight: "54px" }}>
                  <div>{linha.description}</div>
                  <div style={{ fontFamily: "'DM Mono', monospace", textAlign: "right" }}>
                    {intl.formatNumber(linha.amount ?? 0, { style: "currency", currency: "BRL" })}
                  </div>
                </div>
              ))
            )}

            {linhas.length > 0 && (
              <div style={{ display: "grid", gridTemplateColumns: "1fr 110px", gap: "16px", padding: "15px 20px", alignItems: "center", fontSize: "16px", minHeight: "56px", background: "oklch(0.975 0.004 150)" }}>
                <div style={{ fontWeight: 500 }}>
                  {intl.formatMessage({ id: "apadrinhar.custoReal" })}
                </div>
                <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "17px", fontWeight: 500, textAlign: "right" }}>
                  {intl.formatNumber(oferta.data?.monthlyTotal ?? 0, { style: "currency", currency: "BRL" })}
                </div>
              </div>
            )}
          </div>

          {/* ------------------------------------------------------ quanto voce quer bancar */}
          <div style={{ fontSize: "13px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "14px" }}>
            {intl.formatMessage({ id: "apadrinhar.quantoBancar" })}
          </div>

          <div style={{ display: "flex", gap: "12px", marginBottom: "16px", flexWrap: "wrap" }}>
            {linhas.map((linha) => (
              <button
                key={linha.costId}
                type="button"
                onClick={() => setEscolhido(linha.costId ?? null)}
                style={{ ...opcao, flex: "1 1 150px", ...(escolhido === linha.costId ? opcaoEscolhida : {}) }}
              >
                <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 500 }}>
                  {intl.formatNumber(linha.amount ?? 0, { style: "currency", currency: "BRL" })}
                </div>
                <div style={{ fontSize: "13px", color: CINZA, marginTop: "4px" }}>
                  {linha.description}
                </div>
              </button>
            ))}

            <button
              type="button"
              onClick={() => setEscolhido("LIVRE")}
              style={{ ...opcao, flex: "1 1 150px", ...(ehLivre ? opcaoEscolhida : {}) }}
            >
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 500 }}>
                {intl.formatMessage({ id: "apadrinhar.outro" })}
              </div>
              <div style={{ fontSize: "13px", color: CINZA, marginTop: "4px" }}>
                {intl.formatMessage({ id: "apadrinhar.outro.nota" })}
              </div>
            </button>
          </div>

          {ehLivre && (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginBottom: "16px" }}>
              <div>
                <label htmlFor="descricaoLivre" style={rotulo}>
                  {intl.formatMessage({ id: "apadrinhar.livre.oQue" })}
                </label>
                <input
                  id="descricaoLivre"
                  value={descricaoLivre}
                  onChange={(evento) => setDescricaoLivre(evento.target.value)}
                  style={campo}
                />
              </div>
              <div>
                <label htmlFor="valorLivre" style={rotulo}>
                  {intl.formatMessage({ id: "apadrinhar.livre.quanto" })}
                </label>
                <input
                  id="valorLivre"
                  inputMode="decimal"
                  value={valorLivre}
                  onChange={(evento) => setValorLivre(evento.target.value)}
                  style={campo}
                />
              </div>
            </div>
          )}

          <div style={{ fontSize: "14px", color: CINZA, lineHeight: 1.6, marginBottom: "24px" }}>
            {intl.formatMessage({ id: "apadrinhar.coisaConcreta" })}
          </div>

          {apadrinhar.error !== null && apadrinhar.error !== undefined && (
            <div style={{ marginBottom: "16px" }}>
              <ErroAoGravar erro={apadrinhar.error} oQue={nome} />
            </div>
          )}

          <button
            type="button"
            disabled={!podeEnviar}
            onClick={enviar}
            style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: podeEnviar ? VERDE : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "15px 26px", minHeight: "52px", cursor: podeEnviar ? "pointer" : "not-allowed" }}
          >
            {intl.formatMessage({
              id: apadrinhar.isPending ? "apadrinhar.enviando" : "apadrinhar.bancar",
            })}
          </button>

          <div style={{ fontSize: "14px", color: CINZA, lineHeight: 1.6, marginTop: "12px" }}>
            {intl.formatMessage({ id: "apadrinhar.podeParar" })}
          </div>

          {/* Um numero, e nao nomes: nenhum ranking, nenhuma barra de meta. */}
          {(oferta.data?.sponsorCount ?? 0) > 0 && (
            <div style={{ fontSize: "14px", color: CINZA, lineHeight: 1.6, marginTop: "8px" }}>
              {intl.formatMessage(
                { id: "apadrinhar.quantosBancam" },
                { quantos: oferta.data?.sponsorCount ?? 0 },
              )}
            </div>
          )}

          {!podeEnviar && !apadrinhar.isPending && (
            <div style={{ fontSize: "13px", color: CINZA, marginTop: "10px", lineHeight: 1.55 }}>
              {intl.formatMessage({ id: "apadrinhar.faltaEscolher" })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const cartao = {
  margin: "0 auto",
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

const opcao = {
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "8px",
  padding: "16px",
  textAlign: "center",
  background: "oklch(1 0 0)",
  fontFamily: "inherit",
  cursor: "pointer",
  minHeight: "44px",
} as const;

const opcaoEscolhida = {
  border: `1px solid ${VERDE}`,
  background: "oklch(0.96 0.02 150)",
} as const;

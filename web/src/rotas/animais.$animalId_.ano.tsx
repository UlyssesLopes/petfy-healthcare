import { createFileRoute, redirect } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnoDoAnimal } from "../dados/ano.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 48 · uma vez por ano — O ano do Code", de
 * `design/IdentidadeVisual/Telas Petfy - Apadrinhar, hospedar, o ano.dc.html`.
 *
 * <b>"NAO E RETROSPECTIVA."</b> E a frase que governa esta tela inteira: <i>"um resumo anual de
 * produto costuma ser sentimental e inutil: colagem de fotos, numero de passos, 'que ano incrivel'.
 * Este e um documento clinico e social do animal, e serve para levar ao veterinario. Por isso ele
 * inclui o que deu errado."</i>
 *
 * Metade desta tela existe para mostrar o que deu errado — os dias com a vacina vencida e as
 * condicoes cronicas que ninguem reavaliou. <b>Um resumo que so mostra o bonito nao serve para
 * cuidar.</b>
 *
 * ------------------------------------------------------------ o que nao existe aqui
 *
 * <b>Nenhum botao de rede social, nenhuma imagem pronta para story, nenhuma marca d'agua.</b> "Se o
 * tutor quiser mostrar, ele imprime ou tira foto da tela — e o produto nao ganha nada com isso, o que
 * e exatamente o ponto." O unico botao e "Imprimir", e ele chama o `print` do navegador.
 *
 * <b>E ela nao chega sozinha.</b> O desenho diz que o resumo aparece "uma vez por ano, no aniversario
 * estimado do animal, como informe no feed, nunca como notificacao no celular". O feed in-app nao
 * existe neste produto, entao a tela existe e o gatilho nao — fica declarado em vez de simulado.
 */

export const Route = createFileRoute("/animais/$animalId_/ano")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: OAno,
});

function OAno() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const ano = useAnoDoAnimal(animalId);

  if (ano.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "ano.oQueE" })} />;
  }

  if (ano.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "ano.oQueE" })}
        erro={ano.error}
        aoTentarDeNovo={() => void ano.refetch()}
        carregando={ano.isFetching}
      />
    );
  }

  const dados = ano.data;
  const vencidas = dados?.lapses ?? [];
  const naoReavaliadas = dados?.notReassessed ?? [];
  const quemCuidou = dados?.caregivers ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "760px", margin: "0 auto", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "40px 44px 44px" }}>

        <div style={{ display: "flex", justifyContent: "space-between", gap: "20px", alignItems: "baseline", flexWrap: "wrap", marginBottom: "28px" }}>
          <div>
            <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 6px", letterSpacing: "-0.02em" }}>
              {intl.formatMessage({ id: "ano.titulo" }, { nome: dados?.animalName ?? "" })}
            </h1>
            <p style={{ fontSize: "15px", color: CINZA, margin: 0 }}>
              {intl.formatMessage(
                { id: "ano.periodo" },
                {
                  de: intl.formatDate(dados?.from ?? "", { month: "long", year: "numeric" }),
                  ate: intl.formatDate(dados?.to ?? "", { month: "long", year: "numeric" }),
                },
              )}
              {/*
               * Duas mensagens em vez de um `select` com chave vazia: o ICU nao aceita chave vazia,
               * e o ano de vida so existe quando se sabe a data de nascimento.
               */}
              {dados?.yearOfLife !== null && dados?.yearOfLife !== undefined
                ? intl.formatMessage({ id: "ano.anoDeVida" }, { ano: dados.yearOfLife })
                : ""}
            </p>
          </div>

          {/* O unico botao da tela, e ele nao publica nada em lugar nenhum. */}
          <button type="button" onClick={() => window.print()} style={botaoSecundario}>
            {intl.formatMessage({ id: "ano.imprimir" })}
          </button>
        </div>

        {/* ------------------------------------------------------------------ os numeros */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "16px", marginBottom: "26px" }}>
          <Numero
            rotulo={intl.formatMessage({ id: "ano.registros" })}
            valor={String(dados?.records ?? 0)}
            nota={intl.formatMessage(
              { id: "ano.registros.nota" },
              { pessoas: dados?.people ?? 0, organizacoes: dados?.organizations ?? 0 },
            )}
          />
          <Numero
            rotulo={intl.formatMessage({ id: "ano.creche" })}
            valor={String(dados?.daycareDays ?? 0)}
            nota={intl.formatMessage(
              { id: "ano.creche.nota" },
              { dias: dados?.boardingDays ?? 0 },
            )}
          />
          <Numero
            rotulo={intl.formatMessage({ id: "ano.peso" })}
            valor={
              dados?.weightNow === null || dados?.weightNow === undefined
                ? "—"
                : intl.formatMessage({ id: "ano.peso.valor" }, { kg: dados.weightNow })
            }
            nota={
              dados?.weightBefore === null || dados?.weightBefore === undefined
                ? intl.formatMessage({ id: "ano.peso.semAnterior" })
                : intl.formatMessage({ id: "ano.peso.antes" }, { kg: dados.weightBefore })
            }
          />
        </div>

        {/* -------------------------------------------------- o que aconteceu de saude */}
        <section style={caixa}>
          <div style={cabecalhoDaCaixa}>
            {intl.formatMessage({ id: "ano.saude" })}
          </div>
          <div style={{ padding: "18px 22px", display: "flex", flexDirection: "column", gap: "13px", fontSize: "15px", lineHeight: 1.6 }}>
            <Linha cor={VERDE}>
              {intl.formatMessage(
                { id: "ano.saude.consultas" },
                { quantas: dados?.appointments ?? 0 },
              )}
            </Linha>
            <Linha cor={VERDE}>
              {intl.formatMessage(
                { id: "ano.saude.doses" },
                { vacinas: dados?.vaccines ?? 0, antiparasitarios: dados?.antiparasitics ?? 0 },
              )}
            </Linha>

            {/*
             * O QUE DEU ERRADO. E a parte que nenhum resumo automatico costuma ter, e a unica que
             * faz o documento valer alguma coisa numa consulta.
             */}
            {vencidas.map((vencida) => (
              <Linha key={`${vencida.vaccineName}-${vencida.overdueSince}`} cor={AMBAR}>
                {vencida.regularizedOn === null || vencida.regularizedOn === undefined
                  ? intl.formatMessage(
                      { id: "ano.saude.vencidaAgora" },
                      {
                        vacina: vencida.vaccineName ?? "",
                        dias: vencida.days ?? 0,
                        desde: intl.formatDate(vencida.overdueSince ?? ""),
                      },
                    )
                  : intl.formatMessage(
                      { id: "ano.saude.esteveVencida" },
                      {
                        vacina: vencida.vaccineName ?? "",
                        dias: vencida.days ?? 0,
                        quando: intl.formatDate(vencida.overdueSince ?? "", { month: "long" }),
                      },
                    )}
              </Linha>
            ))}

            {naoReavaliadas.map((pendente) => (
              <Linha key={pendente.description} cor={CINZA} tracejado>
                {intl.formatMessage(
                  { id: "ano.saude.naoReavaliada" },
                  {
                    condicao: pendente.description ?? "",
                    ano: new Date(pendente.lastTouchedOn ?? "").getFullYear(),
                  },
                )}
              </Linha>
            ))}

            {/*
             * O vazio aqui e uma NOTICIA BOA, e a frase diz isso — em vez de deixar a seção sem
             * nada, que pareceria dado faltando.
             */}
            {vencidas.length === 0 && naoReavaliadas.length === 0 && (
              <Linha cor={VERDE}>{intl.formatMessage({ id: "ano.saude.nadaPendente" })}</Linha>
            )}
          </div>
        </section>

        {/* --------------------------------------------------- quem cuidou dele este ano */}
        <section style={{ ...caixa, marginTop: "26px" }}>
          <div style={cabecalhoDaCaixa}>
            {intl.formatMessage({ id: "ano.quemCuidou" })}
          </div>
          <div style={{ padding: "18px 22px", display: "flex", flexDirection: "column", gap: "11px", fontSize: "15px" }}>
            {quemCuidou.length === 0 ? (
              <span style={{ color: CINZA, lineHeight: 1.6 }}>
                {intl.formatMessage({ id: "ano.quemCuidou.ninguem" })}
              </span>
            ) : (
              quemCuidou.map((quem) => (
                <div
                  key={`${quem.personName}-${quem.organizationName}`}
                  style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px" }}
                >
                  <span>
                    {/* "Rafaela Lopes, pela Creche Quintal" — duas mensagens, porque o ICU
                        nao aceita chave vazia em `select`. */}
                    {quem.organizationName === null || quem.organizationName === undefined
                      ? (quem.personName ?? "")
                      : intl.formatMessage(
                          { id: "ano.quemCuidou.pelaOrganizacao" },
                          {
                            quem: quem.personName ?? "",
                            organizacao: quem.organizationName,
                          },
                        )}
                  </span>
                  <span style={{ color: CINZA }}>
                    {intl.formatMessage({ id: "ano.quemCuidou.registros" }, { quantos: quem.records ?? 0 })}
                  </span>
                </div>
              ))
            )}
          </div>
        </section>

        <div style={{ borderTop: "1px solid oklch(0.92 0.006 150)", paddingTop: "22px", marginTop: "26px", fontSize: "15px", lineHeight: 1.7, color: "oklch(0.42 0.015 150)" }}>
          {intl.formatMessage({ id: "ano.paraQueServe" })}
        </div>
      </div>
    </div>
  );
}

function Numero({ rotulo, valor, nota }: { rotulo: string; valor: string; nota: string }) {
  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px" }}>
      <div style={{ fontSize: "13px", color: CINZA, marginBottom: "8px" }}>{rotulo}</div>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, letterSpacing: "-0.02em" }}>
        {valor}
      </div>
      <div style={{ fontSize: "13px", color: CINZA, marginTop: "5px", lineHeight: 1.45 }}>{nota}</div>
    </div>
  );
}

function Linha({
  children,
  cor,
  tracejado,
}: {
  children: React.ReactNode;
  cor: string;
  tracejado?: boolean;
}) {
  return (
    <div style={{ display: "flex", gap: "12px" }}>
      <span
        aria-hidden
        style={{
          width: "12px",
          height: "12px",
          borderRadius: "999px",
          flex: "none",
          marginTop: "5px",
          ...(tracejado === true
            ? { border: `2px dashed ${cor}` }
            : cor === AMBAR
              ? { border: `3px solid ${cor}` }
              : { background: cor }),
        }}
      ></span>
      <span>{children}</span>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";
const AMBAR = "oklch(0.62 0.11 70)";

const caixa = {
  border: "1px solid oklch(0.90 0.008 150)",
  borderRadius: "12px",
  overflow: "hidden",
} as const;

const cabecalhoDaCaixa = {
  padding: "14px 22px",
  borderBottom: "1px solid oklch(0.94 0.006 150)",
  fontSize: "12px",
  letterSpacing: "0.05em",
  textTransform: "uppercase",
  color: CINZA,
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

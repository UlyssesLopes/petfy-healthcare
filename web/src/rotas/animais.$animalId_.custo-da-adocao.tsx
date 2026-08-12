import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useIntl } from "react-intl";
import { type ReactNode } from "react";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import {
  usePrevisaoDeCusto,
  useResumoDeCusto,
  type PrevisaoDeCusto,
  type ResumoDeCusto,
} from "../dados/custo.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 39 · onde isto muda uma vida — A conversa honesta antes da adocao", de
 * `design/IdentidadeVisual/Telas Petfy - Custo do cuidado.dc.html`.
 *
 * <b>O desenho a chama de a tela mais valiosa do conjunto, e explica por que:</b> "o abrigo tem um
 * problema que custa vidas: o animal volta. Quase sempre porque alguem adotou sem saber que um cao
 * de 11 anos com artrose custa R$ 270 por mes, todo mes, ate o fim. Nenhum questionario de adocao
 * resolve isso, porque o abrigo tambem nao tinha o numero."
 *
 * <b>NENHUMA REGRA DE ACESSO NOVA, e isso vale registrar.</b> O `requireCustodia` ja conta custodia
 * de ORGANIZACAO desde a Tela 13 — o abrigo que resgatou o animal responde por ele. Entao o abrigo
 * le o custo do Teco pelas mesmas rotas do tutor, sem excecao nenhuma no meio. O ADOTANTE nao le:
 * ele ainda nao responde pelo animal, e quem mostra e o abrigo, na conversa.
 *
 * ------------------------------------------------ o numero que o desenho chama de "previsto"
 *
 * O desenho poe "Previsto para os proximos 12: R$ 3.900 — tende a subir: ele tem 11 anos".
 *
 * <b>A previsao deste produto cobra so O QUE ESTA MARCADO</b> — dose com data de reforco,
 * mensalidade combinada, compra que se repete. A consulta que vai aparecer no meio do ano NAO esta
 * nela, e nao pode estar: seria estimativa, e "isto nao e previsao de gasto".
 *
 * Logo o numero tende a vir MENOR que os doze meses que passaram. Chama-lo de "previsto" na frente
 * de quem esta decidindo adotar subestimaria o custo do animal — o oposto exato do que esta tela
 * existe para fazer. Entao ele e rotulado pelo que e: <b>"ja marcado"</b>, com a falta dita ao lado.
 *
 * E "tende a subir porque ele tem 11 anos" nao e afirmado pelo produto: definir a partir de que
 * idade um animal e idoso e conhecimento veterinario que este codigo nao tem, e a frase do desenho
 * e do abrigo, nao do servidor. A IDADE aparece como fato, e o julgamento fica com quem conversa.
 */

export const Route = createFileRoute("/animais/$animalId_/custo-da-adocao")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: CustoDaAdocao,
});

function CustoDaAdocao() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const contexto = useMeuContexto();
  const resumo = useResumoDeCusto(animalId, "DOZE_MESES");
  const previsao = usePrevisaoDeCusto(animalId);

  const bicho = animal.data;
  const nome = bicho?.name ?? "";
  const ativo = contexto.data?.active;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", display: "grid", gridTemplateColumns: "minmax(0, 700px) minmax(0, 600px)", gap: "24px", alignItems: "start" }}>

        <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
          <div style={{ padding: "28px 32px 24px", borderBottom: "1px solid oklch(0.92 0.006 150)", background: "oklch(0.975 0.008 150)" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "16px", marginBottom: "16px" }}>
              {/*
               * O retrato que nao existe: hachura no lugar da foto. O produto nao guarda foto de
               * perfil, e um circulo cinza vazio pareceria uma foto que falhou em carregar.
               */}
              <span
                aria-hidden
                style={{ width: "60px", height: "60px", borderRadius: "999px", flex: "none", background: "repeating-linear-gradient(135deg, oklch(0.92 0.02 150) 0 8px, oklch(0.95 0.014 150) 8px 16px)" }}
              ></span>

              <div>
                <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "26px", fontWeight: 500, letterSpacing: "-0.02em", margin: 0 }}>
                  {nome}
                </h1>
                <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
                  {aFicha(bicho, ativo?.organizationName, intl)}
                </div>
              </div>
            </div>

            <div style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
              {intl.formatMessage({ id: "adocaoCusto.abertura" }, { nome })}
            </div>
          </div>

          <div style={{ padding: "26px 32px 30px" }}>
            {resumo.isError ? (
              <ErroDeCarga
                oQue={intl.formatMessage({ id: "adocaoCusto.oQue" })}
                erro={resumo.error}
                aoTentarDeNovo={() => void resumo.refetch()}
                carregando={resumo.isFetching}
              />
            ) : resumo.isPending || resumo.data === undefined ? (
              <Carregando oQue={intl.formatMessage({ id: "adocaoCusto.oQue" })} />
            ) : (
              <>
                <OsDoisNumeros nome={nome} resumo={resumo.data} previsao={previsao.data} />
                <OQuePrecisaTodoMes nome={nome} previsao={previsao.data} />

                {/*
                 * A frase que separa esta tela de uma calculadora de mercado, e ela e o valor
                 * inteiro do numero: "estes valores sao o que o abrigo REALMENTE PAGOU, evento por
                 * evento, e nao uma estimativa de mercado."
                 */}
                <div style={{ border: "1px solid oklch(0.88 0.02 150)", background: "oklch(0.98 0.008 150)", borderRadius: "12px", padding: "20px 22px", fontSize: "15px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)" }}>
                  {intl.formatMessage({ id: "adocaoCusto.realmentePagou" })}
                </div>
              </>
            )}

            <div style={{ marginTop: "24px" }}>
              <Link to="/animais/$animalId/adocao" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
                {intl.formatMessage({ id: "adocaoCusto.voltar" }, { nome })}
              </Link>
            </div>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <Cartao titulo={intl.formatMessage({ id: "adocaoCusto.porQue.titulo" })}>
            <p style={{ margin: "0 0 14px" }}>
              {intl.formatMessage({ id: "adocaoCusto.porQue.p1" })}
            </p>
            <p style={{ margin: "0 0 14px" }}>
              {intl.formatMessage({ id: "adocaoCusto.porQue.p2" }, { nome })}
            </p>
            <p style={{ margin: 0 }}>{intl.formatMessage({ id: "adocaoCusto.porQue.p3" })}</p>
          </Cartao>

          <Cartao titulo={intl.formatMessage({ id: "adocaoCusto.foraDeProposito.titulo" })}>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {["triagem", "orcamento", "plano"].map((ausencia) => (
                <div key={ausencia} style={{ display: "flex", gap: "12px" }}>
                  {/* circulo tracejado: ausencia declarada, e nao item de lista */}
                  <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", border: "2px dashed oklch(0.6 0.015 150)", flex: "none", marginTop: "5px" }}></span>
                  <span>{intl.formatMessage({ id: `adocaoCusto.foraDeProposito.${ausencia}` })}</span>
                </div>
              ))}
            </div>
          </Cartao>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/**
 * Os dois numeros: o que custou, e o que ja esta marcado.
 *
 * <b>O segundo NAO se chama "previsto", e essa e a decisao mais importante desta tela.</b> A
 * previsao cobre so o que tem data ou se repete — a consulta que vai aparecer no meio do ano nao
 * esta nela. Chamar isso de "previsto" na frente de quem esta decidindo adotar subestimaria o custo
 * do animal, que e o oposto exato do que esta tela existe para fazer.
 */
function OsDoisNumeros({
  nome,
  resumo,
  previsao,
}: {
  nome: string;
  resumo: ResumoDeCusto;
  previsao: PrevisaoDeCusto | undefined;
}) {
  const intl = useIntl();

  return (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(2, minmax(0, 1fr))", gap: "16px", marginBottom: "24px" }}>
      <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px" }}>
        <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginBottom: "8px" }}>
          {intl.formatMessage({ id: "adocaoCusto.custou" })}
        </div>
        <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, letterSpacing: "-0.02em" }}>
          {dinheiro(intl, resumo.total)}
        </div>
        <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "6px" }}>
          {intl.formatMessage(
            { id: "adocaoCusto.porMes" },
            { valor: dinheiro(intl, resumo.monthlyAverage) },
          )}
        </div>
      </div>

      <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "20px 22px" }}>
        <div style={{ fontSize: "13px", color: "oklch(0.5 0.09 70)", marginBottom: "8px" }}>
          {intl.formatMessage({ id: "adocaoCusto.jaMarcado" })}
        </div>
        <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, letterSpacing: "-0.02em" }}>
          {dinheiro(intl, previsao?.total)}
        </div>
        {/*
         * A FALTA DITA AO LADO DO NUMERO, e nao num rodape: quem le o numero precisa ler a
         * ressalva no mesmo movimento. Sem ela este cartao seria um piso com cara de teto.
         */}
        <div style={{ fontSize: "14px", color: "oklch(0.42 0.015 150)", marginTop: "6px", lineHeight: 1.5 }}>
          {intl.formatMessage({ id: "adocaoCusto.jaMarcado.falta" }, { nome })}
        </div>
      </div>
    </div>
  );
}

/**
 * "O que o Teco precisa todo mes."
 *
 * As linhas que se repetem, da previsao — mensalidade e compra marcada como mensal. <b>E a caixinha
 * "dura cerca de um mes" da Tela 42 que faz esta lista existir:</b> "e o que permite ao abrigo dizer
 * ao adotante do Teco que a racao dele custa R$ 190 por mes, todo mes. Sem ela, o produto so saberia
 * somar o passado."
 */
function OQuePrecisaTodoMes({
  nome,
  previsao,
}: {
  nome: string;
  previsao: PrevisaoDeCusto | undefined;
}) {
  const intl = useIntl();

  const todoMes = (previsao?.items ?? []).filter((item) => item.dueOn === undefined);

  if (todoMes.length === 0) {
    return (
      <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "20px 22px", marginBottom: "20px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
        {intl.formatMessage({ id: "adocaoCusto.todoMes.vazio" }, { nome })}
      </div>
    );
  }

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", overflow: "hidden", marginBottom: "20px" }}>
      <div style={{ padding: "14px 20px", borderBottom: "1px solid oklch(0.94 0.006 150)", fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)" }}>
        {intl.formatMessage({ id: "adocaoCusto.todoMes" }, { nome })}
      </div>

      {todoMes.map((item, indice) => (
        <div
          key={`${item.kind}-${item.sourceId ?? indice}`}
          style={{ display: "grid", gridTemplateColumns: "1fr 120px", gap: "16px", padding: "13px 20px", alignItems: "center", fontSize: "15px", minHeight: "54px", borderTop: indice === 0 ? "none" : "1px solid oklch(0.95 0.005 150)" }}
        >
          <div>
            {item.description}
            <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
              {intl.formatMessage({ id: `adocaoCusto.origem.${item.kind}` })}
            </div>
          </div>
          <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", textAlign: "right" }}>
            {dinheiro(intl, item.amount)}
          </div>
        </div>
      ))}
    </div>
  );
}

function Cartao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "26px 28px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 500, marginBottom: "14px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.7, color: "oklch(0.35 0.018 150)" }}>{children}</div>
    </div>
  );
}

/**
 * "Cao · SRD · 11 anos · Abrigo Lar dos Focinhos".
 *
 * <b>A IDADE E FATO, e o julgamento sobre ela nao e do produto.</b> O desenho escreve "tende a
 * subir: ele tem 11 anos" — a idade entra, e a inferencia fica com quem conversa: definir a partir
 * de quando um animal e idoso e conhecimento veterinario que este codigo nao tem, e varia por
 * especie e por porte.
 *
 * Cada parte so aparece se existir: um animal sem raca declarada nao ganha "SRD" por suposicao.
 */
function aFicha(
  bicho: { species?: string; breed?: string; bornDate?: string } | undefined,
  organizacao: string | undefined,
  intl: ReturnType<typeof useIntl>,
): string {
  if (bicho === undefined) {
    return "";
  }

  const anos =
    bicho.bornDate === undefined
      ? undefined
      : Math.floor(
          (Date.now() - new Date(`${bicho.bornDate}T12:00:00`).getTime()) / (365.25 * 86_400_000),
        );

  return [
    bicho.species === undefined
      ? undefined
      : intl.formatMessage({ id: `animal.especie.${bicho.species}` }),
    bicho.breed,
    anos === undefined ? undefined : intl.formatMessage({ id: "adocaoCusto.anos" }, { anos }),
    organizacao,
  ]
    .filter((parte) => parte !== undefined && parte !== "")
    .join(" · ");
}

/** Dinheiro em real, sem centavos — os numeros do desenho sao "R$ 3.240". */
function dinheiro(intl: ReturnType<typeof useIntl>, valor: number | undefined): string {
  return intl.formatNumber(valor ?? 0, {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  });
}

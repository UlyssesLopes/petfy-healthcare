import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { RegistrarDose } from "../componentes/RegistrarDose.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { useMatriculasDoAnimal, type Matricula } from "../dados/creche.ts";
import { usePrevisaoDeCusto, type PrevisaoDeCusto } from "../dados/custo.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 38 · o que so o Petfy consegue dizer — O que vem pela frente, e o custo de adiar", de
 * `design/IdentidadeVisual/Telas Petfy - Custo do cuidado.dc.html`.
 *
 * <b>"ISTO NAO E PREVISAO DE GASTO: E O QUE JA ESTA MARCADO NO REGISTRO DELE."</b> Nenhuma linha e
 * estatistica ou tendencia: e a data de reforco escrita na carteira, a mensalidade combinada, a
 * compra que o tutor disse que dura um mes. "A previsao nao e estatistica: e a data de reforco que
 * ja esta escrita na carteirinha do animal."
 *
 * <b>O LIMITE DA LEITURA E CLINICO, e o desenho o declara:</b> "ela pode dizer que adiar a vacina
 * custa dias de creche perdidos, porque isso e aritmetica sobre fatos registrados. Nunca vai dizer
 * que tratar a displasia agora sai mais barato que operar depois — isso e prognostico clinico, e o
 * Petfy nao faz prognostico."
 *
 * ------------------------------------------------------- um numero do desenho nao pode existir
 *
 * O desenho escreve, na leitura: "voce perde os dias pagos — foram R$ 176 no mes passado, quando
 * isso aconteceu com OUTRO ANIMAL DA TURMA". <b>Esse numero e o custo de outro animal, e ler custo
 * exige custodia daquele animal.</b> "Nenhum escopo de acesso concede preco junto com saude" — e
 * seria estranho que a excecao aparecesse justamente numa frase que o tutor le.
 *
 * O que entra no lugar e aritmetica sobre o que ESTE tutor ja pode ver: quanto vale um dia
 * combinado do proprio animal, derivado da mensalidade que ele paga e do numero de dias que ele
 * combinou.
 *
 * ------------------------------------------------------------------- e "dispensar" nao existe
 *
 * O desenho poe um "Dispensar" ao lado de "Registrar a dose". Guardar essa dispensa pediria uma
 * tabela que nao existe, e um botao que esquece no recarregamento e pior do que nenhum botao — e a
 * mesma razao pela qual a busca e o marcador de avisos ficam desabilitados com o motivo em vez de
 * fingirem funcionar. <b>Mas o gesto que ele queria ja existe em outro lugar</b>: silenciar a
 * pendencia, no feed, e a tela diz onde.
 */

export const Route = createFileRoute("/animais/$animalId_/previsao")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Previsao,
});

/** Quantas semanas tem um mes, em media. 365 / 12 / 7 — usado para o valor de um dia combinado. */
const SEMANAS_POR_MES = 4.348;

function Previsao() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const previsao = usePrevisaoDeCusto(animalId);
  const matriculas = useMatriculasDoAnimal(animalId);

  const nome = animal.data?.name ?? "";
  const dados = previsao.data;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", display: "grid", gridTemplateColumns: "minmax(0, 820px) minmax(0, 480px)", gap: "24px", alignItems: "start" }}>

        <div style={{ background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "36px 40px 40px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "28px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "previsao.titulo" }, { nome })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 28px", maxWidth: "60ch" }}>
            {intl.formatMessage({ id: "previsao.apoio" })}
          </p>

          {previsao.isError ? (
            <ErroDeCarga
              oQue={intl.formatMessage({ id: "previsao.oQue" })}
              erro={previsao.error}
              aoTentarDeNovo={() => void previsao.refetch()}
              carregando={previsao.isFetching}
            />
          ) : previsao.isPending || dados === undefined ? (
            <Carregando oQue={intl.formatMessage({ id: "previsao.oQue" })} />
          ) : (dados.items ?? []).length === 0 ? (
            <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "previsao.vazia" }, { nome })}
            </div>
          ) : (
            <>
              <AsLinhas previsao={dados} />
              <ALeitura
                animalId={animalId}
                nome={nome}
                previsao={dados}
                matriculas={matriculas.data ?? []}
              />
            </>
          )}

          <div style={{ marginTop: "24px" }}>
            <Link to="/animais/$animalId/custo" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "previsao.verOQueCustou" }, { nome })}
            </Link>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <Cartao titulo={intl.formatMessage({ id: "previsao.diferente.titulo" })}>
            {intl.formatMessage({ id: "previsao.diferente.texto" })}
          </Cartao>

          {/*
           * O LIMITE DA LEITURA, e ele fica na tela e nao so no codigo: e a fronteira entre
           * aritmetica e prognostico, e quem le precisa saber que o produto para ali.
           */}
          <Cartao titulo={intl.formatMessage({ id: "previsao.limite.titulo" })}>
            {intl.formatMessage({ id: "previsao.limite.texto" })}
          </Cartao>

          <Cartao titulo={intl.formatMessage({ id: "previsao.paraQuemRegistra.titulo" })}>
            {intl.formatMessage({ id: "previsao.paraQuemRegistra.texto" })}
          </Cartao>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/**
 * As linhas, e o total.
 *
 * <b>O total soma so o que tem preco</b>, e a linha abaixo dele diz quantas ficaram de fora. Um
 * total que fingisse cobrir tudo seria um numero autoritario e menor que a verdade — e o tutor
 * planejaria por ele.
 */
function AsLinhas({ previsao }: { previsao: PrevisaoDeCusto }) {
  const intl = useIntl();
  const itens = previsao.items ?? [];
  const semValor = previsao.itemsWithoutAmount ?? 0;

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden", marginBottom: "24px" }}>
      {itens.map((item, indice) => (
        <div
          key={`${item.kind}-${item.sourceId ?? indice}`}
          style={{ display: "grid", gridTemplateColumns: "130px 1fr 130px", gap: "16px", padding: "13px 24px", alignItems: "center", fontSize: "15px", minHeight: "60px", borderTop: indice === 0 ? "none" : "1px solid oklch(0.95 0.005 150)" }}
        >
          <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
            {quando(item, intl)}
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <Marcador item={item} />
            <span>{descricao(item, intl)}</span>
          </div>

          {/*
           * "—" e nao "R$ 0": a linha sem preco e uma linha que ninguem informou, e zero
           * afirmaria que ela e de graca. "Se ninguem informou, o evento aparece sem valor, e
           * isso nao e erro."
           */}
          <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", textAlign: "right", color: item.amount === undefined ? "oklch(0.6 0.015 150)" : "inherit" }}>
            {item.amount === undefined ? "—" : dinheiro(intl, item.amount)}
          </div>
        </div>
      ))}

      <div style={{ display: "grid", gridTemplateColumns: "130px 1fr 130px", gap: "16px", padding: "16px 24px", alignItems: "center", fontSize: "16px", minHeight: "60px", background: "oklch(0.975 0.004 150)", borderTop: "1px solid oklch(0.95 0.005 150)" }}>
        <div></div>
        <div style={{ fontWeight: 500 }}>
          {intl.formatMessage({ id: "previsao.total" })}
          {semValor > 0 && (
            <div style={{ fontSize: "13px", fontWeight: 400, color: "oklch(0.5 0.015 150)", marginTop: "3px" }}>
              {intl.formatMessage({ id: "previsao.total.parcial" }, { quantas: semValor })}
            </div>
          )}
        </div>
        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "17px", fontWeight: 500, textAlign: "right" }}>
          {dinheiro(intl, previsao.total)}
        </div>
      </div>
    </div>
  );
}

/**
 * "Uma leitura do Petfy."
 *
 * <b>Ela so aparece quando os fatos a sustentam</b>, e nao sempre: precisa de uma dose atrasada E de
 * uma matricula viva em que a comprovacao impeca a entrada. Uma leitura que aparecesse sem isso
 * seria conselho generico, e conselho generico e o que faz a pessoa parar de ler.
 *
 * <b>A aritmetica e sobre o que ESTE tutor ja pode ver:</b> quanto vale um dia combinado do proprio
 * animal, derivado da mensalidade que ele paga e do numero de dias combinados. O numero do desenho
 * — o que aconteceu com outro animal da turma — e custo de outro animal, e nao ha como le-lo.
 */
function ALeitura({
  animalId,
  nome,
  previsao,
  matriculas,
}: {
  animalId: string;
  nome: string;
  previsao: PrevisaoDeCusto;
  matriculas: Matricula[];
}) {
  const intl = useIntl();
  const [registrando, setRegistrando] = useState(false);

  const atrasada = (previsao.items ?? []).find(
    (item) => item.overdue === true && item.kind === "DOSE_DE_VACINA",
  );

  /*
   * A matricula que a dose atrasada IMPEDE. O `blocks` vem pronto do servidor — "o produto responde
   * pela saude" —, e a tela nao recalcula aptidao nenhuma: se recalculasse, o dia em que divergisse
   * seria o dia em que ela prometeria uma entrada que a creche vai recusar.
   */
  const impedida = matriculas.find((matricula) =>
    (matricula.healthProof ?? []).some((linha) => linha.blocks === true),
  );

  if (atrasada === undefined || impedida === undefined) {
    return null;
  }

  const mensalidade = impedida.monthlyFee;
  const diasCombinados = (impedida.weekdays ?? []).length;

  /*
   * O VALOR DE UM DIA COMBINADO, e nao a diaria avulsa: a diaria e o que se paga por um dia EXTRA, e
   * o que se perde ao ser barrado e um dia que a mensalidade JA COBRIU. Usar a diaria aqui diria um
   * numero maior e errado.
   */
  const valorDoDia =
    mensalidade === undefined || diasCombinados === 0
      ? undefined
      : mensalidade / (diasCombinados * SEMANAS_POR_MES);

  return (
    <div style={{ border: "1px solid oklch(0.46 0.085 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "26px 28px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "14px" }}>
        <span aria-hidden style={{ width: "18px", height: "18px", borderRadius: "999px", border: "2px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
          <span style={{ width: "5px", height: "5px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></span>
        </span>
        <span style={{ fontSize: "13px", fontWeight: 500, letterSpacing: "0.03em", textTransform: "uppercase", color: "oklch(0.46 0.085 150)" }}>
          {intl.formatMessage({ id: "previsao.leitura" })}
        </span>
      </div>

      <div style={{ fontSize: "18px", lineHeight: 1.55, marginBottom: "18px", maxWidth: "60ch" }}>
        {atrasada.amount === undefined
          ? intl.formatMessage(
              { id: "previsao.leitura.semPreco" },
              { nome, dose: atrasada.description ?? "", creche: impedida.organizationName ?? "" },
            )
          : intl.formatMessage(
              { id: "previsao.leitura.comPreco" },
              {
                nome,
                dose: atrasada.description ?? "",
                valor: dinheiro(intl, atrasada.amount),
                creche: impedida.organizationName ?? "",
              },
            )}

        {valorDoDia !== undefined && (
          <>
            {" "}
            {intl.formatMessage(
              { id: "previsao.leitura.diaPerdido" },
              { valor: dinheiro(intl, valorDoDia), nome },
            )}
          </>
        )}
      </div>

      {/*
       * "O QUE GEROU ESTA LEITURA." Um numero previsto sem procedencia e um palpite com cara de
       * fato: quem le precisa poder discordar dele, e para discordar precisa saber de onde saiu.
       */}
      <div style={{ borderLeft: "2px solid oklch(0.85 0.012 150)", paddingLeft: "16px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", marginBottom: "20px" }}>
        <div style={{ fontSize: "12px", letterSpacing: "0.04em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "6px" }}>
          {intl.formatMessage({ id: "previsao.leitura.oQueGerou" })}
        </div>
        {[
          intl.formatMessage(
            { id: "previsao.leitura.fonte.dose" },
            {
              dose: atrasada.description ?? "",
              data:
                atrasada.dueOn === undefined
                  ? ""
                  : intl.formatDate(new Date(`${atrasada.dueOn}T12:00:00`), { dateStyle: "short" }),
            },
          ),
          intl.formatMessage(
            { id: "previsao.leitura.fonte.exigencia" },
            { creche: impedida.organizationName ?? "" },
          ),
          valorDoDia === undefined
            ? undefined
            : intl.formatMessage(
                { id: "previsao.leitura.fonte.combinado" },
                { valor: dinheiro(intl, mensalidade), dias: diasCombinados },
              ),
        ]
          .filter((parte) => parte !== undefined)
          .join(" · ")}
      </div>

      {registrando && atrasada.sourceId !== undefined ? (
        <RegistrarDose
          animalId={animalId}
          vaccineId={atrasada.sourceId}
          aoFechar={() => setRegistrando(false)}
        />
      ) : (
        <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", alignItems: "center" }}>
          <button
            type="button"
            onClick={() => setRegistrando(true)}
            style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", cursor: "pointer" }}
          >
            {intl.formatMessage({ id: "previsao.leitura.registrar" })}
          </button>

          {/*
           * NO LUGAR DO "DISPENSAR" DO DESENHO. Guardar a dispensa pediria uma tabela que nao
           * existe, e um botao que esquece no recarregamento e pior do que nenhum botao. Mas o
           * gesto que ele queria existe: silenciar a pendencia, no feed — e a tela diz onde.
           */}
          <span style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.5 0.015 150)", maxWidth: "38ch" }}>
            {intl.formatMessage({ id: "previsao.leitura.semDispensar" })}
          </span>
        </div>
      )}
    </div>
  );
}

/**
 * O marcador de cada linha.
 *
 * <b>A FORMA MUDA, e nao so a cor</b>, e e o desenho que faz assim: losango na vencida, anel na
 * proxima, circulo cheio no reforco distante, quadrado arredondado no que se repete. Uma tela que
 * distinguisse so por cor deixaria a diferenca entre "vencida" e "vem depois" invisivel para quem
 * nao separa vermelho de verde — e essa e a diferenca que muda o que a pessoa faz hoje.
 */
function Marcador({ item }: { item: NonNullable<PrevisaoDeCusto["items"]>[number] }) {
  const intl = useIntl();

  if (item.overdue === true) {
    return (
      <span
        title={intl.formatMessage({ id: "previsao.marcador.vencida" })}
        style={{ width: "12px", height: "12px", background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)", flex: "none" }}
      ></span>
    );
  }

  if (item.dueOn === undefined) {
    return (
      <span
        title={intl.formatMessage({ id: "previsao.marcador.todoMes" })}
        style={{ width: "11px", height: "11px", borderRadius: "3px", background: "oklch(0.46 0.085 150)", flex: "none" }}
      ></span>
    );
  }

  // dentro de tres meses: anel, que e o "esta chegando" do desenho
  const proxima = new Date(`${item.dueOn}T12:00:00`) <= new Date(Date.now() + 92 * 86_400_000);

  return proxima ? (
    <span
      title={intl.formatMessage({ id: "previsao.marcador.chegando" })}
      style={{ width: "12px", height: "12px", borderRadius: "999px", border: "3px solid oklch(0.62 0.11 70)", flex: "none" }}
    ></span>
  ) : (
    <span
      title={intl.formatMessage({ id: "previsao.marcador.distante" })}
      style={{ width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none" }}
    ></span>
  );
}

function Cartao({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "24px 26px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "12px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.7, color: "oklch(0.42 0.015 150)" }}>{children}</div>
    </div>
  );
}

/**
 * Quando.
 *
 * "este mes", "set 2026", "todo mes" — e nao uma data exata em tudo. O desenho escolhe a
 * granularidade pela distancia: para uma dose de marco de 2027, o dia nao muda o que ninguem faz.
 */
function quando(
  item: NonNullable<PrevisaoDeCusto["items"]>[number],
  intl: ReturnType<typeof useIntl>,
): string {
  if (item.dueOn === undefined) {
    return intl.formatMessage({ id: "previsao.quando.todoMes" });
  }

  const data = new Date(`${item.dueOn}T12:00:00`);
  const agora = new Date();

  if (item.overdue === true) {
    return intl.formatMessage({ id: "previsao.quando.esteMes" });
  }

  if (data.getFullYear() === agora.getFullYear() && data.getMonth() === agora.getMonth()) {
    return intl.formatMessage({ id: "previsao.quando.esteMes" });
  }

  return intl.formatDate(data, { month: "short", year: "numeric" });
}

/** O que a linha e, por extenso — a vencida ganha "vencida ha N dias", como no desenho. */
function descricao(
  item: NonNullable<PrevisaoDeCusto["items"]>[number],
  intl: ReturnType<typeof useIntl>,
): string {
  const nome = item.description ?? "";

  if (item.overdue === true && item.dueOn !== undefined) {
    const dias = Math.max(
      1,
      Math.round((Date.now() - new Date(`${item.dueOn}T12:00:00`).getTime()) / 86_400_000),
    );

    return intl.formatMessage({ id: "previsao.linha.vencida" }, { nome, dias });
  }

  if (item.kind === "CRECHE_MENSALIDADE") {
    return intl.formatMessage({ id: "previsao.linha.mensalidade" }, { onde: nome });
  }

  if (item.kind === "COMPRA_MENSAL") {
    return nome;
  }

  return item.dueOn === undefined
    ? nome
    : intl.formatMessage(
        { id: "previsao.linha.proxima" },
        { nome, data: intl.formatDate(new Date(`${item.dueOn}T12:00:00`), { day: "2-digit", month: "2-digit" }) },
      );
}

/** Dinheiro em real, sem centavos — os numeros do desenho sao "R$ 6.635". */
function dinheiro(intl: ReturnType<typeof useIntl>, valor: number | undefined): string {
  return intl.formatNumber(valor ?? 0, {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  });
}

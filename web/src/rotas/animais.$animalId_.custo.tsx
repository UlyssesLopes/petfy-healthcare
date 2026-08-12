import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import {
  useCustos,
  useResumoDeCusto,
  type CategoriaDeCusto,
  type Custo,
  type ResumoDeCusto,
} from "../dados/custo.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 37 · area do tutor — O custo do Code", de
 * `design/IdentidadeVisual/Telas Petfy - Custo do cuidado.dc.html`.
 *
 * <b>"Montado a partir do que ja esta registrado. Voce nao digitou nada disto duas vezes — cada
 * valor veio junto com o evento que o gerou."</b> Nao ha um campo nesta tela: ela e leitura de ponta
 * a ponta, e e por isso que ela existe. Um aplicativo de financas pede que voce digite tudo, e por
 * isso ninguem mantem.
 *
 * <b>SEM COMPARACAO E SEM JULGAMENTO, e a ausencia mora no servidor.</b> "Nenhuma tela aqui diz que
 * voce gasta mais ou menos que outros tutores, nem sugere trocar de clinica por preco. Quem cuida de
 * um animal doente ja tem o suficiente na cabeca." Nao existe consulta capaz de responder isso, e
 * nao havera — o repositorio de custo documenta essa recusa.
 *
 * ------------------------------------------------------------------ a cor da fatia, e um defeito
 *
 * <b>A COR SEGUE A CATEGORIA, E NUNCA A POSICAO NA LISTA.</b> As fatias sao ordenadas do maior para
 * o menor, e a ordem MUDA quando a pessoa troca "ultimos 12 meses" por "desde 2019" — pintar por
 * posicao faria a creche sair de verde-escuro e a saude entrar nele entre dois cliques da mesma
 * tela, com o mesmo verde significando duas coisas. O desenho nao podia mostrar isso, porque um
 * mockup so desenha um estado.
 *
 * <b>OUTRO e hachurado, e nao um quinto tom.</b> Os quatro tons do desenho sao uma rampa de
 * luminosidade na mesma matiz, e a rampa esta cheia: qualquer quinta cor plana cai entre dois
 * degraus e some ao lado deles — a primeira tentativa, um cinza, ficou a ΔE 3,5 de Alimentacao em
 * deuteranopia. Textura e um canal que sobrevive a daltonismo e a impressao, e a hachura ja e
 * vocabulario do desenho (o retrato do Teco, na Tela 39, e feita dela).
 *
 * <b>E o nome de cada fatia aparece SEMPRE ao lado da cor.</b> Nao e zelo: os dois tons mais claros
 * da rampa aprovada ficam abaixo do piso de contraste de 3:1 contra o branco, e a ΔE 12 um do outro
 * — abaixo do piso de 15 mesmo para quem ve todas as cores. A cor aqui e reforco, e a identidade e
 * o rotulo. Registrado: a paleta e a do desenho e nao foi alterada.
 */

export const Route = createFileRoute("/animais/$animalId_/custo")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: CustoDoAnimal,
});

type Janela = "DOZE_MESES" | "SEMPRE";

/**
 * A cor de cada categoria — os quatro tons do desenho, presos a CATEGORIA e nao a posicao.
 *
 * `HIGIENE` nasce sem ninguem para escrever nela: quem cobra banho e o petshop, do bloco 8. Ela ja
 * tem o tom guardado para o dia em que aparecer.
 */
const COR_DA_CATEGORIA: Record<CategoriaDeCusto, string> = {
  CRECHE: "oklch(0.46 0.085 150)",
  ALIMENTACAO: "oklch(0.60 0.07 150)",
  SAUDE: "oklch(0.72 0.05 150)",
  HIGIENE: "oklch(0.84 0.03 150)",
  // a hachura: "outro" nao e um degrau da rampa, e nao pode parecer um
  OUTRO:
    "repeating-linear-gradient(135deg, oklch(0.62 0.01 150) 0 4px, oklch(0.86 0.006 150) 4px 8px)",
};

function CustoDoAnimal() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const [janela, setJanela] = useState<Janela>("DOZE_MESES");

  const animal = useAnimal(animalId);
  const resumo = useResumoDeCusto(animalId, janela);
  const custos = useCustos(animalId);

  const nome = animal.data?.name ?? "";
  const dados = resumo.data;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "32px 40px 44px" }}>

        <div style={{ display: "flex", alignItems: "baseline", gap: "16px", marginBottom: "8px", flexWrap: "wrap" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: 0, letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "custo.titulo" }, { nome })}
          </h1>

          {/*
           * As duas janelas do desenho, e nada entre elas. Um seletor de periodo aberto convidaria
           * "ultimos 90 dias", e ai a media mensal passaria a comparar recortes que ninguem
           * desenhou.
           */}
          <div role="group" aria-label={intl.formatMessage({ id: "custo.janela" })} style={{ display: "flex", gap: "8px" }}>
            <Aba marcada={janela === "DOZE_MESES"} aoClicar={() => setJanela("DOZE_MESES")}>
              {intl.formatMessage({ id: "custo.janela.dozeMeses" })}
            </Aba>
            <Aba marcada={janela === "SEMPRE"} aoClicar={() => setJanela("SEMPRE")}>
              {dados?.firstYear === undefined
                ? intl.formatMessage({ id: "custo.janela.sempre" })
                : intl.formatMessage({ id: "custo.janela.desde" }, { ano: dados.firstYear })}
            </Aba>
          </div>
        </div>

        <p style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 28px", maxWidth: "70ch" }}>
          {intl.formatMessage({ id: "custo.apoio" })}
        </p>

        {resumo.isError ? (
          <ErroDeCarga
            oQue={intl.formatMessage({ id: "custo.oQue" })}
            erro={resumo.error}
            aoTentarDeNovo={() => void resumo.refetch()}
            carregando={resumo.isFetching}
          />
        ) : resumo.isPending || dados === undefined ? (
          <Carregando oQue={intl.formatMessage({ id: "custo.oQue" })} />
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "minmax(0, 1fr) minmax(0, 400px)", gap: "32px", alignItems: "start" }}>
            <div>
              <Indicadores resumo={dados} janela={janela} />
              <OndeFoi resumo={dados} nome={nome} />
              <CadaValor
                custos={custos.data ?? []}
                desde={dados.from}
                carregando={custos.isPending}
              />
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              <QuemPagou resumo={dados} />

              {/*
               * "Sem comparacao, sem julgamento" e uma promessa que a tela faz e o servidor cumpre:
               * nao ha rota que agregue preco por organizacao, e nao havera.
               */}
              <Cartao titulo={intl.formatMessage({ id: "custo.semJulgamento.titulo" })} destacado>
                {intl.formatMessage({ id: "custo.semJulgamento.texto" })}
              </Cartao>

              <Cartao titulo={intl.formatMessage({ id: "custo.deOndeVem.titulo" })}>
                {intl.formatMessage({ id: "custo.deOndeVem.texto" })}
              </Cartao>
            </div>
          </div>
        )}

        {/*
         * O caminho para a Tela 38 fica DEPOIS dos numeros, e nao antes: "quanto custou" e a
         * pergunta que a pessoa veio fazer, e "o que vem pela frente" e a que ela passa a ter
         * depois de ler a resposta.
         */}
        <div style={{ marginTop: "28px", display: "flex", gap: "24px", flexWrap: "wrap" }}>
          <Link to="/animais/$animalId/previsao" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
            {intl.formatMessage({ id: "custo.verPrevisao" }, { nome })}
          </Link>

          <Link to="/animais/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
            {intl.formatMessage({ id: "custo.voltar" }, { nome })}
          </Link>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/**
 * Os tres numeros do topo.
 *
 * <b>O terceiro nao muda quando a janela muda</b>, e por isso ele vem do servidor separado do total
 * do recorte: e o "desde 2019" do desenho, que fica de pe ao lado de "ultimos 12 meses"
 * selecionado.
 */
function Indicadores({ resumo, janela }: { resumo: ResumoDeCusto; janela: Janela }) {
  const intl = useIntl();

  return (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(3, minmax(0, 1fr))", gap: "16px", marginBottom: "24px" }}>
      <Indicador
        rotulo={intl.formatMessage({
          id: janela === "SEMPRE" ? "custo.indicador.noPeriodo" : "custo.indicador.dozeMeses",
        })}
        valor={dinheiro(intl, resumo.total)}
      />
      <Indicador
        rotulo={intl.formatMessage({ id: "custo.indicador.porMes" })}
        valor={dinheiro(intl, resumo.monthlyAverage)}
      />
      <Indicador
        rotulo={
          resumo.firstYear === undefined
            ? intl.formatMessage({ id: "custo.indicador.sempre" })
            : intl.formatMessage({ id: "custo.indicador.desde" }, { ano: resumo.firstYear })
        }
        valor={dinheiro(intl, resumo.totalEver)}
      />
    </div>
  );
}

function Indicador({ rotulo, valor }: { rotulo: string; valor: string }) {
  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
      <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginBottom: "8px" }}>{rotulo}</div>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, letterSpacing: "-0.02em" }}>
        {valor}
      </div>
    </div>
  );
}

/**
 * "Onde foi": a barra e a legenda.
 *
 * <b>O percentual e calculado AQUI, e nao vem do servidor</b> — de propósito. A fatia e `amount`
 * sobre `total`, e os dois chegam no mesmo corpo: se o servidor mandasse a razao junto, o dia em que
 * ela divergisse da largura desenhada seria o dia em que o tutor leria "38%" ao lado de uma barra de
 * outro tamanho. Uma conta, uma fonte.
 */
function OndeFoi({ resumo, nome }: { resumo: ResumoDeCusto; nome: string }) {
  const intl = useIntl();

  const fatias = resumo.byCategory ?? [];
  const total = Number(resumo.total ?? 0);

  if (fatias.length === 0) {
    return (
      <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", marginBottom: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
        {intl.formatMessage({ id: "custo.ondeFoi.vazio" }, { nome })}
      </div>
    );
  }

  const share = (valor: number) => (total === 0 ? 0 : (valor / total) * 100);
  const menor = fatias[fatias.length - 1];

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden", marginBottom: "24px" }}>
      <div style={{ padding: "20px 24px 16px", borderBottom: "1px solid oklch(0.94 0.006 150)" }}>
        <Rotulo>{intl.formatMessage({ id: "custo.ondeFoi" })}</Rotulo>

        {/*
         * A barra: 2px de folga entre as fatias, para duas cores vizinhas da mesma rampa nao se
         * encostarem e virarem um bloco so. `aria-hidden` porque a legenda abaixo diz o mesmo em
         * texto — a barra e o desenho do numero, e nao o numero.
         */}
        <div aria-hidden style={{ display: "flex", height: "14px", borderRadius: "4px", overflow: "hidden", gap: "2px", marginBottom: "16px" }}>
          {fatias.map((fatia) => (
            <div
              key={fatia.category}
              style={{
                width: `${share(Number(fatia.amount ?? 0))}%`,
                background: COR_DA_CATEGORIA[fatia.category ?? "OUTRO"],
              }}
            ></div>
          ))}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "11px", fontSize: "15px" }}>
          {fatias.map((fatia) => (
            <div key={fatia.category} style={{ display: "grid", gridTemplateColumns: "auto 1fr auto auto", gap: "12px", alignItems: "center" }}>
              <span
                aria-hidden
                style={{
                  width: "11px",
                  height: "11px",
                  borderRadius: "3px",
                  flex: "none",
                  background: COR_DA_CATEGORIA[fatia.category ?? "OUTRO"],
                }}
              ></span>
              <span>{intl.formatMessage({ id: `custo.categoria.${fatia.category}` })}</span>
              <span style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                {intl.formatNumber(share(Number(fatia.amount ?? 0)) / 100, {
                  style: "percent",
                  maximumFractionDigits: 0,
                })}
              </span>
              <span style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", minWidth: "90px", textAlign: "right" }}>
                {dinheiro(intl, fatia.amount)}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/*
       * A frase do desenho, e ela DEPENDE DA ORDEM: "saude e o menor pedaco do gasto do Code — e e o
       * unico que cresce sozinho quando e adiado. Os outros tres sao escolha sua."
       *
       * So aparece quando saude e de fato a menor fatia. Escrita fixa, ela mentiria no mes em que a
       * saude fosse o maior gasto — que e justamente o mes em que o animal esta doente.
       */}
      {menor?.category === "SAUDE" && fatias.length > 1 && (
        <div style={{ padding: "16px 24px", fontSize: "14px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "custo.saudeEMenor" }, { nome, quantos: fatias.length - 1 })}
        </div>
      )}
    </div>
  );
}

/**
 * "Cada valor veio de um evento."
 *
 * <b>Recortada pelo MESMO corte que gerou o total</b>, com o instante que o servidor devolveu. Uma
 * lista da vida inteira embaixo de um total de 12 meses daria ao tutor dois numeros e nenhuma forma
 * de saber em qual acreditar.
 */
function CadaValor({
  custos,
  desde,
  carregando,
}: {
  custos: Custo[];
  desde: string | undefined;
  carregando: boolean;
}) {
  const intl = useIntl();

  const doRecorte = custos.filter(
    (custo) =>
      desde === undefined ||
      (custo.occurredAt !== undefined && custo.occurredAt >= desde),
  );

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
      <div style={{ padding: "16px 24px", borderBottom: "1px solid oklch(0.94 0.006 150)" }}>
        <Rotulo>{intl.formatMessage({ id: "custo.cadaValor" })}</Rotulo>
      </div>

      {carregando ? (
        <div style={{ padding: "20px 24px" }}>
          <Carregando oQue={intl.formatMessage({ id: "custo.oQue.lista" })} quantos={3} />
        </div>
      ) : doRecorte.length === 0 ? (
        <div style={{ padding: "20px 24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "custo.cadaValor.vazio" })}
        </div>
      ) : (
        doRecorte.map((custo, indice) => (
          <div
            key={custo.animalCostId}
            style={{ display: "grid", gridTemplateColumns: "110px 1fr auto 130px", gap: "16px", padding: "13px 24px", alignItems: "center", fontSize: "15px", minHeight: "58px", borderTop: indice === 0 ? "none" : "1px solid oklch(0.95 0.005 150)" }}
          >
            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
              {custo.occurredAt === undefined
                ? "—"
                : intl.formatDate(new Date(custo.occurredAt), { dateStyle: "short" })}
            </div>

            <div>
              {custo.description}
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                {origem(custo, intl)}
              </div>
            </div>

            {/*
             * O quadradinho da categoria. `title` e nao so cor: os dois tons mais claros da rampa
             * ficam a ΔE 12 um do outro, e aqui — diferente da legenda — nao ha rotulo ao lado.
             */}
            <span
              title={intl.formatMessage({ id: `custo.categoria.${custo.category}` })}
              style={{
                width: "11px",
                height: "11px",
                borderRadius: "3px",
                flex: "none",
                background: COR_DA_CATEGORIA[custo.category ?? "OUTRO"],
              }}
            ></span>

            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px", textAlign: "right" }}>
              {dinheiro(intl, custo.amount)}
            </div>
          </div>
        ))
      )}
    </div>
  );
}

/**
 * "Quem pagou o que."
 *
 * <b>A linha sem nome aparece, e nao e escondida.</b> Sem ela a soma das linhas nao fecharia com o
 * total logo acima, e o cartao pareceria dizer que o resto do dinheiro nao existiu. O produto nao
 * sabe quem pagou uma consulta — quem registrou foi a veterinaria, e ela informou o valor, nao o
 * pagador — e a tela diz isso em vez de pedir que o tutor preencha depois.
 */
function QuemPagou({ resumo }: { resumo: ResumoDeCusto }) {
  const intl = useIntl();
  const linhas = resumo.byPayer ?? [];

  if (linhas.length === 0) {
    return null;
  }

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
      <Rotulo>{intl.formatMessage({ id: "custo.quemPagou" })}</Rotulo>

      <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
        {linhas.map((linha) => (
          <div key={linha.personName ?? "sem-nome"} style={{ display: "flex", alignItems: "center", gap: "12px" }}>
            <span
              aria-hidden
              style={{ width: "32px", height: "32px", borderRadius: "999px", flex: "none", background: linha.personName === undefined ? "oklch(0.94 0.006 150)" : "oklch(0.90 0.03 150)", border: linha.personName === undefined ? "1px dashed oklch(0.78 0.012 150)" : "none" }}
            ></span>

            <div style={{ flex: "1", minWidth: 0 }}>
              <div style={{ fontSize: "15px", fontWeight: 500 }}>
                {linha.personName ?? intl.formatMessage({ id: "custo.quemPagou.semNome" })}
              </div>
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                {linha.personName === undefined
                  ? intl.formatMessage({ id: "custo.quemPagou.semNome.detalhe" })
                  : (linha.categories ?? [])
                      .map((categoria) =>
                        intl.formatMessage({ id: `custo.categoria.curta.${categoria}` }),
                      )
                      .join(", ")}
              </div>
            </div>

            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "15px" }}>
              {dinheiro(intl, linha.amount)}
            </div>
          </div>
        ))}
      </div>

      <div style={{ fontSize: "14px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)", marginTop: "14px", borderTop: "1px solid oklch(0.94 0.006 150)", paddingTop: "14px" }}>
        {intl.formatMessage({ id: "custo.quemPagou.soMostra" })}
      </div>
    </div>
  );
}

function Aba({
  children,
  marcada,
  aoClicar,
}: {
  children: ReactNode;
  marcada: boolean;
  aoClicar: () => void;
}) {
  return (
    <button
      type="button"
      aria-pressed={marcada}
      onClick={aoClicar}
      style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: marcada ? 500 : 400, padding: "8px 14px", minHeight: "40px", display: "flex", alignItems: "center", borderRadius: "8px", cursor: "pointer", border: `1px solid ${marcada ? "oklch(0.46 0.085 150)" : "oklch(0.86 0.012 150)"}`, color: marcada ? "oklch(0.46 0.085 150)" : "oklch(0.42 0.015 150)", background: "oklch(1 0 0)" }}
    >
      {children}
    </button>
  );
}

function Cartao({
  titulo,
  children,
  destacado = false,
}: {
  titulo: string;
  children: ReactNode;
  destacado?: boolean;
}) {
  return (
    <div style={{ border: `1px solid ${destacado ? "oklch(0.88 0.02 150)" : "oklch(0.90 0.008 150)"}`, background: destacado ? "oklch(0.98 0.008 150)" : "oklch(1 0 0)", borderRadius: "12px", padding: "22px 24px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "10px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.65, color: destacado ? "oklch(0.35 0.018 150)" : "oklch(0.42 0.015 150)" }}>
        {children}
      </div>
    </div>
  );
}

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
      {children}
    </div>
  );
}

/** Dinheiro em real. Sem centavos: os numeros do desenho sao "R$ 4.180", e nao "R$ 4.180,00". */
function dinheiro(intl: ReturnType<typeof useIntl>, valor: number | undefined): string {
  return intl.formatNumber(valor ?? 0, {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  });
}

/**
 * De onde o valor veio: quem registrou, e por qual organizacao.
 *
 * "Cada valor tem um evento por tras, com autor e data — e por isso pode ser contestado como
 * qualquer outro registro." Sem organizacao, foi o proprio tutor: "lancado por voce".
 */
function origem(custo: Custo, intl: ReturnType<typeof useIntl>): string {
  if (custo.organizationName !== undefined) {
    return [custo.recordedByName, custo.organizationName]
      .filter((parte) => parte !== undefined && parte !== "")
      .join(", ");
  }

  return intl.formatMessage({ id: "custo.origem.voce" });
}

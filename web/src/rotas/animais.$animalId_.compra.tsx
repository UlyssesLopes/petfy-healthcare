import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar } from "../componentes/Estados.tsx";
import { useAnimal } from "../dados/carteira.ts";
import { useLancarCusto, type CategoriaDeCusto } from "../dados/custo.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 42 · na mao do tutor — O que voce compra por fora", de
 * `design/IdentidadeVisual/Telas Petfy - Por onde o valor entra.dc.html`.
 *
 * <b>E O UNICO FORMULARIO DE DINHEIRO EM TODO O PETFY, e o desenho explica por que ele existe:</b>
 * "tudo que acontece numa organizacao entra sozinho, porque alguem ja estava registrando o evento.
 * Racao e coisas de mercado nao tem organizacao por tras — so existem se o tutor lancar."
 *
 * <b>TRES TOQUES, e o numero e a especificacao.</b> "Quanto mais campos, menos gente lanca, e menos
 * verdadeiro fica o custo." Por isso "o que foi" sao tres botoes e nao um campo de texto: escrever
 * "racao premium 15kg" e o quarto toque que faz a pessoa desistir. O preco disso e que uma compra
 * marcada como "Outro" chega ao custo dizendo so "Outro" — e o desenho aceita esse preco de olhos
 * abertos, porque uma compra lancada vale mais do que uma compra bem descrita que ninguem lancou.
 *
 * <b>"DURA CERCA DE UM MES" E O CAMPO MAIS IMPORTANTE DA TELA</b>, e nao parece. "Essa caixinha e o
 * que transforma uma compra avulsa em custo mensal previsivel — e e tambem o que permite ao abrigo
 * dizer ao adotante do Teco que a racao dele custa R$ 190 por mes, todo mes. Sem ela, o produto so
 * saberia somar o passado."
 *
 * <b>O QUE NAO EXISTE AQUI, e nao por falta de tempo:</b> nenhuma integracao com banco ou cartao —
 * "o Petfy nao olha sua conta"; nenhum orcamento, meta ou aviso de que passou do limite; nenhuma
 * loja, link de compra ou sugestao de racao mais barata. "Tudo alem disso seria outro produto
 * morando dentro deste."
 */

export const Route = createFileRoute("/animais/$animalId_/compra")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Compra,
});

/**
 * "O que foi": tres botoes, e nao um campo de texto.
 *
 * <b>Cada botao carrega a CATEGORIA, e nao so o texto.</b> Foi o bloco 4 que mostrou por que isso
 * importa: o "onde foi" da Tela 37 poe racao e remedio em fatias diferentes, e os dois saem do
 * mesmo `kind` COMPRA — entao a fatia so pode vir de quem tocou no botao. Guardar apenas a palavra
 * obrigaria a leitura a casar texto para decidir o grafico, que e o LIKE que este projeto recusa
 * desde os dias da semana.
 *
 * Nao ha catalogo atras disso, e nao deve haver: um catalogo pediria manutencao e daria ao tutor
 * uma lista para ler antes de tocar.
 */
const O_QUE_FOI = [
  { chave: "racao", categoria: "ALIMENTACAO" },
  { chave: "remedio", categoria: "SAUDE" },
  { chave: "outro", categoria: "OUTRO" },
] as const satisfies readonly { chave: string; categoria: CategoriaDeCusto }[];

type OQueFoi = (typeof O_QUE_FOI)[number]["chave"];

function Compra() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const lancar = useLancarCusto();

  const [oQueFoi, setOQueFoi] = useState<OQueFoi>("racao");
  const [valor, setValor] = useState("");
  /*
   * O intervalo em MESES, e zero e "nao se repete". A caixinha booleana anterior nao sabia dizer
   * "dura dois meses", e marcar afirmava o dobro do que o tutor gasta.
   */
  const [duraMeses, setDuraMeses] = useState(0);

  /* Hoje, porque e o caso comum de quem lanca a nota do mercado ao voltar dele. */
  const hoje = new Date().toISOString().slice(0, 10);
  const [quando, setQuando] = useState(hoje);
  const [lancado, setLancado] = useState<string | undefined>(undefined);

  const nome = animal.data?.name ?? "";
  const quanto = Number(valor.replace(/[^\d,.-]/g, "").replace(",", "."));
  const podeLancar = valor.trim() !== "" && !Number.isNaN(quanto) && quanto >= 0 && !lancar.isPending;

  const enviar = () => {
    if (!podeLancar) {
      return;
    }

    setLancado(undefined);

    lancar.mutate(
      {
        animalId,
        descricao: intl.formatMessage({ id: `compra.oQue.${oQueFoi}` }),
        valor: quanto,
        /* COMPRA e o unico tipo que nasce de um gesto do tutor: os outros tres saem de eventos
           que uma organizacao ja estava registrando. */
        tipo: "COMPRA",
        categoria: O_QUE_FOI.find((opcao) => opcao.chave === oQueFoi)!.categoria,
        // COBRIR e REPETIR sao coisas diferentes desde a V47: o intervalo e o que volta
        duraMeses: duraMeses === 0 ? undefined : duraMeses,
        // meia-noite do dia escolhido: o gasto e do DIA, e a hora nao e um fato que alguem saiba
        quando: quando + "T00:00:00",
      },
      {
        onSuccess: () => {
          setLancado(intl.formatNumber(quanto, { style: "currency", currency: "BRL" }));
          /* O formulario volta ao zero porque quem lanca racao lanca remedio em seguida — e
             deixar o valor anterior no campo e como o segundo lancamento sai errado. */
          setValor("");
          setDuraMeses(0);
          setQuando(hoje);
        },
      },
    );
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1160px", margin: "0 auto", display: "grid", gridTemplateColumns: "minmax(0, 390px) minmax(0, 700px)", gap: "32px", alignItems: "start" }}>

        {/* ================================================================ os tres toques */}
        <div style={{ background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
          <div style={{ padding: "16px 20px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)", display: "flex", alignItems: "center", gap: "10px" }}>
            <span aria-hidden style={{ width: "24px", height: "24px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <span style={{ width: "7px", height: "7px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></span>
            </span>
            <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "16px", fontWeight: 600 }}>Petfy</span>
            <span style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginLeft: "auto" }}>{nome}</span>
          </div>

          <div style={{ padding: "22px 20px 26px", display: "flex", flexDirection: "column", gap: "16px" }}>
            <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, margin: 0 }}>
              {intl.formatMessage({ id: "compra.titulo" })}
            </h1>

            <div>
              <div style={rotulo}>{intl.formatMessage({ id: "compra.oQue" })}</div>

              <div role="group" aria-label={intl.formatMessage({ id: "compra.oQue" })} style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                {O_QUE_FOI.map((opcao) => {
                  const marcada = opcao.chave === oQueFoi;

                  return (
                    <button
                      key={opcao.chave}
                      type="button"
                      aria-pressed={marcada}
                      onClick={() => setOQueFoi(opcao.chave)}
                      style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: marcada ? 500 : 400, height: "48px", padding: "0 16px", display: "flex", alignItems: "center", borderRadius: "8px", cursor: "pointer", border: `1px solid ${marcada ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, background: marcada ? "oklch(0.96 0.02 150)" : "oklch(1 0 0)", color: marcada ? "oklch(0.38 0.07 150)" : "oklch(0.25 0.02 150)" }}
                    >
                      {intl.formatMessage({ id: `compra.oQue.${opcao.chave}` })}
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <label htmlFor="valor" style={rotulo}>
                {intl.formatMessage({ id: "compra.valor" })}
              </label>
              <input
                id="valor"
                type="text"
                inputMode="decimal"
                value={valor}
                onChange={(evento) => setValor(evento.target.value)}
                placeholder="289,00"
                style={{ fontFamily: "'DM Mono', monospace", border: "1px solid oklch(0.46 0.085 150)", borderRadius: "4px", padding: "15px 14px", fontSize: "18px", minHeight: "54px", width: "100%", background: "oklch(1 0 0)" }}
              />
            </div>

            {/*
             * QUANDO FOI.
             *
             * <b>O campo nao existia, e a tela sempre lancava AGORA.</b> Quem lanca a nota do mercado
             * a noite registrava hoje; quem lanca a de sabado na segunda registrava errado — e a
             * previsao, que le "quando foi a ultima vez", herdava o erro.
             *
             * Vem preenchido com hoje porque esse e o caso comum, e nao pode ser no futuro: uma compra
             * que ainda nao aconteceu nao e um gasto.
             */}
            <div>
              <label htmlFor="quando" style={rotulo}>
                {intl.formatMessage({ id: "compra.quando" })}
              </label>
              <input
                id="quando"
                type="date"
                value={quando}
                max={hoje}
                onChange={(evento) => setQuando(evento.target.value)}
                style={{ border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", width: "100%", background: "oklch(1 0 0)", fontFamily: "inherit" }}
              />
            </div>

            {/*
             * O CAMPO MAIS IMPORTANTE DA TELA, e ele era uma caixinha booleana ate a V47.
             *
             * "Dura cerca de um mes" gravava `recurrence = MENSAL`, e com isso "a racao dura um mes" e
             * "a mensalidade chega todo mes" viraram o mesmo fato. Enquanto so existia um mes, os dois
             * coincidiam por acidente. A racao que dura DOIS meses nao tinha resposta: marcar dizia
             * "todo mes", que e o dobro do que o tutor gasta — e a previsao planejava para cima.
             *
             * Agora a pergunta e o intervalo, e "nao se repete" e a primeira opcao porque a compra
             * avulsa e o caso mais comum.
             */}
            <div>
              <label htmlFor="dura" style={rotulo}>
                {intl.formatMessage({ id: "compra.dura" })}
              </label>
              <select
                id="dura"
                value={String(duraMeses)}
                onChange={(evento) => setDuraMeses(Number(evento.target.value))}
                style={{ border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", width: "100%", background: "oklch(1 0 0)", fontFamily: "inherit", cursor: "pointer" }}
              >
                <option value="0">{intl.formatMessage({ id: "compra.dura.naoSeRepete" })}</option>
                {[1, 2, 3, 4, 6, 12].map((meses) => (
                  <option key={meses} value={String(meses)}>
                    {intl.formatMessage({ id: "compra.dura.meses" }, { meses })}
                  </option>
                ))}
              </select>
              <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.55 }}>
                {duraMeses > 0
                  ? intl.formatMessage(
                      { id: "compra.dura.nota" },
                      { vezes: Math.floor(12 / duraMeses) },
                    )
                  : intl.formatMessage({ id: "compra.dura.nota.avulsa" })}
              </div>
            </div>

            {lancar.error !== null && lancar.error !== undefined && (
              <ErroAoGravar erro={lancar.error} oQue={intl.formatMessage({ id: "compra.oQueE" })} />
            )}

            <button
              type="button"
              disabled={!podeLancar}
              onClick={enviar}
              style={{ fontFamily: "inherit", fontSize: "17px", fontWeight: 500, color: "oklch(1 0 0)", background: podeLancar ? "oklch(0.46 0.085 150)" : "oklch(0.62 0.05 150)", border: "none", borderRadius: "8px", padding: "17px", minHeight: "58px", cursor: podeLancar ? "pointer" : "not-allowed" }}
            >
              {intl.formatMessage({ id: lancar.isPending ? "compra.lancando" : "compra.lancar" })}
            </button>

            {lancado !== undefined && !lancar.isPending && (
              <div role="status" style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.38 0.07 150)" }}>
                {intl.formatMessage({ id: "compra.lancado" }, { valor: lancado, nome })}
              </div>
            )}

            <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", lineHeight: 1.55 }}>
              {intl.formatMessage({ id: "compra.tresToques" }, { nome })}
            </div>
          </div>
        </div>

        {/* =========================================== por que isto existe, e o que nao existe */}
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <Cartao titulo={intl.formatMessage({ id: "compra.unico.titulo" })}>
            <p style={{ margin: "0 0 14px" }}>{intl.formatMessage({ id: "compra.unico.p1" })}</p>
            <p style={{ margin: 0 }}>{intl.formatMessage({ id: "compra.unico.p2" })}</p>
          </Cartao>

          <Cartao titulo={intl.formatMessage({ id: "compra.mensal.titulo" })}>
            {intl.formatMessage({ id: "compra.mensal.texto" })}
          </Cartao>

          <Cartao titulo={intl.formatMessage({ id: "compra.naoExiste.titulo" })}>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {["banco", "orcamento", "loja"].map((ausencia) => (
                <div key={ausencia} style={{ display: "flex", gap: "12px" }}>
                  {/* circulo TRACEJADO: ausencia declarada, e nao item de lista */}
                  <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", border: "2px dashed oklch(0.6 0.015 150)", flex: "none", marginTop: "5px" }}></span>
                  <span>{intl.formatMessage({ id: `compra.naoExiste.${ausencia}` })}</span>
                </div>
              ))}
            </div>

            <div style={{ borderTop: "1px solid oklch(0.94 0.006 150)", marginTop: "18px", paddingTop: "16px", color: "oklch(0.35 0.018 150)" }}>
              {intl.formatMessage({ id: "compra.naoExiste.fecho" })}
            </div>
          </Cartao>
        </div>
      </div>

      <div style={{ maxWidth: "1160px", margin: "26px auto 0" }}>
        <Link to="/animais/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
          {intl.formatMessage({ id: "compra.voltar" }, { nome })}
        </Link>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

const rotulo = {
  display: "block",
  fontSize: "13px",
  fontWeight: 500,
  color: "oklch(0.42 0.015 150)",
  marginBottom: "7px",
} as const;

function Cartao({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div style={{ background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "26px 28px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "20px", fontWeight: 500, marginBottom: "14px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.7, color: "oklch(0.42 0.015 150)" }}>{children}</div>
    </div>
  );
}

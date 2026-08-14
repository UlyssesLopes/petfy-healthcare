import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Marcador, legendaDaDose } from "../componentes/Vacinacao.tsx";
import type { EntradaDaLinha } from "../dados/animal.ts";
import { useLinhaDoTempo } from "../dados/animal.ts";
import type { Anexo, Condicao, Pesagem } from "../dados/carteira.ts";
import {
  useAnexos,
  useAnimal,
  useCarteira,
  useCondicoes,
  usePesagens,
  useRedeDeCuidado,
} from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 02 · web, tela grande — A vida do Code, carteira e linha do tempo", de
 * `design/IdentidadeVisual/Telas Petfy.dc.html`, com o backend ligado nela.
 *
 * <b>O MARKUP E O DA ENTREGA, e nao uma leitura dele.</b> Os estilos foram convertidos do
 * arquivo por script — `style="..."` vira `style={{...}}` e nada mais —, entao cada
 * padding, cada tamanho e cada cor abaixo esta como o desenho escreveu. O que mudou e so o
 * conteudo: onde havia "Code" ha `animal.name`, onde havia tres linhas de vacina ha o que
 * a API responde.
 *
 * Por isso o estilo e inline aqui, contra o resto do projeto: traduzir para classe abre
 * espaco entre o desenho aprovado e o que sobe.
 *
 * O `style-hover` e a unica coisa do arquivo que nao atravessa — nao e HTML, e um atributo
 * que o `support.js` do Claude Design interpreta em runtime. Vira o `useHover` abaixo, com
 * os mesmos dois valores que ele declarava.
 */

export const Route = createFileRoute("/animais/$animalId")({
  // Mesma guarda da home: o token vive em memoria, e esta tela so existe autenticada.
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: VidaDoAnimal,
});

function useHover() {
  const [sobre, setSobre] = useState(false);

  return {
    sobre,
    props: {
      onMouseEnter: () => setSobre(true),
      onMouseLeave: () => setSobre(false),
      onFocus: () => setSobre(true),
      onBlur: () => setSobre(false),
    },
  };
}

function VidaDoAnimal() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const rede = useRedeDeCuidado(animalId);
  const carteira = useCarteira(animalId);
  const condicoes = useCondicoes(animalId);
  const pesagens = usePesagens(animalId);
  const anexos = useAnexos(animalId);
  const linha = useLinhaDoTempo(animalId);

  const compartilhar = useHover();
  const [recorte, setRecorte] = useState<Recorte>("tudo");

  if (animal.isPending) {
    return (
      <div style={{ padding: "40px", fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
        {intl.formatMessage({ id: "animal.carregando" })}
      </div>
    );
  }

  if (animal.error !== null || animal.data === undefined) {
    return (
      <div style={{ padding: "40px", fontSize: "15px" }}>
        <div>{intl.formatMessage({ id: chaveDoErro(animal.error) })}</div>
        <Link to="/" style={{ color: "oklch(0.46 0.085 150)", display: "inline-block", marginTop: "16px" }}>
          {intl.formatMessage({ id: "animal.voltar" })}
        </Link>
      </div>
    );
  }

  const bicho = animal.data;
  const custodia = (rede.data ?? []).find((membro) => membro.holder === true);

  /*
   * A linha do tempo fechada (Tela 33).
   *
   * <b>UM CAMPO, e a ficha inteira continua igual.</b> "Sem tarja preta, sem laco, sem memorial. A
   * ficha fica igual as outras — so parou de pedir coisas." O que muda daqui para baixo e
   * exatamente isso: os botoes que PEDEM alguma coisa saem, e nada mais.
   */
  const encerrada = bicho.deceasedOn !== undefined && bicho.deceasedOn !== null;

  const identidade = [
    bicho.species === undefined ? undefined : intl.formatMessage({ id: `animal.especie.${bicho.species}` }),
    bicho.breed,
    bicho.gender === undefined ? undefined : intl.formatMessage({ id: `animal.genero.${bicho.gender}` }),
  ].filter((parte): parte is string => parte !== undefined && parte !== "");

  const entradas = linha.data ?? [];
  const visiveis = entradas.filter((entrada) => passaNoRecorte(entrada, recorte));
  const faixa = faixaDeAnos(entradas);

  return (
    <div style={{ padding: "40px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ padding: "36px 40px 28px", background: "oklch(1 0 0)", borderBottom: "1px solid oklch(0.90 0.008 150)", display: "grid", gridTemplateColumns: "auto 1fr auto", gap: "28px", alignItems: "center" }}>
          <div style={{ width: "96px", height: "96px", borderRadius: "999px", background: "repeating-linear-gradient(135deg, oklch(0.92 0.02 150) 0 8px, oklch(0.95 0.014 150) 8px 16px)" }}></div>
          <div>
            <h2 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "38px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>{bicho.name}</h2>
            <div style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)", display: "flex", flexWrap: "wrap", gap: "6px 20px" }}>
              {identidade.length > 0 && <span>{identidade.join(" · ")}</span>}
              {bicho.bornDate !== undefined && (
                <span>
                  {intl.formatMessage(
                    { id: "animal.nascido" },
                    { data: intl.formatDate(bicho.bornDate, { month: "2-digit", year: "numeric", timeZone: "UTC" }) },
                  )}
                </span>
              )}
              {bicho.microchipNumber !== undefined && (
                <span style={{ fontFamily: "'DM Mono', monospace" }}>
                  {intl.formatMessage({ id: "animal.microchip" }, { numero: microchipAgrupado(bicho.microchipNumber) })}
                </span>
              )}
              {bicho.generalRegistry !== undefined && (
                <span style={{ fontFamily: "'DM Mono', monospace" }}>
                  {intl.formatMessage({ id: "animal.rga" }, { numero: bicho.generalRegistry })}
                </span>
              )}
            </div>
            {custodia !== undefined && (
              <div style={{ fontSize: "14px", color: "oklch(0.45 0.015 150)", marginTop: "10px" }}>
                {intl.formatMessage(
                  { id: "animal.custodia" },
                  {
                    nome: <strong style={{ fontWeight: 500, color: "oklch(0.3 0.018 150)" }}>{custodia.name}</strong>,
                    desde: custodia.since === undefined ? "" : intl.formatDate(new Date(custodia.since), { dateStyle: "short" }),
                  },
                )}
              </div>
            )}

            {/*
             * A data do fim, como um fato a mais na mesma linha dos outros — e nao um aviso.
             *
             * Ela nao ganha cor, caixa nem icone: "a ficha fica igual as outras". O que ela faz e
             * responder, para quem abrir isto daqui a tres anos, por que a linha do tempo para em
             * agosto de 2026.
             */}
            {encerrada && (
              <div style={{ fontSize: "14px", color: "oklch(0.45 0.015 150)", marginTop: "6px" }}>
                {intl.formatMessage(
                  { id: "fim.ficha.encerradaEm" },
                  { data: intl.formatDate(bicho.deceasedOn!, { dateStyle: "long", timeZone: "UTC" }) },
                )}
              </div>
            )}
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {/*
             * ================================================ O QUE SOME QUANDO A LINHA FECHA
             *
             * "So parou de pedir coisas" e literal: o que sai daqui e o que PEDE alguma coisa do
             * tutor — registrar, compartilhar, ver o que a creche espera dele, lancar uma compra.
             * O custo FICA, porque ler o que ja foi gasto continua fazendo sentido, e a Tela 39 e
             * construida em cima disso.
             *
             * <b>Desabilitar em vez de esconder seria pior aqui</b>, e e a excecao a regra da
             * secao 06 da identidade. Um botao "Registrar" cinza com a frase "este animal morreu"
             * ao lado transformaria a ficha inteira num aviso de luto, que e exatamente o que o
             * desenho recusa.
             */}
            {!encerrada && (
              <>
                {/*
                 * ELE FICOU `disabled` ATE AGORA, e nao era descuido de estilo: era a porta de um
                 * caminho que nunca foi construido. O tutor nao tinha como registrar NADA no
                 * proprio animal — nem vacina, nem peso, nem observacao —, so discordar de um
                 * registro que ja existia. O unico lugar do produto que registrava era a tela da
                 * clinica, e os hooks do tutor nao eram chamados por rota nenhuma.
                 */}
                <Link
                  to="/animais/$animalId/registrar"
                  params={{ animalId }}
                  style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", textDecoration: "none", fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "12px 20px", minHeight: "44px", cursor: "pointer" }}
                >
                  {intl.formatMessage({ id: "animal.acao.registrar" })}
                </Link>
                <button
                  disabled
                  {...compartilhar.props}
                  style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: compartilhar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: `1px solid ${compartilhar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, borderRadius: "8px", padding: "12px 20px", minHeight: "44px", cursor: "pointer", opacity: 0.6 }}
                >
                  {intl.formatMessage({ id: "animal.acao.compartilhar" })}
                </button>

                {/*
                 * A Tela 10 vista pelo tutor: o que cada creche esta esperando dele. Fica aqui, no
                 * cabecalho do animal, porque a pergunta e sobre ESTE animal — e a resposta pode ser
                 * "falta a antirrabica em dia", que e coisa de agir hoje.
                 */}
                <Link
                  to="/animais/$animalId/matricula"
                  params={{ animalId }}
                  style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "12px 20px", minHeight: "44px", display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none" }}
                >
                  {intl.formatMessage({ id: "animal.acao.creches" })}
                </Link>

                {/*
                 * Tela 42, e o caminho fica AQUI porque e daqui que ele faz sentido: o unico
                 * lancamento manual do produto e sobre ESTE animal. "Racao e coisas de mercado nao tem
                 * organizacao por tras — so existem se o tutor lancar."
                 */}
                <Link
                  to="/animais/$animalId/compra"
                  params={{ animalId }}
                  style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "12px 20px", minHeight: "44px", display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none" }}
                >
                  {intl.formatMessage({ id: "animal.acao.compra" })}
                </Link>
              </>
            )}

            {/*
             * Tela 37. Fica ao lado de "lancar uma compra" de proposito: quem acabou de lancar
             * quer ver onde aquilo entrou, e quem olha o custo costuma descobrir ali que falta
             * lancar algo.
             */}
            <Link
              to="/animais/$animalId/custo"
              params={{ animalId }}
              style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "12px 20px", minHeight: "44px", display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none" }}
            >
              {intl.formatMessage({ id: "animal.acao.custo" })}
            </Link>
          </div>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "340px 1fr" }}>
          <div style={{ borderRight: "1px solid oklch(0.90 0.008 150)", padding: "28px 28px 40px", display: "flex", flexDirection: "column", gap: "26px" }}>
            <div>
              <Rotulo>{intl.formatMessage({ id: "animal.carteira.vacinacao" })}</Rotulo>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "15px" }}>
                {carteira.linhas.length === 0 ? (
                  <Vazio>{intl.formatMessage({ id: "animal.carteira.vacinacao.vazio" })}</Vazio>
                ) : (
                  carteira.linhas.map((dose) => (
                    <div key={dose.nome} style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                      <Marcador estado={dose.estado} />
                      <span style={{ flex: "1" }}>{dose.nome}</span>
                      <span style={{ color: dose.estado === "vencida" ? "oklch(0.45 0.13 30)" : "oklch(0.5 0.015 150)", fontSize: "14px" }}>
                        {legendaDaDose(dose, intl)}
                      </span>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div>
              <Rotulo>{intl.formatMessage({ id: "animal.carteira.condicoes" })}</Rotulo>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px", fontSize: "15px" }}>
                {condicoesAtivas(condicoes.data).length === 0 ? (
                  <Vazio>{intl.formatMessage({ id: "animal.carteira.condicoes.vazio" })}</Vazio>
                ) : (
                  condicoesAtivas(condicoes.data).map((condicao) => (
                    <div key={condicao.animalHealthConditionId}>
                      {condicao.description}{" "}
                      {condicao.since !== undefined && (
                        <span style={{ color: "oklch(0.5 0.015 150)", fontSize: "14px" }}>
                          {intl.formatMessage({ id: "animal.carteira.desde" }, { ano: condicao.since.slice(0, 4) })}
                        </span>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>

            <Peso pesagens={pesagens.data ?? []} />

            <div>
              <Rotulo>{intl.formatMessage({ id: "animal.carteira.anexos" })}</Rotulo>
              <div style={{ display: "flex", flexDirection: "column", gap: "8px", fontSize: "15px" }}>
                {(anexos.data ?? []).length === 0 ? (
                  <Vazio>{intl.formatMessage({ id: "animal.carteira.anexos.vazio" })}</Vazio>
                ) : (
                  (anexos.data ?? []).map((anexo: Anexo) => (
                    <a key={anexo.attachmentId} href={`/attachments/${anexo.attachmentId}/content`}>
                      {anexo.description ?? anexo.originalFilename}
                    </a>
                  ))
                )}
              </div>
            </div>
          </div>

          <div style={{ padding: "28px 40px 48px" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "22px" }}>
              <h3 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "24px", fontWeight: 500, margin: 0 }}>
                {intl.formatMessage({ id: "animal.linha.titulo" })}
              </h3>
              <div style={{ display: "flex", gap: "8px" }}>
                {(["tudo", "vacinas", "atendimentos", "creche"] as const).map((opcao) => (
                  <button
                    key={opcao}
                    aria-pressed={recorte === opcao}
                    onClick={() => setRecorte(opcao)}
                    style={{ fontFamily: "inherit", background: "transparent", cursor: "pointer", fontSize: "14px", padding: "9px 14px", minHeight: "44px", display: "flex", alignItems: "center", border: `1px solid ${recorte === opcao ? "oklch(0.46 0.085 150)" : "oklch(0.86 0.012 150)"}`, color: recorte === opcao ? "oklch(0.46 0.085 150)" : "oklch(0.42 0.015 150)", borderRadius: "8px" }}
                  >
                    {intl.formatMessage({ id: `animal.linha.recorte.${opcao}` })}
                  </button>
                ))}
                {faixa !== undefined && (
                  <div style={{ fontSize: "14px", padding: "9px 14px", minHeight: "44px", display: "flex", alignItems: "center", border: "1px solid oklch(0.86 0.012 150)", borderRadius: "8px", color: "oklch(0.42 0.015 150)" }}>
                    {faixa.de === faixa.ate ? faixa.de : intl.formatMessage({ id: "animal.linha.faixa" }, faixa)}
                  </div>
                )}
              </div>
            </div>

            {linha.isPending ? (
              <Vazio>{intl.formatMessage({ id: "animal.linha.carregando" })}</Vazio>
            ) : visiveis.length === 0 ? (
              <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)", border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px" }}>
                {intl.formatMessage({ id: `animal.linha.vazio.${recorte}` })}
              </div>
            ) : (
              <div style={{ display: "grid", gridTemplateColumns: "128px 1fr" }}>
                {visiveis.map((entrada, indice) => (
                  <Evento key={entrada.eventId} entrada={entrada} ultimo={indice === visiveis.length - 1} animalId={animalId} />
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/*
       * ======================================================= encerrar a linha do tempo (Tela 33)
       *
       * <b>Fora do cartao, depois da vida inteira dele, e escrito como um link.</b> A posicao e a
       * decisao: um botao "O animal morreu" no cabecalho, ao lado de "Registrar", seria uma
       * pergunta feita a quem abriu a ficha para ver uma vacina — e um toque errado num lugar em
       * que o produto nao pode admitir toque errado.
       *
       * Some quando a linha ja fechou, pela mesma razao dos outros: nao ha o que oferecer.
       */}
      {!encerrada && (
        <div style={{ maxWidth: "1360px", margin: "28px auto 0", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
          <Link to="/animais/$animalId/fim" params={{ animalId }} style={{ color: "oklch(0.46 0.085 150)" }}>
            {intl.formatMessage({ id: "animal.acao.encerrar" }, { nome: bicho.name })}
          </Link>
        </div>
      )}
    </div>
  );
}

/* ---------------------------------------------------------------------------- pedacos */

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
      {children}
    </div>
  );
}

function Vazio({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

/* O `Marcador` e a `legendaDaDose` moravam aqui e viraram `componentes/Vacinacao.tsx`: a Tela 30
 * mostra a mesma carteira a clinica, e a segunda copia e que teria feito uma antirrabica vencida
 * aparecer com losango numa tela e com bolinha na outra. */

function condicoesAtivas(condicoes: Condicao[] | undefined): Condicao[] {
  return (condicoes ?? []).filter((condicao) => condicao.ativa !== false);
}


/**
 * As seis barras da Tela 02, com a ULTIMA em musgo.
 *
 * A leitura que interessa e "para onde isto esta indo", e uma serie com todas as barras
 * iguais nao conta essa historia. As alturas do desenho (46%, 52%, 58%, 70%, 82%, 100%)
 * eram exemplo: aqui elas saem da razao entre a pesagem e a maior da serie.
 */
function Peso({ pesagens }: { pesagens: Pesagem[] }) {
  const intl = useIntl();

  const ordenadas = [...pesagens]
    .filter((p): p is Pesagem & { weight: number } => typeof p.weight === "number")
    .sort((a, b) => (a.measuredAt ?? "").localeCompare(b.measuredAt ?? ""));

  const ultimas = ordenadas.slice(-6);
  const atual = ultimas[ultimas.length - 1];

  /*
   * A ESCALA NAO COMECA NO ZERO, e isso e o que faz a barra dizer alguma coisa.
   *
   * Escalando por `peso / maior`, uma serie de 7,5 a 8,4 kg vira barras de 89% a 100% —
   * seis retangulos praticamente iguais. O desenho vai de 46% a 100% para os mesmos
   * numeros: o que a barra mostra e a VARIACAO, nao a massa do animal.
   *
   * O piso de 45% e o do desenho. Com a serie inteira no mesmo peso nao ha variacao a
   * mostrar, e todas ficam cheias em vez de dividir por zero.
   */
  const PISO = 45;
  const maior = Math.max(...ultimas.map((p) => p.weight));
  const menor = Math.min(...ultimas.map((p) => p.weight));
  const amplitude = maior - menor;

  const alturaDe = (peso: number) =>
    amplitude === 0 ? 100 : Math.round(PISO + (100 - PISO) * ((peso - menor) / amplitude));

  return (
    <div>
      <Rotulo>{intl.formatMessage({ id: "animal.carteira.peso" })}</Rotulo>

      {atual === undefined ? (
        <Vazio>{intl.formatMessage({ id: "animal.carteira.peso.vazio" })}</Vazio>
      ) : (
        <>
          <div style={{ display: "flex", alignItems: "flex-end", gap: "6px", height: "76px", marginBottom: "10px" }}>
            {ultimas.map((pesagem, indice) => (
              <div
                key={pesagem.weightHistoryId ?? indice}
                style={{ flex: "1", height: `${alturaDe(pesagem.weight)}%`, background: indice === ultimas.length - 1 ? "oklch(0.46 0.085 150)" : "oklch(0.88 0.03 150)", borderRadius: "3px 3px 0 0" }}
              ></div>
            ))}
          </div>
          <div style={{ fontSize: "14px", color: "oklch(0.45 0.015 150)" }}>
            {intl.formatMessage(
              { id: "animal.carteira.peso.resumo" },
              {
                peso: intl.formatNumber(atual.weight, { maximumFractionDigits: 1 }),
                data: atual.measuredAt === undefined ? "" : intl.formatDate(atual.measuredAt, { dateStyle: "short", timeZone: "UTC" }),
                total: ordenadas.length,
              },
            )}
          </div>
        </>
      )}
    </div>
  );
}

/* ----------------------------------------------------------------------- linha do tempo */

type Recorte = "tudo" | "vacinas" | "atendimentos" | "creche";

/**
 * O recorte "Creche" e o unico que a API nao sustenta como o desenho pede.
 *
 * No desenho ele filtra por ORGANIZACAO, e o backend nao diz que tipo de organizacao e
 * cada uma — clinica, creche e abrigo chegam todas como `organizationName`. O mais proximo
 * honesto e "observacao vinda de uma organizacao", que e o que a creche produz.
 */
function passaNoRecorte(entrada: EntradaDaLinha, recorte: Recorte): boolean {
  if (recorte === "tudo") return true;
  if (recorte === "vacinas") return entrada.eventType === "VACINA" || entrada.eventType === "ANTIPARASITARIO";
  if (recorte === "atendimentos") return entrada.eventType === "ATENDIMENTO";
  return entrada.eventType === "OBSERVACAO" && entrada.organizationName !== undefined;
}

function faixaDeAnos(entradas: EntradaDaLinha[]): { de: string; ate: string } | undefined {
  const anos = entradas
    .map((entrada) => entrada.occurredAt?.slice(0, 4))
    .filter((ano): ano is string => ano !== undefined)
    .sort();

  return anos.length === 0 ? undefined : { de: anos[0]!, ate: anos[anos.length - 1]! };
}

/**
 * O titulo do evento, que quase sempre e o proprio `summary` da view.
 *
 * <b>A pesagem e a excecao, e ela chegava ilegivel.</b> A view monta o `summary` da
 * pesagem com o numero cru — "8.4" —, entao a linha do tempo exibia um titulo que nao e
 * frase nenhuma. Na Tela 02 a pesagem aparece dobrada dentro da consulta ("Consulta de
 * rotina · pesagem 8,4 kg"), o que e trabalho de view e nao de tela; enquanto isso nao
 * existe, o minimo honesto e dizer que aquele numero e um peso.
 */
function tituloDe(entrada: EntradaDaLinha, intl: ReturnType<typeof useIntl>): string {
  const resumo = entrada.summary ?? "";

  if (entrada.eventType !== "PESAGEM") {
    return resumo;
  }

  const peso = Number(resumo.replace(",", "."));

  return Number.isFinite(peso)
    ? intl.formatMessage(
        { id: "animal.linha.pesagem" },
        { peso: intl.formatNumber(peso, { maximumFractionDigits: 1 }) },
      )
    : resumo;
}

/**
 * O microchip agrupado, como na Tela 02: `981020 0034 5127`.
 *
 * Quinze digitos seguidos ninguem confere contra a carteirinha. O agrupamento e 6 + 4 + 4
 * porque e o do desenho, e o que sobrar fecha no ultimo grupo em vez de virar um grupo
 * solto de um digito.
 */
function microchipAgrupado(numero: string): string {
  const digitos = numero.replace(/\D/g, "");

  if (digitos.length <= 6) {
    return digitos;
  }

  const grupos = [digitos.slice(0, 6)];

  for (let i = 6; i < digitos.length; i += 4) {
    grupos.push(digitos.slice(i, i + 4));
  }

  /*
   * O resto menor que um grupo entra no anterior, em vez de virar digito solto. Com os 15
   * digitos do padrao ISO o corte 6+4+4 deixa um sobrando, e "981020 0034 5127 0" nao e
   * agrupamento — e um numero com um erro de digitacao aparente.
   */
  if (grupos.length > 2 && grupos[grupos.length - 1]!.length < 4) {
    const sobra = grupos.pop()!;
    grupos[grupos.length - 1] += sobra;
  }

  return grupos.join(" ");
}

/** Ato clinico e musgo; observacao e ocre; o resto e neutro. Nenhum evento e telha —
 *  telha e vencido, e um evento REGISTRADO nunca esta vencido: ele aconteceu. */
function corDoPonto(tipo: string | undefined): string {
  if (tipo === "VACINA" || tipo === "ATENDIMENTO" || tipo === "ANTIPARASITARIO") return "oklch(0.46 0.085 150)";
  if (tipo === "OBSERVACAO") return "oklch(0.62 0.11 70)";
  return "oklch(0.72 0.012 150)";
}

/**
 * <b>A data grande e QUANDO ACONTECEU; a hora miuda e QUANDO FOI LANCADO</b> (secao 06).
 * Um evento com dois anos entre as duas e normal — a carteirinha de papel de 2019 lancada
 * em 2024 —, e o desenho deixa isso legivel em vez de esconder.
 */
function Evento({ entrada, ultimo, animalId }: { entrada: EntradaDaLinha; ultimo: boolean; animalId: string }) {
  const intl = useIntl();
  const quando = entrada.occurredAt === undefined ? undefined : new Date(entrada.occurredAt);
  const fundo = ultimo ? "0" : "26px";

  return (
    <>
      <div style={{ padding: `2px 20px ${fundo} 0`, textAlign: "right", borderRight: "1px solid oklch(0.90 0.008 150)" }}>
        {quando !== undefined && (
          <>
            <div style={{ fontSize: "15px", fontWeight: 500 }}>
              {intl.formatDate(quando, { day: "2-digit", month: "short", year: "numeric" })}
            </div>
            <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "12px", color: "oklch(0.55 0.015 150)", marginTop: "3px" }}>
              {intl.formatDate(quando, { weekday: "long" })}
            </div>
          </>
        )}
      </div>
      <div style={{ padding: `0 0 ${fundo} 24px`, position: "relative" }}>
        <div style={{ position: "absolute", left: "-7px", top: "5px", width: "13px", height: "13px", borderRadius: "999px", background: corDoPonto(entrada.eventType), border: "3px solid oklch(0.985 0.004 120)" }}></div>
        <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "19px", fontWeight: 500, marginBottom: "6px" }}>
          {tituloDe(entrada, intl)}
        </div>
        {entrada.previousWeight !== undefined && (
          <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "10px" }}>
            {intl.formatMessage(
              { id: "animal.linha.pesoAnterior" },
              { peso: intl.formatNumber(entrada.previousWeight, { maximumFractionDigits: 1 }) },
            )}
          </div>
        )}
        <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
          {autoriaDe(entrada, intl)}
          {entrada.correctionCount !== undefined && entrada.correctionCount > 0 && (
            <>
              {" · "}
              {intl.formatMessage({ id: "animal.linha.corrigido" }, { vezes: entrada.correctionCount })}
            </>
          )}
          {/*
           * A entrada para a Tela 23, e so no atendimento: e o registro de outra pessoa sobre
           * a saude do animal, o unico que o tutor nao pode editar e sobre o qual ele pode
           * discordar. Observacao e pesagem sao dele — nao ha o que contestar no proprio texto.
           */}
          {entrada.eventType === "ATENDIMENTO" && entrada.eventId !== undefined && (
            <>
              {" · "}
              <Link
                to="/animais/$animalId/discordar/$registroId"
                params={{ animalId, registroId: entrada.eventId }}
                style={{ fontSize: "13px", color: "oklch(0.46 0.085 150)" }}
              >
                {intl.formatMessage({ id: "animal.linha.discordar" })}
              </Link>
            </>
          )}
        </div>
      </div>
    </>
  );
}

function autoriaDe(entrada: EntradaDaLinha, intl: ReturnType<typeof useIntl>): string {
  const quemRegistrou = entrada.recordedByName;
  const organizacao = entrada.organizationName;
  const lancadoEm =
    entrada.recordedAt === undefined
      ? undefined
      : intl.formatDate(new Date(entrada.recordedAt), { dateStyle: "short", timeStyle: "short" });

  if (quemRegistrou === undefined) {
    return lancadoEm === undefined ? "" : intl.formatMessage({ id: "animal.linha.lancado" }, { quando: lancadoEm });
  }

  /*
   * "Responsabilidade tem nome" (secao 10): a autoria e fixa no rodape de TODO evento, e a
   * credencial aparece com o estado dela. `INFORMADO` nao e `VERIFICADO`, e o produto nao
   * finge garantia que nao tem.
   */
  const credencial =
    entrada.credentialLabel === undefined
      ? ""
      : ` (${entrada.credentialLabel}${
          entrada.credentialStatus === undefined
            ? ""
            : ` · ${intl.formatMessage({ id: `animal.credencial.${entrada.credentialStatus}` })}`
        })`;

  return intl.formatMessage(
    { id: organizacao === undefined ? "animal.linha.autoria" : "animal.linha.autoriaComOrg" },
    { quem: quemRegistrou + credencial, organizacao: organizacao ?? "", quando: lancadoEm ?? "" },
  );
}

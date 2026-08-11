import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { Marcador, legendaDaDose } from "../componentes/Vacinacao.tsx";
import { useHistoricoClinico, useOrientacoes, useTutores, type RecorteDoHistorico } from "../dados/animal.ts";
import { useDuplicatas, usePedidosDeUniao } from "../dados/uniao.ts";
import { useAnimal, useCarteira, useCondicoes, usePesagens } from "../dados/carteira.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 30 · area de organizacao — O paciente, pelos olhos da clinica", de
 * `design/IdentidadeVisual/Telas Petfy - Nucleo clinico.dc.html`.
 *
 * <b>E o mesmo animal da Tela 02, e nao e a mesma tela.</b> A Tela 02 e a vida do animal como o
 * tutor a le; esta e o que a clinica precisa ter na frente ANTES de prescrever. A ordem das
 * caixas nao e estetica: alergia e o que esta em uso vem antes de tudo, porque prescrever sem
 * ver isso e o erro que este produto existe para tornar dificil.
 *
 * <b>AS TRES ORIGENS NO MESMO FIO, e cada uma se identifica.</b> O desenho fecha com a regra:
 * "o que a clinica registrou, o que veio de outro veterinario e o que e observacao de quem nao
 * tem registro profissional. Um relato de creche nunca se parece com um diagnostico". Isso ja
 * vinha inteiro do `TimelineEntryResponseDTO` — `organizationName`, `credentialLabel` com o
 * `credentialStatus`, e o `eventType` — e o que faltava era a tela DISTINGUIR, em vez de
 * desenhar tres coisas iguais.
 *
 * <b>"Fora do seu alcance" nao e erro nem lista vazia.</b> A linha do tempo devolve o evento
 * fora de escopo COM a data e SEM o conteudo, e o desenho pede exatamente isso: "existe e nao
 * foi compartilhado com a clinica. Se for necessario para o caso, peca ao Marcelo". Some seria
 * pior — a clinica concluiria que o animal nunca foi a creche.
 *
 * <b>"IMPRIMIR CARTEIRINHA" NAO TEM ROTA</b>, e fica desabilitado com o motivo, como a busca e o
 * marcador de avisos da moldura. A secao 06 da identidade manda: "o desabilitado nunca aparece
 * mudo — ao lado dele, sempre a frase que diz por que".
 *
 * <b>A TELA NAO RECALCULA SAUDE.</b> O estado de cada vacina vem do servidor pelo mesmo caminho
 * da Tela 02: recalcular aqui criaria uma segunda verdade sobre o animal, e o dia em que as duas
 * divergissem seria o dia em que uma antirrabica vencida apareceria em dia para quem prescreve.
 */

export const Route = createFileRoute("/pacientes/$animalId")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Paciente,
});

/** Os tres do desenho, na ordem dele. "Tudo" primeiro: o recorte e escolha, e nao o default. */
const RECORTES: RecorteDoHistorico[] = ["tudo", "desta-clinica", "meus"];

function Paciente() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const [recorte, setRecorte] = useState<RecorteDoHistorico>("tudo");

  const animal = useAnimal(animalId);
  const tutores = useTutores(animalId);
  /* A duplicata e um caso raro e caro de descobrir tarde: quem prescreve sem saber que existe
     outro cadastro prescreve contra metade do historico. A leitura e barata e o aviso e mudo
     quando nao ha nada. */
  const duplicatas = useDuplicatas(animalId);
  const pedidosDeUniao = usePedidosDeUniao(animalId);
  const condicoes = useCondicoes(animalId);
  const orientacoes = useOrientacoes(animalId);
  const pesagens = usePesagens(animalId);
  const carteira = useCarteira(animalId);
  const historico = useHistoricoClinico(animalId, recorte);

  const bicho = animal.data;
  /* Quem RESPONDE pelo animal, e nao o primeiro da lista: o desenho manda pedir o que falta a
     ele, e pedir a co-tutora o que so o titular libera nao destrava nada. */
  const titular = (tutores.data ?? []).find((tutor) => tutor.holder === true);

  /* Ativa e vigente, e nao tudo: a caixa "antes de prescrever" existe para o que vale AGORA.
     Condicao resolvida e orientacao encerrada sao historico, e historico tem o proprio fio. */
  const condicoesAtivas = (condicoes.data ?? []).filter((condicao) => condicao.ativa !== false);
  const emUso = (orientacoes.data ?? []).filter((orientacao) => orientacao.vigente !== false);

  const serie = pesagens.data ?? [];
  const entradas = historico.data ?? [];

  /* O que existe e a clinica nao alcanca. Vem da MESMA leitura, e nao de uma segunda consulta:
     a linha do tempo ja devolve a entrada opaca, com data e sem conteudo. */
  const foraDoAlcance = entradas.filter((entrada) => entrada.visivel === false);
  const alcancadas = entradas.filter((entrada) => entrada.visivel !== false);

  return (
    <div style={{ padding: "40px 24px" }}>
      {/* ---------------------------------------------------- o mesmo animal, duas vezes
       *
       * <b>Fica no topo, acima de tudo, e nao numa caixa lateral.</b> Quem prescreve sem saber
       * que existe outro cadastro prescreve contra metade do historico — e a Tela 30 inteira e
       * sobre ter a informacao certa ANTES de prescrever. Um aviso que exige rolar seria um aviso
       * que chega depois da decisao.
       */}
      {((duplicatas.data ?? []).length > 0 || (pedidosDeUniao.data ?? []).length > 0) && (
        <div style={{ maxWidth: "1360px", margin: "0 auto 20px" }}>
          <Link
            to="/pacientes/$animalId/duplicado"
            params={{ animalId }}
            style={{ display: "block", textDecoration: "none", border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "20px 24px" }}
          >
            <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, color: "oklch(0.25 0.02 150)", marginBottom: "6px" }}>
              {intl.formatMessage({
                id: (pedidosDeUniao.data ?? []).length > 0
                  ? "paciente.duplicado.pedido"
                  : "paciente.duplicado.titulo",
              })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "paciente.duplicado.texto" })}
            </div>
          </Link>
        </div>
      )}

      <div style={{ maxWidth: "1360px", margin: "0 auto", display: "grid", gridTemplateColumns: "380px 1fr", gap: "24px", alignItems: "start" }}>

        {/* ============================================================ a coluna do animal */}
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>

          <div style={{ border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "26px 28px" }}>
            {animal.isPending && <Carregando oQue={intl.formatMessage({ id: "paciente.oQue" })} />}

            {animal.isError && (
              <ErroDeCarga
                oQue={intl.formatMessage({ id: "paciente.oQue" })}
                erro={animal.error}
                aoTentarDeNovo={() => void animal.refetch()}
                carregando={animal.isFetching}
              />
            )}

            {bicho !== undefined && (
              <>
                <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 6px", letterSpacing: "-0.02em" }}>
                  {bicho.name}
                </h1>

                <div style={{ fontSize: "15px", color: "oklch(0.45 0.015 150)", lineHeight: 1.6 }}>
                  {[
                    bicho.species === undefined
                      ? undefined
                      : intl.formatMessage({ id: `animal.especie.${bicho.species}` }),
                    bicho.breed,
                    serie[0]?.weight === undefined
                      ? undefined
                      : intl.formatMessage({ id: "paciente.peso" }, { peso: serie[0].weight }),
                  ]
                    .filter((parte) => parte !== undefined && parte !== "")
                    .join(" · ")}
                </div>

                {/* O microchip e identidade, e por isso monoespacado: numero que se confere
                    digito a digito nao pode ter largura variavel. */}
                {bicho.microchipNumber !== undefined && bicho.microchipNumber !== "" && (
                  <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.42 0.015 150)", marginTop: "12px" }}>
                    {intl.formatMessage({ id: "paciente.microchip" }, { numero: bicho.microchipNumber })}
                  </div>
                )}

                {titular !== undefined && (
                  <div style={{ fontSize: "15px", color: "oklch(0.42 0.015 150)", marginTop: "10px" }}>
                    {intl.formatMessage({ id: "paciente.tutor" }, { nome: titular.personName ?? "" })}
                  </div>
                )}

                <div style={{ display: "flex", gap: "10px", marginTop: "22px", flexWrap: "wrap" }}>
                  <Link
                    to="/pacientes/$animalId/atendimento"
                    params={{ animalId }}
                    style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", display: "flex", alignItems: "center", textDecoration: "none" }}
                  >
                    {intl.formatMessage({ id: "paciente.registrar" })}
                  </Link>

                  {/* Sem rota nenhuma para gerar carteirinha. Desabilitado com o motivo. */}
                  <button
                    type="button"
                    disabled
                    title={intl.formatMessage({ id: "paciente.imprimir.indisponivel" })}
                    style={{ fontFamily: "inherit", fontSize: "15px", color: "oklch(0.62 0.012 150)", background: "oklch(0.98 0.004 150)", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", cursor: "not-allowed" }}
                  >
                    {intl.formatMessage({ id: "paciente.imprimir" })}
                  </button>
                </div>
              </>
            )}
          </div>

          {/* --------------------------------------------------------- antes de prescrever
           *
           * <b>A caixa mais importante da tela, e por isso ela e a primeira.</b> Prescrever sem
           * ver alergia e o que ja esta em uso e o erro que este produto existe para tornar
           * dificil — e a Tela 31 repete o aviso de sobreposicao no proprio formulario, porque
           * quem esta digitando pode nao ter olhado para ca.
           */}
          <Caixa titulo={intl.formatMessage({ id: "paciente.antesDePrescrever" })} destaque>
            {condicoesAtivas.length === 0 && emUso.length === 0 && (
              <Vazio>{intl.formatMessage({ id: "paciente.antesDePrescrever.nada" })}</Vazio>
            )}

            {condicoesAtivas.map((condicao) => (
              <div key={condicao.animalHealthConditionId} style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.3 0.02 150)", marginTop: "6px" }}>
                <strong style={{ fontWeight: 500 }}>{condicao.description}</strong>
                {condicao.since !== undefined && (
                  <span style={{ color: "oklch(0.5 0.015 150)", fontSize: "14px" }}>
                    {" "}
                    {intl.formatMessage({ id: "animal.carteira.desde" }, { ano: condicao.since.slice(0, 4) })}
                  </span>
                )}
              </div>
            ))}

            {emUso.map((orientacao) => (
              <div key={orientacao.careInstructionId} style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.3 0.02 150)", marginTop: "6px" }}>
                {intl.formatMessage(
                  { id: "paciente.emUso" },
                  {
                    o_que: orientacao.description ?? "",
                    ate:
                      orientacao.endsOn === undefined
                        ? ""
                        : intl.formatDate(orientacao.endsOn, { day: "2-digit", month: "2-digit" }),
                  },
                )}
              </div>
            ))}
          </Caixa>

          {/* ------------------------------------------------------------------- vacinacao */}
          <Caixa titulo={intl.formatMessage({ id: "paciente.vacinacao" })}>
            {carteira.carregando && <Carregando oQue={intl.formatMessage({ id: "paciente.oQue.vacinas" })} />}

            {!carteira.carregando &&
              carteira.linhas.map((linha) => (
                <div key={linha.nome} style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "15px", marginTop: "8px" }}>
                  <Marcador estado={linha.estado} />
                  <span style={{ flex: 1 }}>{linha.nome}</span>
                  <span style={{ fontSize: "14px", color: linha.estado === "vencida" ? "oklch(0.45 0.13 30)" : "oklch(0.5 0.015 150)", textAlign: "right" }}>
                    {legendaDaDose(linha, intl)}
                  </span>
                </div>
              ))}
          </Caixa>

          {/* ------------------------------------------------------------------------ peso */}
          <Caixa titulo={intl.formatMessage({ id: "paciente.peso.titulo" }, { quantos: serie.length })}>
            {serie.length === 0 ? (
              <Vazio>{intl.formatMessage({ id: "paciente.peso.vazio" })}</Vazio>
            ) : (
              <div style={{ fontSize: "15px", color: "oklch(0.35 0.018 150)" }}>
                {intl.formatMessage(
                  { id: "paciente.peso.variacao" },
                  { de: serie[serie.length - 1]?.weight ?? 0, para: serie[0]?.weight ?? 0 },
                )}
              </div>
            )}
          </Caixa>

          {/* ------------------------------------------------------------ fora do alcance
           *
           * O desenho pede isto com estas palavras: "existe e nao foi compartilhado com a
           * clinica. Se for necessario para o caso, peca ao Marcelo". A saida e humana e nao
           * tecnica de proposito — quem pode liberar e o tutor, e nao um botao daqui.
           */}
          {foraDoAlcance.length > 0 && (
            <Caixa titulo={intl.formatMessage({ id: "paciente.foraDoAlcance" })}>
              <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage(
                  { id: "paciente.foraDoAlcance.texto" },
                  { quantos: foraDoAlcance.length, quem: titular?.personName ?? "" },
                )}
              </div>
            </Caixa>
          )}
        </div>

        {/* ====================================================== o historico, a coluna larga */}
        <div style={{ border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "26px 28px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "16px", flexWrap: "wrap", marginBottom: "20px" }}>
            <h2 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, margin: 0 }}>
              {intl.formatMessage({ id: "paciente.historico" })}
            </h2>

            <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
              {RECORTES.map((qual) => (
                <button
                  key={qual}
                  type="button"
                  onClick={() => setRecorte(qual)}
                  style={{ fontFamily: "inherit", fontSize: "14px", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "8px", padding: "9px 14px", minHeight: "40px", cursor: "pointer", background: recorte === qual ? "oklch(0.94 0.02 150)" : "oklch(1 0 0)", color: recorte === qual ? "oklch(0.38 0.07 150)" : "oklch(0.42 0.015 150)", fontWeight: recorte === qual ? 500 : 400 }}
                >
                  {intl.formatMessage({ id: `paciente.recorte.${qual}` })}
                </button>
              ))}
            </div>
          </div>

          {historico.isPending && <Carregando oQue={intl.formatMessage({ id: "paciente.oQue.historico" })} />}

          {historico.isError && (
            <ErroDeCarga
              oQue={intl.formatMessage({ id: "paciente.oQue.historico" })}
              erro={historico.error}
              aoTentarDeNovo={() => void historico.refetch()}
              carregando={historico.isFetching}
            />
          )}

          {/* Vazio com recorte marcado nao e o mesmo que vazio: "esta clinica nunca registrou
              nada deste animal" e um fato, e nao a ausencia de historico. */}
          {!historico.isPending && alcancadas.length === 0 && (
            <Vazio>
              {intl.formatMessage({ id: `paciente.historico.vazio.${recorte}` })}
            </Vazio>
          )}

          {alcancadas.map((entrada) => (
            <Entrada key={`${entrada.eventType}-${entrada.eventId ?? entrada.occurredAt}`} entrada={entrada} />
          ))}
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

/**
 * Uma entrada do fio, com a assinatura que a distingue das outras duas origens.
 *
 * <b>A linha de autoria e o que impede um relato de creche de parecer um diagnostico.</b> Ela
 * junta tres coisas que o servidor manda separadas — quem, por qual organizacao, e com qual
 * credencial em qual estado — e a credencial apenas INFORMADA aparece como informada, em tinta
 * secundaria: o produto nao da selo de verificado que nao conferiu (identidade, 5.10).
 */
function Entrada({ entrada }: { entrada: ReturnType<typeof useHistoricoClinico>["data"] extends (infer T)[] | undefined ? T : never }) {
  const intl = useIntl();

  const clinico = entrada.eventType === "ATENDIMENTO";

  return (
    <div style={{ borderTop: "1px solid oklch(0.94 0.006 150)", padding: "18px 0" }}>
      <div style={{ display: "flex", alignItems: "baseline", gap: "14px", flexWrap: "wrap" }}>
        <span style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px", color: "oklch(0.5 0.015 150)", minWidth: "96px" }}>
          {entrada.occurredAt === undefined
            ? ""
            : intl.formatDate(entrada.occurredAt, { day: "2-digit", month: "short", year: "numeric" })}
        </span>

        <span style={{ fontSize: "16px", fontWeight: clinico ? 500 : 400 }}>{entrada.summary}</span>

        {/* O rotulo que o desenho exige: "observacao, nao clinico". Um relato de creche nunca
            se parece com um diagnostico, e o rotulo e o que garante isso quando o texto e
            longo e o olho corre. */}
        {entrada.eventType === "OBSERVACAO" && (
          <span style={{ fontSize: "12px", letterSpacing: "0.04em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", border: "1px solid oklch(0.90 0.008 150)", borderRadius: "999px", padding: "3px 10px" }}>
            {intl.formatMessage({ id: "paciente.naoClinico" })}
          </span>
        )}

        {(entrada.correctionCount ?? 0) > 0 && (
          <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
            {intl.formatMessage({ id: "paciente.corrigido" }, { quantas: entrada.correctionCount })}
          </span>
        )}
      </div>

      <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "8px", paddingLeft: "110px", lineHeight: 1.6 }}>
        {[
          entrada.recordedByName,
          entrada.organizationName === undefined
            ? undefined
            : intl.formatMessage({ id: "paciente.pela" }, { organizacao: entrada.organizationName }),
          entrada.credentialLabel === undefined
            ? intl.formatMessage({ id: "paciente.semCredencial" })
            : intl.formatMessage(
                { id: "paciente.credencial" },
                {
                  registro: entrada.credentialLabel,
                  estado: intl.formatMessage({
                    id: `animal.credencial.${entrada.credentialStatus ?? "INFORMADO"}`,
                  }),
                },
              ),
        ]
          .filter((parte) => parte !== undefined && parte !== "")
          .join(" · ")}
      </div>
    </div>
  );
}

function Caixa({ titulo, children, destaque = false }: { titulo: string; children: ReactNode; destaque?: boolean }) {
  return (
    <div style={{ border: `1px solid ${destaque ? "oklch(0.86 0.03 70)" : "oklch(0.90 0.008 150)"}`, background: destaque ? "oklch(0.985 0.012 70)" : "oklch(1 0 0)", borderRadius: "12px", padding: "22px 24px" }}>
      <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "12px" }}>
        {titulo}
      </div>
      {children}
    </div>
  );
}

function Vazio({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.5 0.015 150)" }}>{children}</div>
  );
}

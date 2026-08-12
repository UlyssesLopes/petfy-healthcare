import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useCandidatosAEncaminhamento,
  useEncaminhar,
  useOpcoesDeEncaminhamento,
  type Escopo,
} from "../dados/encaminhamento.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 45 · entre profissionais — Encaminhar para o especialista", de
 * `design/IdentidadeVisual/Telas Petfy - Animal comunitario e encaminhamento.dc.html`.
 *
 * <b>"Marcelo Dias precisa autorizar. Encaminhar e voce indicando o caminho; conceder acesso
 * continua sendo dele, como sempre foi."</b> E a regra que torna esta tela possivel, e o que ela
 * significa aqui e que <b>o botao no fim nao concede nada</b>: ele cria um pedido, e quem responde
 * pelo animal decide.
 *
 * ------------------------------------------------------- o que vai junto e ESCOPO, nao evento
 *
 * O desenho mostra quatro caixas que selecionam EVENTOS: "o raio-X de 2023", "4 observacoes da
 * Creche Quintal entre 02/06 e 05/08". A concessao deste produto so sabe conceder por TIPO, e essa e
 * a unidade que a guarda aplica em toda leitura.
 *
 * Entao as caixas SAO os escopos, com o subtitulo contado do animal de verdade, e o que motivou o
 * encaminhamento vai no campo do motivo. <b>A tela nao promete na caixa um recorte que a concessao
 * nao sabe fazer</b> — e o recorte por evento tambem mentiria com facilidade: as quatro observacoes
 * escolhidas hoje nao dizem nada sobre a quinta, escrita amanha pela mesma creche sobre o mesmo
 * problema, e o especialista tratando o caso nao a veria.
 *
 * ------------------------------------------------------- o nome que esta tela NAO diz
 *
 * O desenho escreve "Marcelo Dias precisa autorizar", com o nome. <b>Aqui a frase e sem nome</b>, e
 * a razao e de acesso: o nome de quem responde pelo animal vive no escopo `CONTATO`, e quem
 * encaminha em geral tem so o clinico. Escrever o nome exigiria que a tela pedisse ao servidor um
 * dado que aquela concessao nao alcanca — e o resultado seria um campo vazio no lugar do nome, ou
 * uma tela que entrega contato a quem o tutor nao o concedeu.
 */

export const Route = createFileRoute("/animais/$animalId_/encaminhar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Encaminhar,
});

function Encaminhar() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const opcoes = useOpcoesDeEncaminhamento(animalId);
  const encaminhar = useEncaminhar(animalId);

  const [busca, setBusca] = useState("");
  const [paraQuem, setParaQuem] = useState<{ id: string; nome: string } | null>(null);
  const [motivo, setMotivo] = useState("");
  const [escolhidos, setEscolhidos] = useState<Set<Escopo> | null>(null);
  const [enviado, setEnviado] = useState<"PENDENTE" | "AUTORIZADO" | null>(null);

  const candidatos = useCandidatosAEncaminhamento(animalId, busca);

  if (opcoes.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "encaminhar.oQueE" })} />;
  }

  if (opcoes.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "encaminhar.oQueE" })}
        erro={opcoes.error}
        aoTentarDeNovo={() => void opcoes.refetch()}
        carregando={opcoes.isFetching}
      />
    );
  }

  const caixas = opcoes.data?.scopes ?? [];
  const nome = opcoes.data?.animalName ?? "";
  const dias = opcoes.data?.defaultAccessDays ?? 90;

  /* O servidor SUGERE, e a tela nao decide sozinha: as marcadas sao as que o desenho mostra
     marcadas, e recalcular isso aqui criaria uma segunda verdade sobre o que e o recorte clinico
     de um encaminhamento. Nulo significa "ainda nao mexi", e ai vale a sugestao. */
  const marcados =
    escolhidos ??
    new Set<Escopo>(
      caixas.filter((caixa) => caixa.suggested === true).map((caixa) => caixa.scope as Escopo),
    );

  const alternar = (escopo: Escopo) => {
    const proximo = new Set(marcados);

    if (proximo.has(escopo)) {
      proximo.delete(escopo);
    } else {
      proximo.add(escopo);
    }

    setEscolhidos(proximo);
  };

  const podeEnviar =
    paraQuem !== null && motivo.trim() !== "" && marcados.size > 0 && !encaminhar.isPending;

  const enviar = () => {
    if (paraQuem === null) {
      return;
    }

    encaminhar.mutate(
      { paraQuemId: paraQuem.id, motivo: motivo.trim(), escopos: [...marcados] },
      {
        onSuccess: (resposta) =>
          setEnviado(resposta.status === "AUTORIZADO" ? "AUTORIZADO" : "PENDENTE"),
      },
    );
  };

  if (enviado !== null) {
    return (
      <div style={{ padding: "40px 24px" }}>
        <div style={{ ...cartao, maxWidth: "700px" }}>
          <h1 style={titulo}>{intl.formatMessage({ id: "encaminhar.enviado.titulo" })}</h1>
          <p style={{ fontSize: "15px", lineHeight: 1.65, margin: "0 0 18px" }}>
            {intl.formatMessage(
              {
                id:
                  enviado === "AUTORIZADO"
                    ? "encaminhar.enviado.jaAutorizado"
                    : "encaminhar.enviado.esperandoDecisao",
              },
              { quem: paraQuem?.nome ?? "", dias },
            )}
          </p>
          <Link to="/animais/$animalId" params={{ animalId }} style={{ color: VERDE }}>
            {intl.formatMessage({ id: "encaminhar.voltarAoAnimal" }, { nome })}
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ ...cartao, maxWidth: "760px" }}>
        <h1 style={titulo}>{intl.formatMessage({ id: "encaminhar.titulo" }, { nome })}</h1>

        <p style={{ fontSize: "15px", lineHeight: 1.6, color: CINZA, margin: "0 0 26px" }}>
          {intl.formatMessage({ id: "encaminhar.oCasoVaiInteiro" })}
        </p>

        <div style={{ display: "flex", flexDirection: "column", gap: "18px" }}>
          {/* -------------------------------------------------------------------- para quem */}
          <div>
            <label htmlFor="busca" style={rotulo}>
              {intl.formatMessage({ id: "encaminhar.paraQuem" })}
            </label>

            {paraQuem === null ? (
              <>
                <input
                  id="busca"
                  type="search"
                  value={busca}
                  placeholder={intl.formatMessage({ id: "encaminhar.busca.dica" })}
                  onChange={(evento) => setBusca(evento.target.value)}
                  style={campo}
                />

                {busca.trim().length > 0 && busca.trim().length < 3 && (
                  <div style={nota}>{intl.formatMessage({ id: "encaminhar.busca.curta" })}</div>
                )}

                {candidatos.data?.length === 0 && (
                  <div style={nota}>{intl.formatMessage({ id: "encaminhar.busca.vazia" })}</div>
                )}

                <div style={{ display: "flex", flexDirection: "column", gap: "8px", marginTop: "10px" }}>
                  {(candidatos.data ?? []).map((pessoa) => (
                    <button
                      key={pessoa.personId}
                      type="button"
                      onClick={() =>
                        setParaQuem({ id: pessoa.personId ?? "", nome: pessoa.name ?? "" })
                      }
                      style={{
                        textAlign: "left",
                        fontFamily: "inherit",
                        fontSize: "15px",
                        background: "oklch(1 0 0)",
                        border: "1px solid oklch(0.86 0.008 150)",
                        borderRadius: "8px",
                        padding: "12px 14px",
                        minHeight: "48px",
                        cursor: "pointer",
                      }}
                    >
                      <div style={{ fontWeight: 500 }}>
                        {[pessoa.name, pessoa.specialty, ...(pessoa.organizations ?? [])]
                          .filter((parte) => parte !== null && parte !== undefined && parte !== "")
                          .join(" · ")}
                      </div>
                      <div style={{ fontSize: "13px", color: CINZA, marginTop: "3px" }}>
                        {/*
                         * "Ele ja registrou o raio-X do Code em 2023." E a diferenca entre
                         * encaminhar para um nome numa lista e encaminhar para quem conhece o caso.
                         */}
                        {pessoa.lastContributionAt === null ||
                        pessoa.lastContributionAt === undefined
                          ? intl.formatMessage({ id: "encaminhar.candidato.novo" }, { nome })
                          : intl.formatMessage(
                              { id: "encaminhar.candidato.jaRegistrou" },
                              {
                                nome,
                                ano: new Date(pessoa.lastContributionAt).getFullYear(),
                              },
                            )}
                      </div>
                      <div style={{ fontSize: "13px", color: CINZA }}>{pessoa.credential}</div>
                    </button>
                  ))}
                </div>
              </>
            ) : (
              <div
                style={{
                  border: `1px solid ${VERDE}`,
                  borderRadius: "4px",
                  padding: "13px 14px",
                  fontSize: "16px",
                  minHeight: "48px",
                  background: "oklch(1 0 0)",
                  display: "flex",
                  justifyContent: "space-between",
                  gap: "12px",
                  alignItems: "center",
                }}
              >
                <span>{paraQuem.nome}</span>
                <button
                  type="button"
                  onClick={() => setParaQuem(null)}
                  style={{
                    fontFamily: "inherit",
                    fontSize: "14px",
                    color: VERDE,
                    background: "transparent",
                    border: "none",
                    textDecoration: "underline",
                    cursor: "pointer",
                    minHeight: "44px",
                  }}
                >
                  {intl.formatMessage({ id: "encaminhar.buscarOutro" })}
                </button>
              </div>
            )}
          </div>

          {/* ---------------------------------------------------------------------- o motivo */}
          <div>
            <label htmlFor="motivo" style={rotulo}>
              {intl.formatMessage({ id: "encaminhar.motivo" })}
            </label>
            <textarea
              id="motivo"
              rows={4}
              value={motivo}
              onChange={(evento) => setMotivo(evento.target.value)}
              style={{ ...campo, minHeight: "96px", lineHeight: 1.6, resize: "vertical" }}
            />
            <div style={nota}>{intl.formatMessage({ id: "encaminhar.motivo.nota" })}</div>
          </div>

          {/* ------------------------------------------------------------- o que vai junto */}
          <fieldset
            style={{
              border: "1px solid oklch(0.90 0.008 150)",
              borderRadius: "12px",
              background: "oklch(1 0 0)",
              padding: "20px 22px",
              margin: 0,
            }}
          >
            <legend
              style={{
                fontSize: "12px",
                letterSpacing: "0.05em",
                textTransform: "uppercase",
                color: CINZA,
                padding: "0 6px",
              }}
            >
              {intl.formatMessage({ id: "encaminhar.oQueVaiJunto" })}
            </legend>

            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {caixas.map((caixa) => {
                const escopo = caixa.scope as Escopo;

                return (
                  <label
                    key={escopo}
                    style={{ display: "flex", alignItems: "flex-start", gap: "12px", cursor: "pointer" }}
                  >
                    <input
                      type="checkbox"
                      checked={marcados.has(escopo)}
                      onChange={() => alternar(escopo)}
                      style={{ width: "20px", height: "20px", marginTop: "2px", flex: "none" }}
                    />
                    <span>
                      <span style={{ fontSize: "15px" }}>
                        {intl.formatMessage({ id: `encaminhar.escopo.${escopo}` })}
                      </span>
                      <span style={{ display: "block", fontSize: "13px", color: CINZA, marginTop: "2px" }}>
                        {intl.formatMessage(
                          { id: "encaminhar.caixa.eventos" },
                          { quantos: caixa.events ?? 0 },
                        )}
                        {caixa.since !== null && caixa.since !== undefined
                          ? intl.formatMessage(
                              { id: "encaminhar.caixa.desde" },
                              { ano: new Date(caixa.since).getFullYear() },
                            )
                          : ""}
                        {/*
                         * CONTATO nao e um pedaco do prontuario: e o telefone de quem responde pelo
                         * animal. Marca-lo junto com os outros, sem que a diferenca apareca,
                         * entregaria dado pessoal escondido dentro de um recorte clinico.
                         */}
                        {caixa.personalData === true
                          ? ` ${intl.formatMessage({ id: "encaminhar.caixa.dadoPessoal" })}`
                          : ""}
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>

            <div style={{ ...nota, marginTop: "14px" }}>
              {intl.formatMessage(
                { id: "encaminhar.oQueVaiJunto.porEscopo" },
                { total: opcoes.data?.totalEvents ?? 0 },
              )}
            </div>
          </fieldset>

          {/*
           * O aviso de que falta o tutor vem ANTES do botao, e nao depois do gesto: o que a frase
           * evita e a surpresa de o especialista nao alcancar o animal no instante do clique.
           */}
          <div
            style={{
              border: "1px solid oklch(0.86 0.03 70)",
              background: "oklch(0.985 0.012 70)",
              borderRadius: "12px",
              padding: "18px 20px",
              display: "flex",
              gap: "12px",
              alignItems: "flex-start",
            }}
          >
            <span
              aria-hidden
              style={{
                width: "12px",
                height: "12px",
                borderRadius: "999px",
                border: "3px solid oklch(0.62 0.11 70)",
                flex: "none",
                marginTop: "4px",
              }}
            ></span>
            <span style={{ fontSize: "15px", lineHeight: 1.6 }}>
              {intl.formatMessage({ id: "encaminhar.precisaAutorizar" }, { dias })}
            </span>
          </div>

          {encaminhar.error !== null && encaminhar.error !== undefined && (
            <ErroAoGravar erro={encaminhar.error} oQue={nome} />
          )}

          <div style={{ display: "flex", alignItems: "center", gap: "14px", flexWrap: "wrap" }}>
            <button
              type="button"
              disabled={!podeEnviar}
              onClick={enviar}
              style={{
                fontFamily: "inherit",
                fontSize: "16px",
                fontWeight: 500,
                color: "oklch(1 0 0)",
                background: podeEnviar ? VERDE : "oklch(0.62 0.05 150)",
                border: "none",
                borderRadius: "8px",
                padding: "15px 26px",
                minHeight: "52px",
                cursor: podeEnviar ? "pointer" : "not-allowed",
              }}
            >
              {intl.formatMessage({
                id: encaminhar.isPending ? "encaminhar.enviando" : "encaminhar.enviar",
              })}
            </button>
            <span style={{ fontSize: "15px", color: CINZA }}>
              {intl.formatMessage({ id: "encaminhar.tutorDecide" })}
            </span>
          </div>

          {/* O desabilitado nunca aparece mudo: a frase diz o que falta, e nao so que nao da. */}
          {!podeEnviar && !encaminhar.isPending && (
            <div style={nota}>{intl.formatMessage({ id: "encaminhar.faltaPreencher" })}</div>
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
  background: "oklch(0.985 0.004 120)",
  border: "1px solid oklch(0.86 0.008 150)",
  borderRadius: "12px",
  padding: "34px 38px 38px",
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

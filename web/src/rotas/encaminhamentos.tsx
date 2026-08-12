import { createFileRoute, redirect } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useAutorizarEncaminhamento,
  useEncaminhamentosRecebidos,
  usePendentesParaDecisao,
  type Encaminhamento,
} from "../dados/encaminhamento.ts";
import { useRecusarEncaminhamento } from "../dados/encaminhamento.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O outro lado da Tela 45: "O Marcelo recebe e decide."
 *
 * <b>O desenho nao desenha esta tela</b>, e sem ela a metade que ele desenha nao funciona — o
 * encaminhamento e o unico fluxo do produto em que a acao de uma pessoa fica parada esperando outra
 * que nao esta na tela. A clinica clica e sai do consultorio; o tutor recebe um e-mail e precisa de
 * um lugar para onde ir.
 *
 * <b>Duas listas na mesma pagina, e nao duas paginas.</b> Elas respondem perguntas diferentes — "o
 * que espera minha decisao" e "o que encaminharam para mim" —, e a mesma pessoa pode ter as duas: a
 * veterinaria que e tutora de um animal decide sobre ele e recebe casos de colegas. Duas rotas
 * fariam essa pessoa descobrir a segunda por acidente.
 *
 * <b>Nao ha rota por animal</b>, e e deliberado: o tutor chega pelo e-mail sem saber de que animal
 * se trata, e uma URL com o id do animal o obrigaria a descobri-lo antes de poder ler o pedido.
 */

export const Route = createFileRoute("/encaminhamentos")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Encaminhamentos,
});

function Encaminhamentos() {
  const intl = useIntl();

  const pendentes = usePendentesParaDecisao();
  const recebidos = useEncaminhamentosRecebidos();
  const autorizar = useAutorizarEncaminhamento();
  const recusar = useRecusarEncaminhamento();

  if (pendentes.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "encaminhamentos.oQueE" })} />;
  }

  if (pendentes.isError) {
    return (
      <ErroDeCarga
        oQue={intl.formatMessage({ id: "encaminhamentos.oQueE" })}
        erro={pendentes.error}
        aoTentarDeNovo={() => void pendentes.refetch()}
        carregando={pendentes.isFetching}
      />
    );
  }

  const paraDecidir = pendentes.data ?? [];
  const paraMim = recebidos.data ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "820px", margin: "0 auto" }}>
        <h1 style={titulo}>{intl.formatMessage({ id: "encaminhamentos.titulo" })}</h1>

        <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 28px" }}>
          {intl.formatMessage({ id: "encaminhamentos.oQueEstaTelaE" })}
        </p>

        {(autorizar.error !== null && autorizar.error !== undefined) && (
          <ErroAoGravar
            erro={autorizar.error}
            oQue={intl.formatMessage({ id: "encaminhamentos.oQueE" })}
          />
        )}

        {(recusar.error !== null && recusar.error !== undefined) && (
          <ErroAoGravar
            erro={recusar.error}
            oQue={intl.formatMessage({ id: "encaminhamentos.oQueE" })}
          />
        )}

        {/* ---------------------------------------------------------- o que espera decisao */}
        <h2 style={subtitulo}>{intl.formatMessage({ id: "encaminhamentos.esperandoVoce" })}</h2>

        {paraDecidir.length === 0 ? (
          <p style={vazio}>{intl.formatMessage({ id: "encaminhamentos.nadaEsperando" })}</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
            {paraDecidir.map((pedido) => (
              <article key={pedido.referralId} style={cartao}>
                <h3 style={{ ...subtitulo, fontSize: "19px", margin: "0 0 8px" }}>
                  {intl.formatMessage(
                    { id: "encaminhamentos.quemEncaminhou" },
                    {
                      quem: nomeDeQuemEncaminhou(pedido),
                      animal: pedido.animalName ?? "",
                      para: pedido.toPersonName ?? "",
                    },
                  )}
                </h3>

                {pedido.toPersonSpecialty !== null && pedido.toPersonSpecialty !== undefined && (
                  <div style={{ fontSize: "14px", color: CINZA, marginBottom: "10px" }}>
                    {pedido.toPersonSpecialty}
                  </div>
                )}

                {/* O motivo e o que o tutor le para decidir. Sem ele, autorizaria o desconhecido. */}
                <p style={{ fontSize: "15px", lineHeight: 1.65, margin: "0 0 14px" }}>
                  {pedido.reason}
                </p>

                <div
                  style={{
                    fontSize: "13px",
                    color: CINZA,
                    lineHeight: 1.55,
                    borderTop: "1px solid oklch(0.94 0.006 150)",
                    paddingTop: "12px",
                    marginBottom: "16px",
                  }}
                >
                  <div>
                    {intl.formatMessage(
                      { id: "encaminhamentos.oQueIriaJunto" },
                      {
                        escopos: (pedido.scopes ?? [])
                          .map((escopo) =>
                            intl.formatMessage({ id: `encaminhar.escopo.${escopo}` }),
                          )
                          .join(" · "),
                      },
                    )}
                  </div>
                  <div>
                    {intl.formatMessage(
                      { id: "encaminhamentos.prazo" },
                      { dias: pedido.accessDays ?? 90 },
                    )}
                  </div>
                </div>

                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                  <button
                    type="button"
                    disabled={autorizar.isPending || recusar.isPending}
                    onClick={() => autorizar.mutate(pedido.referralId ?? "")}
                    style={botaoPrincipal}
                  >
                    {intl.formatMessage({ id: "encaminhamentos.autorizar" })}
                  </button>
                  <button
                    type="button"
                    disabled={autorizar.isPending || recusar.isPending}
                    onClick={() => recusar.mutate(pedido.referralId ?? "")}
                    style={botaoSecundario}
                  >
                    {intl.formatMessage({ id: "encaminhamentos.recusar" })}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}

        {/* ------------------------------------------------------- o que chegou para mim */}
        <h2 style={{ ...subtitulo, marginTop: "36px" }}>
          {intl.formatMessage({ id: "encaminhamentos.paraMim" })}
        </h2>

        {paraMim.length === 0 ? (
          <p style={vazio}>{intl.formatMessage({ id: "encaminhamentos.nadaParaMim" })}</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
            {paraMim.map((pedido) => (
              <article key={pedido.referralId} style={cartao}>
                <h3 style={{ ...subtitulo, fontSize: "19px", margin: "0 0 8px" }}>
                  {/*
                   * O PENDENTE NAO TEM O NOME DO ANIMAL, e nao e falha de carga: o tutor ainda nao
                   * autorizou nada, e o nome ja e informacao sobre um animal que nao e desta pessoa.
                   * A frase diz isso, em vez de deixar um espaco vazio.
                   */}
                  {pedido.status === "PENDENTE"
                    ? intl.formatMessage(
                        { id: "encaminhamentos.recebido.pendente" },
                        { quem: nomeDeQuemEncaminhou(pedido) },
                      )
                    : intl.formatMessage(
                        { id: "encaminhamentos.recebido.titulo" },
                        {
                          quem: nomeDeQuemEncaminhou(pedido),
                          animal: pedido.animalName ?? "",
                        },
                      )}
                </h3>

                <p style={{ fontSize: "15px", lineHeight: 1.65, margin: "0 0 12px" }}>
                  {pedido.reason}
                </p>

                <div style={{ fontSize: "13px", color: CINZA, lineHeight: 1.55 }}>
                  {pedido.status === "AUTORIZADO" &&
                  pedido.accessExpiresAt !== null &&
                  pedido.accessExpiresAt !== undefined
                    ? intl.formatMessage(
                        { id: "encaminhamentos.acessoAte" },
                        { quando: intl.formatDate(pedido.accessExpiresAt) },
                      )
                    : intl.formatMessage({
                        id:
                          pedido.status === "RECUSADO"
                            ? "encaminhamentos.recebido.recusado"
                            : "encaminhamentos.recebido.esperando",
                      })}
                </div>
              </article>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/** "Ana Ferreira, pela Clínica Vet Norte" — ou só o nome, no autônomo. */
function nomeDeQuemEncaminhou(pedido: Encaminhamento) {
  const nome = pedido.referredByName ?? "";

  return pedido.fromOrganizationName === null || pedido.fromOrganizationName === undefined
    ? nome
    : `${nome}, pela ${pedido.fromOrganizationName}`;
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "30px",
  fontWeight: 500,
  margin: "0 0 8px",
  letterSpacing: "-0.02em",
} as const;

const subtitulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "20px",
  fontWeight: 500,
  margin: "0 0 14px",
} as const;

const cartao = {
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.86 0.008 150)",
  borderRadius: "12px",
  padding: "22px 24px",
} as const;

const vazio = {
  fontSize: "15px",
  lineHeight: 1.65,
  color: CINZA,
  margin: 0,
} as const;

const botaoPrincipal = {
  fontFamily: "inherit",
  fontSize: "15px",
  fontWeight: 500,
  color: "oklch(1 0 0)",
  background: VERDE,
  border: "none",
  borderRadius: "8px",
  padding: "12px 20px",
  minHeight: "48px",
  cursor: "pointer",
} as const;

const botaoSecundario = {
  fontFamily: "inherit",
  fontSize: "15px",
  fontWeight: 500,
  color: "oklch(0.25 0.02 150)",
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "8px",
  padding: "12px 20px",
  minHeight: "48px",
  cursor: "pointer",
} as const;

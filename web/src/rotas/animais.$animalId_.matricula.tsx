import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroDeCarga } from "../componentes/Estados.tsx";
import { useAnimal, useCondicoes } from "../dados/carteira.ts";
import { useOrientacoes } from "../dados/animal.ts";
import {
  FRASE_DA_COMPROVACAO,
  useMatriculasDoAnimal,
  type LinhaDaComprovacao,
  type Matricula,
} from "../dados/creche.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 10 · area de organizacao — Matricula na creche, o produto responde pela saude", de
 * `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * <b>O titulo do desenho e a tese, e ela e uma decisao de arquitetura:</b> "o produto responde pela
 * saude". A creche nao julga se o animal esta apto e o tutor nao precisa provar nada — o servidor
 * cruza o que a organizacao exige com o que a carteira tem, e devolve o resultado linha por linha,
 * com o `blocks` pronto. Esta tela nao recalcula nada; se recalculasse, o dia em que ela divergisse
 * do servidor seria o dia em que um animal com antirrabica vencida entraria na creche.
 *
 * <b>Ela e a mesma tela dos dois lados.</b> O desenho e da area de organizacao, e a rota que
 * alimenta esta pagina (`GET /animals/{id}/enrollments`) e do lado do TUTOR: quem concedeu acesso
 * precisa ver o que a creche esta esperando dele — "falta a antirrabica em dia" — sem ser membro de
 * creche nenhuma. A comprovacao vem igual para os dois, porque e a mesma verdade.
 *
 * <b>A FRASE MAIS IMPORTANTE DA TELA E SOBRE O QUE ELA NAO MOSTRA:</b> "o historico completo de
 * atendimentos do Code existe e nao foi compartilhado com a Creche Quintal. Isso e diferente de nao
 * existir." Ela fica, literal, porque e a unica linha do produto que ensina a diferenca entre
 * ausencia de dado e ausencia de acesso — e e a diferenca que o escopo existe para manter.
 *
 * <b>"Avisar Marcelo Dias" nao existe</b>, e o motivo aparece no lugar do botao: nao ha canal de
 * aviso para matricula. A matricula pendente se completa sozinha quando a dose for registrada, e e
 * isso que a tela promete — em vez de prometer uma mensagem que ninguem manda.
 */

export const Route = createFileRoute("/animais/$animalId_/matricula")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: MatriculaDoAnimal,
});

function MatriculaDoAnimal() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const matriculas = useMatriculasDoAnimal(animalId);
  const condicoes = useCondicoes(animalId);
  const orientacoes = useOrientacoes(animalId);

  const nome = animal.data?.name ?? "";
  const lista = matriculas.data ?? [];

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "40px 48px 44px" }}>
        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "matricula.titulo" }, { nome })}
        </h1>
        <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "64ch" }}>
          {intl.formatMessage({ id: "matricula.apoio" })}
        </p>

        {matriculas.isError ? (
          <ErroDeCarga
            oQue={intl.formatMessage({ id: "matricula.oQue" })}
            erro={matriculas.error}
            aoTentarDeNovo={() => void matriculas.refetch()}
            carregando={matriculas.isFetching}
          />
        ) : matriculas.isPending ? (
          <Carregando oQue={intl.formatMessage({ id: "matricula.oQue" })} />
        ) : lista.length === 0 ? (
          <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "matricula.vazio" }, { nome })}
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {lista.map((matricula) => (
              <CartaoDaMatricula
                key={matricula.enrollmentId}
                matricula={matricula}
                nome={nome}
                condicoes={(condicoes.data ?? [])
                  .map((condicao) => condicao.description ?? "")
                  .filter((texto) => texto !== "")}
                orientacoes={(orientacoes.data ?? [])
                  .filter((orientacao) => orientacao.vigente !== false)
                  .map((orientacao) => orientacao.description ?? "")
                  .filter((texto) => texto !== "")}
              />
            ))}
          </div>
        )}

        <div style={{ marginTop: "28px" }}>
          <Link to="/animais/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
            {intl.formatMessage({ id: "matricula.voltar" }, { nome })}
          </Link>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function CartaoDaMatricula({
  matricula,
  nome,
  condicoes,
  orientacoes,
}: {
  matricula: Matricula;
  nome: string;
  condicoes: string[];
  orientacoes: string[];
}) {
  const intl = useIntl();
  const comprovacao = matricula.healthProof ?? [];
  const pendente = matricula.status === "PENDENTE";

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "16px", padding: "18px 24px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", flexWrap: "wrap" }}>
        <div>
          <div style={{ fontSize: "17px", fontWeight: 500 }}>{matricula.organizationName}</div>
          <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
            {intl.formatMessage(
              { id: "matricula.turma" },
              { turma: matricula.classGroupName ?? "" },
            )}
          </div>
        </div>

        <div style={{ fontSize: "14px", fontWeight: 500, color: pendente ? "oklch(0.45 0.09 70)" : "oklch(0.38 0.07 150)" }}>
          {intl.formatMessage({ id: `matricula.status.${matricula.status ?? "PENDENTE"}` })}
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1fr", gap: "28px", padding: "24px" }}>
        {/* ------------------------------------------------- a comprovacao de saude */}
        <div>
          <Rotulo>{intl.formatMessage({ id: "matricula.comprovacao" })}</Rotulo>

          {comprovacao.length === 0 ? (
            <Nota>{intl.formatMessage({ id: "matricula.comprovacao.semExigencia" })}</Nota>
          ) : (
            <div style={{ display: "flex", flexDirection: "column" }}>
              {comprovacao.map((linha, indice) => (
                <LinhaDaProva key={linha.vaccineCatalogId ?? indice} linha={linha} primeira={indice === 0} />
              ))}
            </div>
          )}

          {pendente && (
            <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "8px", padding: "14px 16px", marginTop: "18px" }}>
              <div style={{ fontSize: "15px", fontWeight: 500, marginBottom: "4px" }}>
                {intl.formatMessage({ id: "matricula.pendente.titulo" })}
              </div>
              <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
                {intl.formatMessage({ id: "matricula.pendente.texto" })}
              </div>
              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "10px" }}>
                {intl.formatMessage({ id: "matricula.pendente.semAviso" })}
              </div>
            </div>
          )}
        </div>

        {/* ------------------------------------------- o que a creche precisa saber */}
        <div>
          <Rotulo>{intl.formatMessage({ id: "matricula.precisaSaber" })}</Rotulo>

          {condicoes.length === 0 && orientacoes.length === 0 ? (
            <Nota>{intl.formatMessage({ id: "matricula.precisaSaber.vazio" }, { nome })}</Nota>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px", fontSize: "15px", lineHeight: 1.5 }}>
              {[...condicoes, ...orientacoes].map((texto) => (
                <div key={texto} style={{ display: "flex", alignItems: "flex-start", gap: "10px" }}>
                  <span aria-hidden style={{ width: "10px", height: "10px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none", marginTop: "6px" }}></span>
                  <span>{texto}</span>
                </div>
              ))}
            </div>
          )}

          {/*
           * A frase mais importante da tela, e ela e sobre o que a tela NAO mostra. E a unica linha
           * do produto que ensina a diferenca entre ausencia de dado e ausencia de acesso.
           */}
          <div style={{ fontSize: "14px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", marginTop: "18px", borderTop: "1px solid oklch(0.95 0.005 150)", paddingTop: "14px" }}>
            {intl.formatMessage(
              { id: "matricula.naoCompartilhado" },
              { nome, organizacao: matricula.organizationName ?? "" },
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/** Uma linha da comprovacao: o que a creche exige, e o que a carteira responde. */
function LinhaDaProva({ linha, primeira }: { linha: LinhaDaComprovacao; primeira: boolean }) {
  const intl = useIntl();
  const estado = linha.state ?? "SEM_REGISTRO";
  const impede = linha.blocks === true;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px", alignItems: "flex-start", padding: "14px 0", borderTop: primeira ? "none" : "1px solid oklch(0.95 0.005 150)" }}>
      <div>
        <div style={{ fontSize: "16px" }}>{linha.vaccineName}</div>
        <div style={{ fontSize: "14px", color: impede ? "oklch(0.45 0.13 30)" : "oklch(0.5 0.015 150)", marginTop: "3px", lineHeight: 1.5 }}>
          {estado === "EM_DIA"
            ? linha.nextDoseDate === undefined
              ? intl.formatMessage({ id: "matricula.linha.emDiaSemPrazo" })
              : intl.formatMessage(
                  { id: "matricula.linha.emDia" },
                  { data: intl.formatDate(new Date(`${linha.nextDoseDate}T12:00:00`), { dateStyle: "short" }) },
                )
            : estado === "VENCIDA"
              ? intl.formatMessage(
                  { id: "matricula.linha.vencida" },
                  {
                    data:
                      linha.nextDoseDate === undefined
                        ? ""
                        : intl.formatDate(new Date(`${linha.nextDoseDate}T12:00:00`), { dateStyle: "short" }),
                  },
                )
              : intl.formatMessage({ id: "matricula.linha.semRegistro" })}

          {/*
           * O aviso de casamento por nome. A `Vaccine.catalog` e nula em registro antigo e em texto
           * livre, e o servidor casa por nome nesse caso — quem le precisa saber que a prova e mais
           * fraca ANTES de deixar um animal entrar por causa dela.
           */}
          {linha.matchedByName === true && (
            <div style={{ marginTop: "4px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "matricula.linha.porNome" })}
            </div>
          )}
        </div>
      </div>

      <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px", color: "oklch(0.5 0.015 150)", textAlign: "right" }}>
        <div>{intl.formatMessage({ id: FRASE_DA_COMPROVACAO[estado] ?? "matricula.linha.semRegistro" })}</div>
        {linha.lastApplicationDate !== undefined && (
          <div style={{ marginTop: "3px" }}>
            {intl.formatDate(new Date(`${linha.lastApplicationDate}T12:00:00`), { dateStyle: "short" })}
          </div>
        )}
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

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)", lineHeight: 1.6 }}>{children}</div>;
}

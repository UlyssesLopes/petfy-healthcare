import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAnimal } from "../dados/carteira.ts";
import { useTutores } from "../dados/animal.ts";
import {
  FRASE_DA_LEITURA,
  FRASE_DO_ESCOPO,
  useAcessosDeOrganizacao,
  useLeituras,
  useRevogarAcesso,
  type AcessoDeOrganizacao,
  type Leitura,
} from "../dados/acessos.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 22 · area do tutor — Quem alcanca o Code, e o que cada um leu", de
 * `design/IdentidadeVisual/Telas Petfy - Confianca e operacao.dc.html`.
 *
 * O markup vem do arquivo. O que ela resolve, e a frase esta na propria tela: "conceder
 * deixa de ser um ato de fe quando voce ve o que acontece depois".
 *
 * <b>A LINHA VERMELHA DO DESENHO NAO EXISTE AQUI, e nao e esquecimento.</b> Ele mostra uma
 * tentativa NEGADA — "Douglas Prado tentou abrir o historico clinico, sem acesso" — e o
 * `sensitive_access_log` so grava leitura que aconteceu. Nao ha o que exibir, e inventar a
 * linha seria pior que nao ter: ela e a prova de que o limite funcionou, e uma prova falsa
 * nao prova nada. Fica registrado como o que falta no backend.
 */

export const Route = createFileRoute("/animais/$animalId_/quem-cuida")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: QuemCuida,
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

function QuemCuida() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const acessos = useAcessosDeOrganizacao(animalId);
  const leituras = useLeituras(animalId);
  const tutores = useTutores(animalId);

  const nome = animal.data?.name ?? "";

  const porOrganizacao = new Map<string, Leitura[]>();
  for (const leitura of leituras.data?.content ?? []) {
    const chave = leitura.organizationName ?? "";
    porOrganizacao.set(chave, [...(porOrganizacao.get(chave) ?? []), leitura]);
  }

  const lista = acessos.data ?? [];
  const vigentes = lista.filter((a) => a.active !== false);
  const encerrados = lista.filter((a) => a.active === false);

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>

        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
            </div>
            <Link to="/" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "acesso.voltar" })}
            </Link>
          </div>
          <Link to="/animais/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.42 0.015 150)" }}>
            {nome}
          </Link>
        </div>

        <div style={{ padding: "32px 40px 44px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "acesso.titulo" }, { nome })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 28px", maxWidth: "62ch" }}>
            {intl.formatMessage({ id: "acesso.apoio" })}
          </p>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 420px", gap: "32px", alignItems: "start" }}>

            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {acessos.isError ? (
                <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: 0 }}>
                  {intl.formatMessage({ id: chaveDoErro(acessos.error) })}
                </p>
              ) : acessos.isPending ? (
                <Nota>{intl.formatMessage({ id: "acesso.carregando" })}</Nota>
              ) : lista.length === 0 ? (
                /*
                 * O desenho nao desenha o vazio desta tela, e sem uma saida aqui a Tela 09
                 * seria inalcancavel por quem nunca concedeu nada — que e exatamente quem
                 * mais precisa dela. A secao 06 pede vazio que ofereca o gesto, e nao
                 * vazio que so informe.
                 */
                <div style={{ border: "1px dashed oklch(0.90 0.008 150)", borderRadius: "12px", padding: "24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                  <div>{intl.formatMessage({ id: "acesso.vazio" }, { nome })}</div>
                  <Link
                    to="/animais/$animalId/conceder-acesso"
                    params={{ animalId }}
                    style={{ display: "inline-block", marginTop: "14px", fontSize: "15px", color: "oklch(0.46 0.085 150)" }}
                  >
                    {intl.formatMessage({ id: "acesso.vazio.acao" })}
                  </Link>
                </div>
              ) : (
                <>
                  {vigentes.map((acesso) => (
                    <CartaoDeOrganizacao
                      key={acesso.grantId}
                      acesso={acesso}
                      animalId={animalId}
                      leituras={porOrganizacao.get(acesso.organizationName ?? "") ?? []}
                    />
                  ))}
                  {encerrados.map((acesso) => (
                    <Encerrado key={acesso.grantId} acesso={acesso} animalId={animalId} />
                  ))}
                </>
              )}
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
                <Rotulo>{intl.formatMessage({ id: "acesso.pessoas" })}</Rotulo>

                {(tutores.data ?? []).length === 0 ? (
                  <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                    {intl.formatMessage({ id: "acesso.pessoas.vazio" })}
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                    {(tutores.data ?? []).map((tutor) => (
                      <div key={tutor.vinculoId ?? tutor.personId} style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                        <div aria-hidden style={{ width: "36px", height: "36px", borderRadius: "999px", background: tutor.holder === true ? "oklch(0.90 0.03 150)" : "oklch(0.90 0.012 150)", flex: "none" }}></div>
                        <div style={{ flex: 1 }}>
                          <div style={{ fontSize: "15px", fontWeight: 500 }}>{tutor.personName}</div>
                          <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                            {intl.formatMessage(
                              { id: tutor.holder === true ? "acesso.pessoa.titular" : "acesso.pessoa.coTutor" },
                              { nome },
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "22px 24px" }}>
                <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "10px" }}>
                  {intl.formatMessage({ id: "acesso.revogar.titulo" })}
                </div>
                <div style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)" }}>
                  {intl.formatMessage({ id: "acesso.revogar.texto" })}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "12px" }}>
      {children}
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

function CartaoDeOrganizacao({
  acesso,
  animalId,
  leituras,
}: {
  acesso: AcessoDeOrganizacao;
  animalId: string;
  leituras: Leitura[];
}) {
  const intl = useIntl();
  const ajustar = useHover();
  const revogar = useRevogarAcesso();

  const ultimas = [...leituras]
    .sort((a, b) => (b.accessedAt ?? "").localeCompare(a.accessedAt ?? ""))
    .slice(0, 3);

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", overflow: "hidden" }}>
      <div style={{ padding: "20px 24px", display: "grid", gridTemplateColumns: "44px 1fr auto", gap: "16px", alignItems: "center", borderBottom: "1px solid oklch(0.94 0.006 150)" }}>
        <div aria-hidden style={{ width: "44px", height: "44px", borderRadius: "999px", background: "oklch(0.90 0.03 150)" }}></div>

        <div>
          <div style={{ fontSize: "17px", fontWeight: 500 }}>{acesso.organizationName}</div>
          <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
            {oQueVe(acesso, intl)}
          </div>
        </div>

        <div style={{ display: "flex", gap: "10px" }}>
          {/*
           * "Ajustar" e a Tela 09 de novo, e nao uma tela propria: o POST de conceder
           * reativa a concessao vigente em vez de acumular linhas, entao ajustar o que a
           * organizacao ve e o mesmo gesto para a API. Era este caminho que respondia 500
           * antes do `@Transactional` no `grant`.
           */}
          <Link
            to="/animais/$animalId/conceder-acesso"
            params={{ animalId }}
            {...ajustar.props}
            style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: ajustar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: `1px solid ${ajustar.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, borderRadius: "8px", padding: "11px 16px", minHeight: "44px", display: "flex", alignItems: "center", textDecoration: "none" }}
          >
            {intl.formatMessage({ id: "acesso.acao.ajustar" })}
          </Link>

          <button
            type="button"
            disabled={revogar.isPending || acesso.organizationId === undefined}
            onClick={() => revogar.mutate({ animalId, organizationId: acesso.organizationId! })}
            style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.42 0.13 30)", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.03 30)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", cursor: "pointer" }}
          >
            {intl.formatMessage({ id: revogar.isPending ? "acesso.acao.revogando" : "acesso.acao.revogar" })}
          </button>
        </div>
      </div>

      <div style={{ padding: "16px 24px 20px" }}>
        <Rotulo>{intl.formatMessage({ id: "acesso.ultimosAcessos" })}</Rotulo>

        {ultimas.length === 0 ? (
          <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
            {intl.formatMessage({ id: "acesso.ultimosAcessos.vazio" })}
          </div>
        ) : (
          <>
            <div style={{ display: "flex", flexDirection: "column", gap: "11px", fontSize: "15px" }}>
              {ultimas.map((leitura) => (
                <div key={leitura.sensitiveAccessLogId} style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px" }}>
                  <span>
                    {intl.formatMessage(
                      { id: "acesso.leitura.linha" },
                      {
                        quem: leitura.actorName ?? "",
                        o_que: intl.formatMessage({
                          id: FRASE_DA_LEITURA[leitura.resource ?? ""] ?? "acesso.leitura.desconhecida",
                        }),
                      },
                    )}
                  </span>
                  <span style={{ fontFamily: "'DM Mono', monospace", fontSize: "14px", color: "oklch(0.5 0.015 150)" }}>
                    {leitura.accessedAt === undefined
                      ? ""
                      : intl.formatDate(new Date(leitura.accessedAt), { dateStyle: "short", timeStyle: "short" })}
                  </span>
                </div>
              ))}
            </div>

            {leituras.length > ultimas.length && (
              <div style={{ fontSize: "14px", marginTop: "14px", color: "oklch(0.5 0.015 150)" }}>
                {intl.formatMessage({ id: "acesso.leitura.total" }, { total: leituras.length })}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

/** O acesso que acabou: cinza, e com a saida de "conceder de novo" ao lado. */
function Encerrado({ acesso, animalId }: { acesso: AcessoDeOrganizacao; animalId: string }) {
  const intl = useIntl();
  const conceder = useHover();

  return (
    <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "20px 24px", display: "grid", gridTemplateColumns: "44px 1fr auto", gap: "16px", alignItems: "center" }}>
      <div aria-hidden style={{ width: "44px", height: "44px", borderRadius: "999px", background: "oklch(0.93 0.008 150)" }}></div>

      <div>
        <div style={{ fontSize: "17px", fontWeight: 500, color: "oklch(0.45 0.015 150)" }}>
          {acesso.organizationName}
        </div>
        <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
          {intl.formatMessage(
            { id: acesso.revokedAt !== undefined ? "acesso.encerrado.revogado" : "acesso.encerrado.venceu" },
            {
              data: intl.formatDate(new Date(acesso.revokedAt ?? acesso.expiresAt ?? Date.now()), {
                dateStyle: "short",
              }),
            },
          )}
        </div>
      </div>

      <Link
        to="/animais/$animalId/conceder-acesso"
        params={{ animalId }}
        {...conceder.props}
        style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: conceder.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: `1px solid ${conceder.sobre ? "oklch(0.46 0.085 150)" : "oklch(0.82 0.012 150)"}`, borderRadius: "8px", padding: "11px 16px", minHeight: "44px", display: "flex", alignItems: "center", textDecoration: "none" }}
      >
        {intl.formatMessage({ id: "acesso.acao.concederDeNovo" })}
      </Link>
    </div>
  );
}

/**
 * O que a organizacao ve, em frase — nunca a lista de constantes.
 *
 * "Le a saude e registra atendimentos · ate 31/12/2027" e o formato do desenho: o QUE
 * primeiro, o PRAZO depois. Sem prazo escrito, quem concedeu nao sabe que concedeu para
 * sempre.
 */
function oQueVe(acesso: AcessoDeOrganizacao, intl: ReturnType<typeof useIntl>): string {
  const escopos = (acesso.scopes ?? [])
    .map((escopo) => FRASE_DO_ESCOPO[escopo])
    .filter((chave): chave is string => chave !== undefined)
    .map((chave) => intl.formatMessage({ id: chave }));

  const oQue =
    escopos.length === 0
      ? intl.formatMessage({ id: "acesso.escopo.nenhum" })
      : new Intl.ListFormat("pt-BR", { style: "long", type: "conjunction" }).format(escopos);

  const prazo =
    acesso.expiresAt === undefined
      ? intl.formatMessage({ id: "acesso.semPrazo" })
      : intl.formatMessage(
          { id: "acesso.ate" },
          { data: intl.formatDate(new Date(acesso.expiresAt), { dateStyle: "short" }) },
        );

  return `${oQue} · ${prazo}`;
}

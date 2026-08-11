import { createFileRoute, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import {
  useAjustarFuncao,
  useConvidarParaEquipe,
  useConvitesDaOrganizacao,
  useDesligarDaEquipe,
  useEquipe,
  useRevogarConvite,
  type FuncaoNaEquipe,
} from "../dados/organizacoes.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 16 Â· administracao â€” Equipe, funcoes e desligamento", de
 * `design/IdentidadeVisual/Telas Petfy - Organizacao.dc.html`.
 *
 * <b>A TELA ESTAVA A UM QUARTO DO DESENHO, e o que faltava era backend.</b> Ela dizia isso em
 * vez de desenhar uma tabela vazia â€” a armadilha que a secao 06 nomeia: <b>vazio nao e a mesma
 * coisa que nao existe</b>. Uma tabela de equipe com "nenhum membro" seria mentira, porque os
 * membros existiam e o produto e que nao sabia mostra-los.
 *
 * As tres coisas que faltavam entraram, e por isso a tela tem agora as quatro colunas do
 * desenho â€” pessoa, funcao, o que ela registra e "ajustar" â€” mais o desligamento:
 *
 * <ul>
 *   <li><b>`GET /organizations/members`</b> devolve quem ja entrou, com funcao, "desde" e o
 *       registro profissional de quem tem.</li>
 *   <li><b>A funcao viaja no CONVITE</b>, escolhida por quem convida. Ela nao esta no cadastro
 *       de quem aceita de proposito: escolher a propria funcao e escolher a propria
 *       permissao.</li>
 *   <li><b>Ajustar e desligar existem</b>, so para administrador. Desligar marca a saida e nao
 *       apaga o vinculo, porque o que a pessoa registrou continua no historico dos animais.</li>
 * </ul>
 *
 * <b>A ultima parte que dependia de backend fechou.</b> Quem JA tem conta no Petfy agora aceita
 * o convite pela tela `/convites/aceitar`, sem precisar criar uma segunda conta com outro e-mail
 * â€” que era o que o `inviteToken` do `PersonRequestDTO`, unico caminho de aceite ate aqui, na
 * pratica exigia da veterinaria que ja usava o produto.
 */

export const Route = createFileRoute("/organizacoes/$organizationId/equipe")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Equipe,
});

/**
 * As quatro funcoes, na ordem do que pode menos para o que pode mais.
 *
 * A ordem nao e alfabetica de proposito: o primeiro item e o default do campo de convite, e o
 * default tem de ser o que pode menos. Promover alguem e um ato escolhido.
 */
const FUNCOES: FuncaoNaEquipe[] = ["VOLUNTARIO", "MONITOR", "VETERINARIO", "ADMINISTRADOR"];

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

function Equipe() {
  const { organizationId } = Route.useParams();
  const intl = useIntl();

  const convites = useConvitesDaOrganizacao();
  const convidar = useConvidarParaEquipe();
  const revogar = useRevogarConvite();
  const equipe = useEquipe();
  const ajustar = useAjustarFuncao();
  const desligar = useDesligarDaEquipe();
  const acao = useHover();

  const [email, setEmail] = useState("");
  /* ADMINISTRADOR nao e o default: promover alguem tem de ser um ato escolhido, e nao o que
     acontece quando ninguem mexe no campo. VOLUNTARIO e a funcao que pode menos. */
  const [funcao, setFuncao] = useState<FuncaoNaEquipe>("VOLUNTARIO");

  const daOrganizacao = (convites.data ?? []).filter(
    (convite) => convite.organizationId === organizationId,
  );
  const aguardando = daOrganizacao.filter((convite) => convite.usable !== false);
  const encerrados = daOrganizacao.filter((convite) => convite.usable === false);

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        {/* A marca, o "voltar" e o "Agindo como" viraram a moldura do produto: o "Hoje" dela
            leva ao mesmo lugar que o voltar levava, e o contexto ativo ja vive no cabecalho. */}

        <div style={{ padding: "32px 40px 44px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 8px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "equipe.titulo" })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 28px", maxWidth: "64ch" }}>
            {intl.formatMessage({ id: "equipe.apoio" })}
          </p>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 380px", gap: "32px", alignItems: "start" }}>
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              {/* --------------------------------------------------------- convidar */}
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
                <Rotulo>{intl.formatMessage({ id: "equipe.convidar" })}</Rotulo>

                <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                  <input
                    type="email"
                    value={email}
                    onChange={(evento) => setEmail(evento.target.value)}
                    placeholder={intl.formatMessage({ id: "equipe.convidar.campo" })}
                    aria-label={intl.formatMessage({ id: "equipe.convidar.campo" })}
                    style={{ fontFamily: "inherit", flex: 1, minWidth: "220px", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", background: "oklch(1 0 0)" }}
                  />
                  <select
                    value={funcao}
                    onChange={(evento) => setFuncao(evento.target.value as FuncaoNaEquipe)}
                    aria-label={intl.formatMessage({ id: "equipe.funcao.rotulo" })}
                    style={{ fontFamily: "inherit", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", background: "oklch(1 0 0)" }}
                  >
                    {FUNCOES.map((opcao) => (
                      <option key={opcao} value={opcao}>
                        {intl.formatMessage({ id: `equipe.funcao.${opcao}` })}
                      </option>
                    ))}
                  </select>

                  <button
                    type="button"
                    disabled={convidar.isPending || email.trim() === ""}
                    {...acao.props}
                    onClick={async () => {
                      await convidar.mutateAsync({ email: email.trim(), funcao });
                      setEmail("");
                    }}
                    style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: convidar.isPending || email.trim() === "" ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", cursor: email.trim() === "" ? "not-allowed" : "pointer" }}
                  >
                    {intl.formatMessage({ id: convidar.isPending ? "equipe.convidando" : "equipe.convidar.acao" })}
                  </button>
                </div>

                <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "12px" }}>
                  {intl.formatMessage({ id: "equipe.convidar.apoio" })}
                </div>

                {convidar.isError && (
                  <div style={{ marginTop: "14px" }}>
                    <ErroAoGravar erro={convidar.error} oQue={intl.formatMessage({ id: "equipe.oQue.convite" })} />
                  </div>
                )}
              </div>

              {/* ------------------------------------------------------------ a equipe */}
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
                <Rotulo>
                  {intl.formatMessage({ id: "equipe.tabela" }, { quantos: (equipe.data ?? []).length })}
                </Rotulo>

                {equipe.isError ? (
                  <ErroDeCarga
                    oQue={intl.formatMessage({ id: "equipe.oQue.equipe" })}
                    erro={equipe.error}
                    aoTentarDeNovo={() => void equipe.refetch()}
                    carregando={equipe.isFetching}
                  />
                ) : equipe.isPending ? (
                  <Nota>{intl.formatMessage({ id: "equipe.carregando.membros" })}</Nota>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    {(equipe.data ?? []).map((membro) => (
                      <div
                        key={membro.membershipId}
                        style={{ display: "grid", gridTemplateColumns: "1fr auto auto", gap: "16px", alignItems: "center", borderTop: "1px solid oklch(0.95 0.005 150)", paddingTop: "12px" }}
                      >
                        <div>
                          <div style={{ fontSize: "16px" }}>{membro.personName}</div>
                          <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                            {membro.joinedAt === undefined
                              ? ""
                              : intl.formatMessage(
                                  { id: "equipe.membro.desde" },
                                  { data: intl.formatDate(new Date(membro.joinedAt), { month: "2-digit", year: "numeric" }) },
                                )}
                            {/*
                              O CRMV so aparece de quem tem, e a ausencia NAO vira "sem registro":
                              monitor e voluntario nao tem credencial e nao ha nada de pendente
                              nisso. Dizer o vazio aqui inventaria uma cobranca.
                            */}
                            {membro.professionalCredential !== undefined
                              ? ` Â· ${membro.professionalCredential}`
                              : ""}
                          </div>
                        </div>

                        <select
                          value={membro.role}
                          disabled={ajustar.isPending || membro.membershipId === undefined}
                          onChange={(evento) =>
                            ajustar.mutate({
                              membershipId: membro.membershipId!,
                              funcao: evento.target.value as FuncaoNaEquipe,
                            })
                          }
                          aria-label={intl.formatMessage(
                            { id: "equipe.membro.ajustar" },
                            { pessoa: membro.personName ?? "" },
                          )}
                          style={{ fontFamily: "inherit", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "11px 12px", fontSize: "15px", minHeight: "44px", background: "oklch(1 0 0)" }}
                        >
                          {FUNCOES.map((opcao) => (
                            <option key={opcao} value={opcao}>
                              {intl.formatMessage({ id: `equipe.funcao.${opcao}` })}
                            </option>
                          ))}
                        </select>

                        <button
                          type="button"
                          disabled={desligar.isPending || membro.membershipId === undefined}
                          onClick={() => desligar.mutate(membro.membershipId!)}
                          style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.42 0.13 30)", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.03 30)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", cursor: "pointer" }}
                        >
                          {intl.formatMessage({ id: "equipe.membro.desligar" })}
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/*
                  As duas recusas do servidor aparecem AQUI, e nao ao lado do controle: quem nao
                  administra recebe 151 e quem tentou tirar o ultimo administrador recebe 152, e
                  as duas frases dizem a saida. Desabilitar o controle sem explicar seria o
                  "desabilitado mudo" que a secao 06 proibe.
                */}
                {ajustar.isError && (
                  <div style={{ marginTop: "14px" }}>
                    <ErroAoGravar erro={ajustar.error} oQue={intl.formatMessage({ id: "equipe.oQue.funcao" })} />
                  </div>
                )}
                {desligar.isError && (
                  <div style={{ marginTop: "14px" }}>
                    <ErroAoGravar erro={desligar.error} oQue={intl.formatMessage({ id: "equipe.oQue.desligamento" })} />
                  </div>
                )}
              </div>

              {/* ------------------------------------------------- quem esta esperando */}
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
                <Rotulo>
                  {intl.formatMessage({ id: "equipe.aguardando" }, { quantos: aguardando.length })}
                </Rotulo>

                {convites.isError ? (
                  <ErroDeCarga
                    oQue={intl.formatMessage({ id: "equipe.oQue" })}
                    erro={convites.error}
                    aoTentarDeNovo={() => void convites.refetch()}
                    carregando={convites.isFetching}
                  />
                ) : convites.isPending ? (
                  <Nota>{intl.formatMessage({ id: "equipe.carregando" })}</Nota>
                ) : aguardando.length === 0 ? (
                  <Nota>{intl.formatMessage({ id: "equipe.aguardando.vazio" })}</Nota>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                    {aguardando.map((convite) => (
                      <div
                        key={convite.organizationInviteId}
                        style={{ display: "grid", gridTemplateColumns: "1fr auto", gap: "16px", alignItems: "center", borderTop: "1px solid oklch(0.95 0.005 150)", paddingTop: "12px" }}
                      >
                        <div>
                          <div style={{ fontSize: "16px" }}>{convite.email}</div>
                          <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "2px" }}>
                            {convite.expiresAt === undefined
                              ? ""
                              : intl.formatMessage(
                                  { id: "equipe.convite.vale" },
                                  { data: intl.formatDate(new Date(convite.expiresAt), { dateStyle: "short" }) },
                                )}
                          </div>
                        </div>

                        <button
                          type="button"
                          disabled={revogar.isPending || convite.organizationInviteId === undefined}
                          onClick={() => revogar.mutate(convite.organizationInviteId!)}
                          style={{ fontFamily: "inherit", fontSize: "14px", fontWeight: 500, color: "oklch(0.42 0.13 30)", background: "oklch(1 0 0)", border: "1px solid oklch(0.86 0.03 30)", borderRadius: "8px", padding: "11px 16px", minHeight: "44px", cursor: "pointer" }}
                        >
                          {intl.formatMessage({ id: "equipe.convite.revogar" })}
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {encerrados.length > 0 && (
                  <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "16px" }}>
                    {intl.formatMessage({ id: "equipe.encerrados" }, { quantos: encerrados.length })}
                  </div>
                )}
              </div>
            </div>

            {/* ---------------------------------------------------------- a direita */}
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {/* O aviso ambar que morava aqui dizia que quem ja tem conta nao conseguia aceitar.
                  Ele saiu junto com o defeito: agora os dois caminhos existem, e o que resta e
                  explicar QUAL deles cada pessoa vai usar â€” que e informacao util, e nao desculpa. */}
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "22px 24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage({ id: "equipe.comoAceita" })}
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
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
      {children}
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

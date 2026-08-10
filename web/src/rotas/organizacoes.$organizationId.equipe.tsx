import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useMeuContexto } from "../dados/contexto.ts";
import {
  useConvidarParaEquipe,
  useConvitesDaOrganizacao,
  useRevogarConvite,
} from "../dados/organizacoes.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 16 · administracao — Equipe, funcoes e desligamento", de
 * `design/IdentidadeVisual/Telas Petfy - Organizacao.dc.html`.
 *
 * <b>ESTA TELA ESTA A UM QUARTO DO QUE O DESENHO PEDE, e a razao e o contrato.</b> O desenho
 * mostra uma tabela de equipe com quatro colunas — pessoa, funcao, o que ela registra e
 * "ajustar" — mais o desligamento. Do contrato inteiro, existe isto:
 *
 * <ul>
 *   <li><b>Nao ha rota que liste MEMBROS.</b> `GET /organizations/invites` devolve convites, e
 *       nada devolve quem ja entrou. A tabela do desenho nao tem de onde sair — nem os nomes,
 *       nem o "desde 03/2024", nem o CRMV ao lado da veterinaria.</li>
 *   <li><b>Nao ha FUNCAO no convite.</b> O `OrganizationInviteRequestDTO` tem e-mail e prazo.
 *       Os papeis (`VETERINARIO`, `MONITOR`, `VOLUNTARIO`, `ADMINISTRADOR`) existem no
 *       `ContextOptionDTO`, mas nao ha por onde escolher um ao convidar nem depois.</li>
 *   <li><b>Nao ha "ajustar" nem desligamento.</b> Sem membro listado e sem papel, nao ha o que
 *       ajustar; e nao existe rota para desligar ninguem.</li>
 * </ul>
 *
 * O que existe e o convite: criar, ver quem esta esperando e revogar. E e isso que esta tela
 * faz, dizendo o resto em vez de desenhar uma tabela vazia — que e a armadilha que a secao 06
 * nomeia: <b>vazio nao e a mesma coisa que nao existe</b>. Uma tabela de equipe com "nenhum
 * membro" seria mentira: os membros existem, e o produto e que nao sabe mostra-los.
 *
 * <b>Como alguem aceita o convite:</b> pelo `inviteToken` do `PersonRequestDTO`, na criacao da
 * conta. Nao ha rota de aceite para quem JA tem conta — quem ja e do Petfy e recebe convite de
 * organizacao nao tem por onde entrar.
 */

export const Route = createFileRoute("/organizacoes/$organizationId/equipe")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Equipe,
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

function Equipe() {
  const { organizationId } = Route.useParams();
  const intl = useIntl();

  const contexto = useMeuContexto();
  const convites = useConvitesDaOrganizacao();
  const convidar = useConvidarParaEquipe();
  const revogar = useRevogarConvite();
  const acao = useHover();

  const [email, setEmail] = useState("");

  const quem = contexto.data?.personName ?? "";
  const daOrganizacao = (convites.data ?? []).filter(
    (convite) => convite.organizationId === organizationId,
  );
  const aguardando = daOrganizacao.filter((convite) => convite.usable !== false);
  const encerrados = daOrganizacao.filter((convite) => convite.usable === false);
  const nomeDaOrganizacao = daOrganizacao[0]?.organizationName ?? "";

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div aria-hidden style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
              </div>
              <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
            </div>
            <Link to="/" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "acesso.voltar" })}
            </Link>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", padding: "8px 14px", minHeight: "44px", background: "oklch(0.975 0.004 150)" }}>
            <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "comecar.agindoComo" })}
            </span>
            <span style={{ fontSize: "15px", fontWeight: 500 }}>{quem}</span>
            {nomeDaOrganizacao !== "" && (
              <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
                {intl.formatMessage({ id: "equipe.pela" }, { organizacao: nomeDaOrganizacao })}
              </span>
            )}
          </div>
        </div>

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
                  <button
                    type="button"
                    disabled={convidar.isPending || email.trim() === ""}
                    {...acao.props}
                    onClick={async () => {
                      await convidar.mutateAsync(email.trim());
                      setEmail("");
                    }}
                    style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: convidar.isPending || email.trim() === "" ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "13px 20px", minHeight: "48px", cursor: email.trim() === "" ? "not-allowed" : "pointer" }}
                  >
                    {intl.formatMessage({ id: convidar.isPending ? "equipe.convidando" : "equipe.convidar.acao" })}
                  </button>
                </div>

                <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "12px" }}>
                  {intl.formatMessage({ id: "equipe.convidar.semFuncao" })}
                </div>

                {convidar.isError && (
                  <div style={{ marginTop: "14px" }}>
                    <ErroAoGravar erro={convidar.error} oQue={intl.formatMessage({ id: "equipe.oQue.convite" })} />
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
              <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "22px 24px" }}>
                <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
                  {intl.formatMessage({ id: "equipe.falta.titulo" })}
                </div>
                <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
                  {intl.formatMessage({ id: "equipe.falta.texto" })}
                </div>
              </div>

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

import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar } from "../componentes/Estados.tsx";
import { useAceitarConvite, usePreviaDoConvite } from "../dados/organizacoes.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O OUTRO LADO da Tela 16: quem recebeu o convite, e nao quem o emitiu.
 *
 * <b>Ele fecha a ultima parte da Tela 16 que dependia de backend.</b> Ate aqui o unico aceite
 * possivel era o `inviteToken` na CRIACAO da conta, e a consequencia era absurda na pratica: a
 * veterinaria que ja usa o Petfy, convidada pela clinica onde passou a atender, so entraria
 * criando uma segunda conta com outro e-mail — e sem nenhum dos animais que ja acompanha. A
 * propria Tela 16 dizia isso num aviso ambar, em vez de desenhar o que nao existia.
 *
 * <b>Le antes de oferecer o botao.</b> Aceitar as cegas nao e aceitar: o que esta em jogo e
 * entrar numa equipe que enxerga a saude de animais alheios, entao a tela mostra de qual
 * organizacao e o convite, quem o fez, com que funcao e ate quando vale — e so entao o botao.
 * O `preview` nao consome nada, porque abrir o link para ler nao pode gastar o direito de entrar.
 *
 * <b>A RECUSA E UM ESTADO SO, E ISSO E DELIBERADO.</b> O servidor responde 111 igual para
 * convite inexistente, expirado, revogado, ja usado e enderecado a outra pessoa — distinguir
 * diria a quem tenta adivinhar qual parte errou. A tela nao pode inventar a distincao que o
 * backend recusa a dar, entao ela lista as possibilidades e manda pedir um convite novo.
 *
 * <b>Quem chega deslogado nao perde o convite.</b> O link chega por fora do produto, e o normal
 * e a pessoa nao estar com a sessao aberta; mandar para `/entrar` e esquecer o token faria o
 * clique morrer ali. O token viaja para a tela de entrar e volta — e volta como TOKEN, e nao
 * como caminho de destino, que seria um redirecionamento aberto disfarcado de conveniencia.
 */

export const Route = createFileRoute("/convites/aceitar")({
  validateSearch: (search: Record<string, unknown>): { token: string } => ({
    token: typeof search.token === "string" ? search.token : "",
  }),
  beforeLoad: ({ search }) => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar", search: { convite: search.token } });
    }
  },
  component: AceitarConvite,
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

function AceitarConvite() {
  const { token } = Route.useSearch();
  const intl = useIntl();
  const navegar = useNavigate();

  const previa = usePreviaDoConvite(token);
  const aceitar = useAceitarConvite();
  const acao = useHover();

  const convite = previa.data;

  /* O 153 nao e falha de gravacao: quem chega nele ja esta onde queria estar, e a tela diz
     isso em vez de pintar de vermelho um caminho que terminou bem. */
  const jaEraMembro = aceitar.isError && chaveDoErro(aceitar.error) === "erro.153";

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "760px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        {/* A marca e o "Agindo como" viraram a moldura do produto. Aqui eles faziam ainda menos
            sentido do que nas outras: quem le esta tela esta decidindo entrar numa equipe, e o
            cabecalho global ja diz em nome de quem ela age hoje. */}

        <div style={{ padding: "40px 48px 44px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 24px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "convite.titulo" })}
          </h1>

          {/* Sem token nao ha o que ler, e isso nao e erro de servidor: e alguem que digitou o
              endereco na mao ou copiou o link pela metade. */}
          {token === "" && (
            <Aviso
              titulo={intl.formatMessage({ id: "convite.semToken.titulo" })}
              texto={intl.formatMessage({ id: "convite.semToken.texto" })}
            />
          )}

          {token !== "" && previa.isPending && (
            <Carregando oQue={intl.formatMessage({ id: "convite.oQue" })} />
          )}

          {token !== "" && previa.isError && (
            <Aviso
              titulo={intl.formatMessage({ id: "convite.invalido.titulo" })}
              texto={intl.formatMessage({ id: "convite.invalido.texto" })}
            />
          )}

          {/* ------------------------------------------------------- o convite, e o aceite */}
          {convite !== undefined && !aceitar.isSuccess && (
            <>
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "26px 28px" }}>
                <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, marginBottom: "14px" }}>
                  {intl.formatMessage(
                    { id: "convite.organizacao" },
                    { organizacao: convite.organizationName ?? "" },
                  )}
                </div>

                <Linha>
                  {intl.formatMessage({ id: "convite.porQuem" }, { quem: convite.invitedByName ?? "" })}
                </Linha>

                {convite.role !== undefined && (
                  <Linha>
                    {intl.formatMessage(
                      { id: "convite.funcao" },
                      { funcao: intl.formatMessage({ id: `equipe.funcao.${convite.role}` }) },
                    )}
                  </Linha>
                )}

                {/* Convite antigo, emitido antes de a funcao existir. A tela diz o que vai
                    acontecer em vez de inventar uma funcao que o convite nao escreveu. */}
                {convite.role === undefined && (
                  <Linha>{intl.formatMessage({ id: "convite.funcao.semFuncao" })}</Linha>
                )}

                {convite.expiresAt !== undefined && (
                  <Linha>
                    {intl.formatMessage(
                      { id: "convite.vale" },
                      { data: intl.formatDate(convite.expiresAt, { dateStyle: "long" }) },
                    )}
                  </Linha>
                )}
              </div>

              {/* Custodia e acesso sao coisas separadas (ROADMAP.md, P2), e quem entra numa
                  equipe precisa saber disso ANTES de entrar: a organizacao comeca vazia. */}
              <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "20px 24px", marginTop: "16px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
                {intl.formatMessage({ id: "convite.oQueMuda" })}
              </div>

              {jaEraMembro && (
                <div style={{ marginTop: "18px" }}>
                  <Aviso
                    titulo={intl.formatMessage({ id: "convite.jaMembro.titulo" })}
                    texto={intl.formatMessage({ id: "convite.jaMembro.texto" })}
                  />
                </div>
              )}

              {aceitar.isError && !jaEraMembro && (
                <div style={{ marginTop: "18px" }}>
                  <ErroAoGravar
                    erro={aceitar.error}
                    oQue={intl.formatMessage({ id: "convite.oQue.aceite" })}
                  />
                </div>
              )}

              <div style={{ marginTop: "26px" }}>
                <button
                  type="button"
                  disabled={aceitar.isPending}
                  {...acao.props}
                  onClick={() => aceitar.mutate(token)}
                  style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: aceitar.isPending ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: aceitar.isPending ? "progress" : "pointer" }}
                >
                  {intl.formatMessage({ id: aceitar.isPending ? "convite.aceitando" : "convite.aceitar" })}
                </button>
              </div>
            </>
          )}

          {/* ------------------------------------------------------------------- entrou */}
          {aceitar.isSuccess && (
            <div style={{ border: "1px solid oklch(0.86 0.05 150)", background: "oklch(0.975 0.012 150)", borderRadius: "12px", padding: "26px 28px" }}>
              <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, marginBottom: "16px" }}>
                {intl.formatMessage(
                  { id: "convite.agora" },
                  { organizacao: aceitar.data?.organizationName ?? "" },
                )}
              </div>

              <button
                type="button"
                onClick={() => {
                  const organizationId = aceitar.data?.organizationId;

                  if (organizationId !== undefined) {
                    void navegar({
                      to: "/organizacoes/$organizationId/equipe",
                      params: { organizationId },
                    });
                  }
                }}
                style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "14px 26px", minHeight: "48px", cursor: "pointer" }}
              >
                {intl.formatMessage({ id: "convite.verEquipe" })}
              </button>
            </div>
          )}

          <div style={{ marginTop: "24px" }}>
            <Link to="/" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
              {intl.formatMessage({ id: "organizacao.nova.voltar" })}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Linha({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "16px", lineHeight: 1.65, color: "oklch(0.35 0.018 150)", marginTop: "6px" }}>
      {children}
    </div>
  );
}

function Aviso({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <div role="status" style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "22px 24px" }}>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>{texto}</div>
    </div>
  );
}

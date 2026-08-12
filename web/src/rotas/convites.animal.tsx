import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState } from "react";
import { useIntl } from "react-intl";

import { Carregando, ErroAoGravar } from "../componentes/Estados.tsx";
import {
  useAceitarConviteDeAnimal,
  usePreviaDoConviteDeAnimal,
  useRecusarConviteDeAnimal,
} from "../dados/convite-de-animal.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * As "Telas 19–21 · quem recebe — Convite, transferencia e adocao", de
 * `design/IdentidadeVisual/Telas Petfy - Petshop e convites.dc.html`.
 *
 * <b>UMA ROTA PARA AS TRES, e o desenho concorda:</b> ele desenha tres cartoes lado a lado com a
 * mesma forma — titulo, o que isso significa, aceitar, recusar. O que muda entre eles e o
 * SIGNIFICADO, e o significado vem do convite: dividir o cuidado, receber a responsabilidade de uma
 * pessoa, ou adotar de um abrigo. Tres rotas seriam a mesma tela tres vezes, e a diferenca ficaria
 * onde ninguem a mantem.
 *
 * ------------------------------------------------------------ o buraco que ela fecha
 *
 * <b>Esta tela nao existia, e dois fluxos ja entregues terminavam nela.</b> A adocao da colonia
 * (Tela 44) cria um convite de titularidade quando a segunda pessoa concorda; a transferencia
 * (`/animais/{id}/transferir`) faz o mesmo. O `POST /pet-tutor-invites/{token}/accept` existia no
 * backend desde o P4 — e o frontend nunca o chamou. Quem recebia o e-mail nao tinha para onde ir.
 *
 * ------------------------------------------------------------ o que ela faz antes de perguntar
 *
 * <b>Le, e ler nao consome.</b> Aceitar as cegas nao e aceitar: o que esta em jogo e ou enxergar a
 * saude inteira de um animal, ou passar a RESPONDER por um. A tela mostra de que animal se trata,
 * quem convidou e o que muda — e so entao os botoes.
 *
 * <b>E o erro e um estado so</b>, como no convite de organizacao: o servidor responde igual para
 * convite inexistente, expirado, revogado, ja usado e enderecado a outra pessoa. Distinguir diria a
 * quem tenta adivinhar qual parte errou.
 */

export const Route = createFileRoute("/convites/animal")({
  validateSearch: (search: Record<string, unknown>): { token: string } => ({
    token: typeof search.token === "string" ? search.token : "",
  }),
  beforeLoad: ({ search }) => {
    if (!lerSessao().autenticada) {
      // o token viaja para a tela de entrar e volta — e volta como TOKEN, e nao como caminho de
      // destino, que seria um redirecionamento aberto disfarcado de conveniencia
      throw redirect({ to: "/entrar", search: { convite: search.token } });
    }
  },
  component: ConviteDeAnimal,
});

function ConviteDeAnimal() {
  const { token } = Route.useSearch();
  const intl = useIntl();

  const previa = usePreviaDoConviteDeAnimal(token);
  const aceitar = useAceitarConviteDeAnimal();
  const recusar = useRecusarConviteDeAnimal();

  const [desfecho, setDesfecho] = useState<"ACEITO" | "RECUSADO" | null>(null);

  if (previa.isPending) {
    return <Carregando oQue={intl.formatMessage({ id: "conviteDeAnimal.oQueE" })} />;
  }

  /*
   * O convite invalido nao e um erro de carga, e a tela nao mostra "tente de novo": tentar de novo
   * nao muda nada. Ela lista o que pode ter acontecido e manda pedir outro.
   */
  if (previa.isError || previa.data === undefined) {
    return (
      <div style={{ padding: "40px 24px" }}>
        <div style={{ ...cartao, maxWidth: "520px" }}>
          <h1 style={titulo}>{intl.formatMessage({ id: "conviteDeAnimal.invalido.titulo" })}</h1>
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 18px" }}>
            {intl.formatMessage({ id: "conviteDeAnimal.invalido.oQuePodeSer" })}
          </p>
          <Link to="/" style={{ color: VERDE }}>
            {intl.formatMessage({ id: "conviteDeAnimal.irParaInicio" })}
          </Link>
        </div>
      </div>
    );
  }

  const convite = previa.data;
  const animal = convite.animalName ?? "";
  const quemConvidou = convite.invitedByName ?? "";

  /*
   * TRES SIGNIFICADOS, UMA MECANICA. `HOLDER` e passar a responder pelo animal; vindo de uma
   * organizacao, isso se chama adotar. As outras funcoes sao dividir o cuidado.
   */
  const eTitularidade = convite.role === "HOLDER";
  const eAdocao = eTitularidade && convite.fromOrganization === true;

  const chaveDoTitulo = eAdocao
    ? "conviteDeAnimal.adocao.titulo"
    : eTitularidade
      ? "conviteDeAnimal.transferencia.titulo"
      : "conviteDeAnimal.coTutoria.titulo";

  const chaveDoResumo = eAdocao
    ? "conviteDeAnimal.adocao.resumo"
    : eTitularidade
      ? "conviteDeAnimal.transferencia.resumo"
      : "conviteDeAnimal.coTutoria.resumo";

  if (desfecho !== null) {
    return (
      <div style={{ padding: "40px 24px" }}>
        <div style={{ ...cartao, maxWidth: "520px" }}>
          <h1 style={titulo}>
            {intl.formatMessage(
              {
                id:
                  desfecho === "ACEITO"
                    ? "conviteDeAnimal.aceito.titulo"
                    : "conviteDeAnimal.recusado.titulo",
              },
              { animal },
            )}
          </h1>
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: CINZA, margin: "0 0 18px" }}>
            {intl.formatMessage(
              {
                id:
                  desfecho === "ACEITO"
                    ? eTitularidade
                      ? "conviteDeAnimal.aceito.agoraVoceResponde"
                      : "conviteDeAnimal.aceito.agoraVoceAcompanha"
                    : "conviteDeAnimal.recusado.nadaMuda",
              },
              { animal, quem: quemConvidou },
            )}
          </p>
          <Link to="/" style={{ color: VERDE }}>
            {intl.formatMessage({
              id: desfecho === "ACEITO" ? "conviteDeAnimal.verAnimais" : "conviteDeAnimal.irParaInicio",
            })}
          </Link>
        </div>
      </div>
    );
  }

  const ocupado = aceitar.isPending || recusar.isPending;

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ ...cartao, maxWidth: "520px" }}>
        <div style={{ fontSize: "13px", color: CINZA, marginBottom: "18px" }}>
          {intl.formatMessage({
            id: eAdocao
              ? "conviteDeAnimal.tipo.adocao"
              : eTitularidade
                ? "conviteDeAnimal.tipo.transferencia"
                : "conviteDeAnimal.tipo.coTutoria",
          })}
        </div>

        <h1 style={{ ...titulo, fontSize: "26px" }}>
          {intl.formatMessage({ id: chaveDoTitulo }, { quem: quemConvidou, animal })}
        </h1>

        <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", margin: "0 0 22px" }}>
          {intl.formatMessage({ id: chaveDoResumo }, { animal })}
        </p>

        {/* ------------------------------------------------------------ o que isso significa */}
        <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", padding: "18px 20px", marginBottom: "20px" }}>
          <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: CINZA, marginBottom: "12px" }}>
            {intl.formatMessage({ id: "conviteDeAnimal.oQueSignifica" })}
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "10px", fontSize: "15px", lineHeight: 1.5 }}>
            <Ponto>{intl.formatMessage({ id: "conviteDeAnimal.significa.avisos" })}</Ponto>
            <Ponto>{intl.formatMessage({ id: "conviteDeAnimal.significa.autoria" })}</Ponto>
            {/*
             * A ultima linha e a que MAIS muda entre as tres telas, e e a que diz o que NAO acontece.
             * Na co-tutoria, quem convidou continua respondendo; na transferencia e na adocao, quem
             * responde passa a ser quem aceita — e quem entregou continua vendo a vida do animal.
             */}
            <Ponto tracejado>
              {eTitularidade
                ? intl.formatMessage(
                    { id: "conviteDeAnimal.significa.voceResponde" },
                    { quem: convite.currentHolderName ?? quemConvidou },
                  )
                : intl.formatMessage(
                    { id: "conviteDeAnimal.significa.eleContinuaRespondendo" },
                    { quem: quemConvidou, animal },
                  )}
            </Ponto>
          </div>
        </div>

        {(aceitar.error !== null && aceitar.error !== undefined) && (
          <div style={{ marginBottom: "14px" }}>
            <ErroAoGravar erro={aceitar.error} oQue={animal} />
          </div>
        )}

        {(recusar.error !== null && recusar.error !== undefined) && (
          <div style={{ marginBottom: "14px" }}>
            <ErroAoGravar erro={recusar.error} oQue={animal} />
          </div>
        )}

        <button
          type="button"
          disabled={ocupado}
          onClick={() => aceitar.mutate(token, { onSuccess: () => setDesfecho("ACEITO") })}
          style={{ ...botao, color: "oklch(1 0 0)", background: VERDE, border: "none", marginBottom: "10px" }}
        >
          {intl.formatMessage({ id: aceitar.isPending ? "conviteDeAnimal.aceitando" : "conviteDeAnimal.aceitar" })}
        </button>

        <button
          type="button"
          disabled={ocupado}
          onClick={() => recusar.mutate(token, { onSuccess: () => setDesfecho("RECUSADO") })}
          style={{ ...botao, color: "oklch(0.42 0.015 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.84 0.012 150)" }}
        >
          {intl.formatMessage({ id: recusar.isPending ? "conviteDeAnimal.recusando" : "conviteDeAnimal.recusar" })}
        </button>

        {/* "Se recusar, Marcelo é avisado e nada muda para o Code." */}
        <div style={{ fontSize: "13px", color: CINZA, lineHeight: 1.55, marginTop: "14px" }}>
          {intl.formatMessage(
            { id: "conviteDeAnimal.seRecusar" },
            { quem: quemConvidou, animal },
          )}
        </div>
      </div>
    </div>
  );
}

function Ponto({ children, tracejado }: { children: React.ReactNode; tracejado?: boolean }) {
  return (
    <div style={{ display: "flex", gap: "11px" }}>
      <span
        aria-hidden
        style={{
          width: "12px",
          height: "12px",
          borderRadius: "999px",
          flex: "none",
          marginTop: "4px",
          ...(tracejado === true
            ? { border: "2px dashed oklch(0.6 0.015 150)" }
            : { background: VERDE }),
        }}
      ></span>
      <span style={tracejado === true ? { color: "oklch(0.45 0.015 150)" } : undefined}>
        {children}
      </span>
    </div>
  );
}

const VERDE = "oklch(0.46 0.085 150)";
const CINZA = "oklch(0.5 0.015 150)";

const cartao = {
  margin: "0 auto",
  background: "oklch(1 0 0)",
  border: "1px solid oklch(0.86 0.008 150)",
  borderRadius: "12px",
  padding: "30px 28px 32px",
} as const;

const titulo = {
  fontFamily: "Bitter, Georgia, serif",
  fontSize: "26px",
  fontWeight: 500,
  margin: "0 0 12px",
  letterSpacing: "-0.02em",
} as const;

const botao = {
  fontFamily: "inherit",
  fontSize: "16px",
  fontWeight: 500,
  borderRadius: "8px",
  padding: "15px",
  minHeight: "52px",
  cursor: "pointer",
  width: "100%",
} as const;

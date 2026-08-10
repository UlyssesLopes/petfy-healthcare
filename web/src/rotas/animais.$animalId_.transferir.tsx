import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAnexos, useAnimal, useCondicoes } from "../dados/carteira.ts";
import {
  useConvidarSucessor,
  useLinhaDoTempo,
  useOrientacoes,
  useTutores,
} from "../dados/animal.ts";
import { useAcessosDeOrganizacao } from "../dados/acessos.ts";
import { lerSessao } from "../dados/sessao.ts";
import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 11 · area do tutor — Transferir a titularidade, nenhuma custodia termina sem
 * sucessor", de `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * O markup vem do arquivo, e o caminho na API e o CONVITE COM PAPEL DE TITULAR — nao a rota
 * `transfer-holder`, que passa a titularidade na hora e exige que a pessoa ja alcance o
 * animal. O desenho manda e-mail para alguem de fora e diz "a pessoa precisa aceitar",
 * entao e o convite.
 *
 * <b>O PAINEL DA DIREITA DIZ O CONTRARIO DO DESENHO, E E DE PROPOSITO.</b> Ele desenhou
 * "quem deixa de ver o Code" — co-tutora e organizacoes perdendo acesso no aceite —, com a
 * tese escrita embaixo: "acessos nao sao herdados. Paula recebe o Code sem a plateia que
 * ela nao escolheu". <b>O backend nao faz isso.</b> Conferido no `PetTutorServiceImpl`, nos
 * dois caminhos de transferencia:
 *
 * <ul>
 *   <li>as concessoes dos outros co-tutores NAO sao revogadas — eles continuam alcancando;</li>
 *   <li>as concessoes de organizacao NAO sao revogadas — a clinica e a creche continuam;</li>
 *   <li>o titular anterior ganha uma concessao `EDITOR`, que e ESCRITA sobre o animal
 *       inteiro — mais do que a "leitura do periodo" que o desenho promete a ele.</li>
 * </ul>
 *
 * Entao a tela mostra a consequencia REAL, com o mesmo peso visual de alerta que o desenho
 * deu: quem vai continuar vendo, e que quem recebe pode revogar cada um. Mentir na direcao
 * do desenho seria pior que divergir dele — a pessoa que recebe o animal decidiria sobre uma
 * plateia que ela acha que nao existe. <b>Fazer o backend obedecer ao desenho e decisao de
 * produto, nao de tela</b>, e esta registrada como tal.
 */

export const Route = createFileRoute("/animais/$animalId_/transferir")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Transferir,
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

function Transferir() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const linha = useLinhaDoTempo(animalId);
  const orientacoes = useOrientacoes(animalId);
  const condicoes = useCondicoes(animalId);
  const anexos = useAnexos(animalId);
  const tutores = useTutores(animalId);
  const organizacoes = useAcessosDeOrganizacao(animalId);
  const convidar = useConvidarSucessor();
  const enviar = useHover();

  const [email, setEmail] = useState("");
  const nome = animal.data?.name ?? "";

  const enviado = convidar.data;

  /* O que vai junto: nao e lista fixa, e o que ESTE animal tem. */
  const desde = [...(linha.data ?? [])]
    .map((entrada) => entrada.occurredAt)
    .filter((quando): quando is string => quando !== undefined)
    .sort()[0];

  const emCurso = (orientacoes.data ?? []).filter((orientacao) => orientacao.vigente !== false);
  const quantasCondicoes = (condicoes.data ?? []).length;
  const quantosAnexos = (anexos.data ?? []).length;

  const vaiJunto: string[] = [];

  if ((linha.data ?? []).length > 0 && desde !== undefined) {
    vaiJunto.push(
      intl.formatMessage(
        { id: "transferir.junto.linha" },
        { ano: new Date(desde).getFullYear() },
      ),
    );
  } else {
    vaiJunto.push(intl.formatMessage({ id: "transferir.junto.linhaVazia" }));
  }

  for (const orientacao of emCurso) {
    vaiJunto.push(
      orientacao.lastFulfilledAt === undefined
        ? intl.formatMessage(
            { id: "transferir.junto.orientacao" },
            { o_que: orientacao.description ?? "" },
          )
        : intl.formatMessage(
            { id: "transferir.junto.orientacaoComDose" },
            {
              o_que: orientacao.description ?? "",
              data: intl.formatDate(new Date(orientacao.lastFulfilledAt), { dateStyle: "short" }),
            },
          ),
    );
  }

  if (quantasCondicoes > 0 || quantosAnexos > 0) {
    vaiJunto.push(
      intl.formatMessage(
        { id: "transferir.junto.condicoes" },
        { condicoes: quantasCondicoes, anexos: quantosAnexos },
      ),
    );
  }

  /* Quem continua alcancando: a consequencia real, e nao a desenhada. */
  const coTutores = (tutores.data ?? []).filter((tutor) => tutor.holder !== true);
  const organizacoesVigentes = (organizacoes.data ?? []).filter(
    (acesso) => acesso.active !== false,
  );

  if (enviado !== undefined) {
    return (
      <Moldura>
        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
          {intl.formatMessage({ id: "transferir.enviado.titulo" }, { email: enviado.email ?? email })}
        </h1>
        <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 12px", maxWidth: "64ch" }}>
          {intl.formatMessage({ id: "transferir.enviado.apoio" }, { nome })}
        </p>
        {enviado.expiresAt !== undefined && (
          <p style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", margin: "0 0 28px" }}>
            {intl.formatMessage(
              { id: "transferir.enviado.prazo" },
              { data: intl.formatDate(new Date(enviado.expiresAt), { dateStyle: "long" }) },
            )}
          </p>
        )}
        <Link to="/animais/$animalId" params={{ animalId }} style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
          {intl.formatMessage({ id: "transferir.enviado.voltar" }, { nome })}
        </Link>
      </Moldura>
    );
  }

  return (
    <Moldura>
      <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
        {intl.formatMessage({ id: "transferir.titulo" }, { nome })}
      </h1>
      <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "64ch" }}>
        {intl.formatMessage({ id: "transferir.apoio" }, { nome })}
      </p>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
            <label htmlFor="transferir-email" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
              {intl.formatMessage({ id: "transferir.paraQuem" })}
            </label>
            <input
              id="transferir-email"
              type="email"
              value={email}
              onChange={(evento) => setEmail(evento.target.value)}
              autoComplete="email"
              style={{ fontFamily: "inherit", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", width: "100%", background: "oklch(1 0 0)" }}
            />
            <div style={{ fontSize: "14px", color: "oklch(0.5 0.015 150)", marginTop: "8px", lineHeight: 1.5 }}>
              {intl.formatMessage({ id: "transferir.paraQuem.apoio" }, { nome })}
            </div>

            {/*
             * O MOTIVO FICA DESABILITADO, com o motivo ao lado. O desenho pede o campo, e o
             * `PetTutorInviteRequestDTO` tem tres coisas: e-mail, papel e validade. Nao ha
             * onde guardar o texto — e um campo que aceitasse digitacao e jogasse fora
             * seria pior que um campo que explica por que ainda nao da.
             */}
            <label htmlFor="transferir-motivo" style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", margin: "22px 0 7px" }}>
              {intl.formatMessage({ id: "transferir.motivo" })}
            </label>
            <input
              id="transferir-motivo"
              type="text"
              disabled
              value=""
              style={{ fontFamily: "inherit", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", width: "100%", background: "oklch(0.975 0.004 150)", opacity: 0.7 }}
            />
            <div style={{ fontSize: "14px", color: "oklch(0.42 0.015 150)", marginTop: "8px", lineHeight: 1.5 }}>
              {intl.formatMessage({ id: "transferir.motivo.indisponivel" })}
            </div>
          </div>

          <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
            <Rotulo>{intl.formatMessage({ id: "transferir.junto.rotulo" }, { nome })}</Rotulo>
            <div style={{ display: "flex", flexDirection: "column", gap: "11px", fontSize: "16px" }}>
              {vaiJunto.map((frase) => (
                <div key={frase} style={{ display: "flex", alignItems: "center", gap: "11px" }}>
                  <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none" }}></span>
                  {frase}
                </div>
              ))}
            </div>
          </div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          <div style={{ border: "1px solid oklch(0.86 0.03 30)", background: "oklch(0.985 0.008 30)", borderRadius: "12px", padding: "24px 26px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "14px" }}>
              <div aria-hidden style={{ width: "13px", height: "13px", background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)" }}></div>
              <span style={{ fontSize: "13px", fontWeight: 500, letterSpacing: "0.04em", textTransform: "uppercase", color: "oklch(0.45 0.13 30)" }}>
                {intl.formatMessage({ id: "transferir.continua.rotulo" }, { nome })}
              </span>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "14px", fontSize: "16px" }}>
              <Consequencia
                quem={intl.formatMessage({ id: "transferir.continua.voce" })}
                oQue={intl.formatMessage({ id: "transferir.continua.voce.texto" }, { nome })}
              />

              {coTutores.map((tutor) => (
                <Consequencia
                  key={tutor.vinculoId ?? tutor.personId}
                  quem={intl.formatMessage(
                    { id: "transferir.continua.coTutor" },
                    { quem: tutor.personName ?? "" },
                  )}
                  oQue={intl.formatMessage({ id: "transferir.continua.coTutor.texto" })}
                />
              ))}

              {organizacoesVigentes.map((acesso) => (
                <Consequencia
                  key={acesso.grantId}
                  quem={acesso.organizationName ?? ""}
                  oQue={intl.formatMessage({ id: "transferir.continua.organizacao.texto" })}
                />
              ))}
            </div>
          </div>

          <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "transferir.continua.tese" })}
          </div>

          {convidar.isError && (
            <p role="alert" style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.45 0.13 30)", background: "oklch(0.97 0.012 30)", borderRadius: "8px", padding: "12px 14px", margin: 0 }}>
              {intl.formatMessage({ id: chaveDoErro(convidar.error) })}
            </p>
          )}

          <button
            type="button"
            disabled={convidar.isPending || email.trim() === ""}
            {...enviar.props}
            onClick={() => convidar.mutate({ animalId, email: email.trim() })}
            style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: convidar.isPending || email.trim() === "" ? "oklch(0.62 0.05 150)" : enviar.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: email.trim() === "" ? "not-allowed" : "pointer" }}
          >
            {intl.formatMessage(
              { id: convidar.isPending ? "transferir.acao.enviando" : "transferir.acao" },
              { email: email.trim() },
            )}
          </button>

          <Link
            to="/animais/$animalId"
            params={{ animalId }}
            style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: "pointer", textAlign: "center", textDecoration: "none" }}
          >
            {intl.formatMessage({ id: "transferir.acao.cancelar" })}
          </Link>
        </div>
      </div>
    </Moldura>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Moldura({ children }: { children: ReactNode }) {
  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", padding: "40px 48px 44px" }}>
        {children}
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

function Consequencia({ quem, oQue }: { quem: string; oQue: string }) {
  return (
    <div>
      <div style={{ fontWeight: 500 }}>{quem}</div>
      <div style={{ fontSize: "14px", color: "oklch(0.42 0.015 150)", marginTop: "2px", lineHeight: 1.5 }}>
        {oQue}
      </div>
    </div>
  );
}

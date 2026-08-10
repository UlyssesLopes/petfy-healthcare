import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar, ErroDeCarga } from "../componentes/Estados.tsx";
import { useConvidarSucessor, useLinhaDoTempo, useOrientacoes } from "../dados/animal.ts";
import { useAnimal, useCondicoes } from "../dados/carteira.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 13 · area de organizacao — A adocao, o registro inteiro muda de mao", de
 * `design/IdentidadeVisual/Telas Petfy - Abrigo e estados.dc.html`.
 *
 * <b>A frase final do desenho e a tese do produto inteiro:</b> "e o momento em que o produto
 * prova a tese. O adotante nao ganha uma ficha em branco com a data de hoje: ganha onze anos de
 * vida de um animal que ele acabou de conhecer."
 *
 * <b>O QUE FOI PRECISO CONSTRUIR NO BACKEND PARA ESTA TELA EXISTIR.</b> O dominio da adocao
 * estava pronto e inalcancavel: o `Custody` aceita `holderOrganization` desde o P2 e o
 * `CustodyEndReason` traz `ADOCAO` com a documentacao do caso — mas o `requireCustodia` da
 * guarda so sabia perguntar por PESSOA. Nenhum membro do abrigo conseguia agir sobre o animal
 * do proprio abrigo. Agora a custodia da organizacao DECLARADA no cabecalho conta, o aceite
 * grava `ADOCAO` em vez de `TRANSFERENCIA`, e o abrigo fica com `VIEWER` — "passa a ler o que
 * registrou, e nao decide mais nada".
 *
 * <b>O RESUMO SAI DE DADOS REAIS, e por isso ele e menor que o do desenho.</b> Ele lista
 * resgate, castracao, "41 atos clinicos por 6 veterinarios em 11 anos", "2 anos e 4 meses em
 * lares transitorios" e "uma devolucao, em 2019". Do que existe: a contagem de atendimentos, de
 * quem os assinou e a faixa de anos saem da linha do tempo; condicoes e orientacoes saem das
 * proprias rotas. <b>O que nao existe:</b> `RESGATE` nao e tipo de evento, lar transitorio nao
 * e modelado, e a historia de custodias nao tem rota — entao a devolucao de 2019, que o desenho
 * faz questao de mostrar ("e biografia, e o adotante tem direito de saber"), <b>nao tem de onde
 * sair</b>. Fica dito na tela, e nao inventado.
 */

export const Route = createFileRoute("/animais/$animalId_/adocao")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Adocao,
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

function Adocao() {
  const { animalId } = Route.useParams();
  const intl = useIntl();

  const animal = useAnimal(animalId);
  const linha = useLinhaDoTempo(animalId);
  const condicoes = useCondicoes(animalId);
  const orientacoes = useOrientacoes(animalId);
  const contexto = useMeuContexto();
  const convidar = useConvidarSucessor();
  const acao = useHover();

  const [email, setEmail] = useState("");

  const nome = animal.data?.name ?? "";
  const abrigo = contexto.data?.active?.organizationName ?? "";

  /* O que o adotante recebe, contado a partir da linha do tempo. */
  const entradas = linha.data ?? [];
  const atendimentos = entradas.filter((entrada) => entrada.eventType === "ATENDIMENTO");
  const assinaturas = new Set(
    atendimentos
      .map((entrada) => entrada.recordedByName)
      .filter((quem): quem is string => quem !== undefined),
  );
  const anos = entradas
    .map((entrada) => entrada.occurredAt)
    .filter((quando): quando is string => quando !== undefined)
    .map((quando) => new Date(quando).getFullYear())
    .sort();
  const desde = anos[0];
  const emCurso = (orientacoes.data ?? []).filter((o) => o.vigente !== false);

  if (convidar.isSuccess) {
    return (
      <Moldura>
        <Titulo>{intl.formatMessage({ id: "adocao.enviado.titulo" }, { email: convidar.data?.email ?? email })}</Titulo>
        <Apoio>{intl.formatMessage({ id: "adocao.enviado.apoio" }, { nome, abrigo })}</Apoio>
        <Link to="/pacientes" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
          {intl.formatMessage({ id: "adocao.enviado.voltar" })}
        </Link>
      </Moldura>
    );
  }

  return (
    <Moldura>
      <Titulo>{intl.formatMessage({ id: "adocao.titulo" }, { nome })}</Titulo>
      <Apoio>{intl.formatMessage({ id: "adocao.apoio" }, { abrigo })}</Apoio>

      {animal.isError ? (
        <ErroDeCarga
          oQue={intl.formatMessage({ id: "adocao.oQue" })}
          erro={animal.error}
          aoTentarDeNovo={() => void animal.refetch()}
          carregando={animal.isFetching}
        />
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px" }}>
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* ------------------------------------------- o que o adotante recebe */}
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
              <Rotulo>{intl.formatMessage({ id: "adocao.recebe" })}</Rotulo>

              <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "16px" }}>
                {desde !== undefined && (
                  <Item>{intl.formatMessage({ id: "adocao.recebe.linha" }, { desde })}</Item>
                )}

                {atendimentos.length > 0 && (
                  <Item>
                    {intl.formatMessage(
                      { id: "adocao.recebe.atendimentos" },
                      { quantos: atendimentos.length, pessoas: assinaturas.size },
                    )}
                  </Item>
                )}

                {(condicoes.data ?? []).length > 0 && (
                  <Item>
                    {intl.formatMessage(
                      { id: "adocao.recebe.condicoes" },
                      { quantas: (condicoes.data ?? []).length },
                    )}
                  </Item>
                )}

                {emCurso.length > 0 && (
                  <Item>
                    {intl.formatMessage({ id: "adocao.recebe.emCurso" }, { quantas: emCurso.length })}
                  </Item>
                )}

                {entradas.length === 0 && (
                  <Nota>{intl.formatMessage({ id: "adocao.recebe.vazio" }, { nome })}</Nota>
                )}
              </div>

              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "16px", borderTop: "1px solid oklch(0.95 0.005 150)", paddingTop: "14px" }}>
                {intl.formatMessage({ id: "adocao.recebe.faltaResgate" })}
              </div>
            </div>

            {/* -------------------------------------------------- quem vai adotar */}
            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(1 0 0)", padding: "24px 26px" }}>
              <Rotulo>{intl.formatMessage({ id: "adocao.adotante" })}</Rotulo>

              <input
                type="email"
                value={email}
                onChange={(evento) => setEmail(evento.target.value)}
                placeholder={intl.formatMessage({ id: "adocao.adotante.campo" })}
                aria-label={intl.formatMessage({ id: "adocao.adotante.campo" })}
                style={{ fontFamily: "inherit", width: "100%", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "4px", padding: "13px 14px", fontSize: "16px", minHeight: "48px", background: "oklch(1 0 0)" }}
              />

              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.5 0.015 150)", marginTop: "8px" }}>
                {intl.formatMessage({ id: "adocao.adotante.semConta" }, { nome })}
              </div>
            </div>
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            {/* O que muda para o abrigo, dito antes do gesto. */}
            <div style={{ border: "1px solid oklch(0.86 0.03 70)", background: "oklch(0.985 0.012 70)", borderRadius: "12px", padding: "24px 26px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
                <div aria-hidden style={{ width: "13px", height: "13px", background: "oklch(0.62 0.11 70)", transform: "rotate(45deg)" }}></div>
                <span style={{ fontSize: "13px", fontWeight: 500, letterSpacing: "0.04em", textTransform: "uppercase", color: "oklch(0.45 0.09 70)" }}>
                  {intl.formatMessage({ id: "adocao.mudaParaOAbrigo" })}
                </span>
              </div>

              <div style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
                {intl.formatMessage({ id: "adocao.passaALer" }, { abrigo, nome })}
              </div>
            </div>

            <div style={{ border: "1px solid oklch(0.90 0.008 150)", borderRadius: "12px", background: "oklch(0.975 0.004 150)", padding: "22px 24px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
              {intl.formatMessage({ id: "adocao.tese" })}
            </div>

            {convidar.isError && (
              <ErroAoGravar erro={convidar.error} oQue={intl.formatMessage({ id: "adocao.oQue.enviar" })} />
            )}

            <button
              type="button"
              disabled={convidar.isPending || email.trim() === ""}
              {...acao.props}
              onClick={() => convidar.mutate({ animalId, email: email.trim() })}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: convidar.isPending || email.trim() === "" ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: email.trim() === "" ? "not-allowed" : "pointer" }}
            >
              {intl.formatMessage({ id: convidar.isPending ? "adocao.acao.enviando" : "adocao.acao" })}
            </button>

            <Link
              to="/pacientes"
              style={{ fontFamily: "inherit", fontSize: "16px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "15px", minHeight: "52px", cursor: "pointer", textAlign: "center", textDecoration: "none" }}
            >
              {intl.formatMessage({ id: "adocao.cancelar" })}
            </Link>
          </div>
        </div>
      )}
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

function Titulo({ children }: { children: ReactNode }) {
  return (
    <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "30px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
      {children}
    </h1>
  );
}

function Apoio({ children }: { children: ReactNode }) {
  return (
    <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "64ch" }}>
      {children}
    </p>
  );
}

function Rotulo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "12px", letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
      {children}
    </div>
  );
}

function Item({ children }: { children: ReactNode }) {
  return (
    <div style={{ display: "flex", alignItems: "flex-start", gap: "11px" }}>
      <span aria-hidden style={{ width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none", marginTop: "6px" }}></span>
      <span>{children}</span>
    </div>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>{children}</div>;
}

import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 08 · depois do login", de `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * <b>A tese esta na ultima linha do desenho, e ela e uma decisao de produto inteira:</b>
 * "nenhum passo pergunta se voce e tutor ou profissional. Os comodos ficam abertos, e a area
 * que voce alcanca vem do que voce tem — um animal sob sua custodia, ou um vinculo com uma
 * organizacao". Por isso os cartoes aparecem para todo mundo, e nenhum esta atras de um papel
 * declarado.
 *
 * ------------------------------------------------ O DESENHO TEM TRES COMODOS. A TELA TEM DOIS.
 *
 * <b>"Convidar quem mais cuida" saiu em 2026-08-15</b>, e o motivo e que ele nao podia
 * funcionar nunca: convite e sempre PARA um animal (`/animals/{animalId}/tutors/invites`), e
 * <b>esta tela so e alcancada por quem acabou de criar a conta</b> — quem, por definicao, nao
 * tem animal nenhum. O cartao nao estava vazio "por enquanto": estava vazio para sempre.
 *
 * O desenho nao errou; ele desenhou uma pessoa que ja tem animal numa tela que so aparece
 * antes de existir um. <b>E o convite nao se perdeu</b> — ele vive no passo 4 do stepper
 * (`/animais/novo`), que e onde ha um animal para convidar PARA, e funciona de ponta a ponta.
 *
 * <b>A regra que decidiu isso:</b> o que nao abre nao fica na tela. E a mesma do rodape, que
 * perdeu dez itens pelo mesmo criterio, e vale para toda tela que ainda falta.
 *
 * ------------------------------------------------------------ e o que ficou, ficou porque abre
 *
 * O terceiro comodo dizia que declarar registro depois e aceitar convite de organizacao "ainda
 * nao tem caminho" — e os dois tinham: a `/conta` grava CRMV, UF e especialidade, e a
 * `/convites` recebe o codigo, pergunta ao servidor de que tipo ele e e leva ao aceite certo.
 * As duas telas nasceram depois daquele texto, e ninguem voltou aqui para apaga-lo. Era texto
 * velho, nao limitacao.
 */

export const Route = createFileRoute("/comecar")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: Comecar,
});

function Comecar() {
  const intl = useIntl();
  const contexto = useMeuContexto();

  const nome = contexto.data?.personName ?? "";

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1100px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "14px 28px", borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <div aria-hidden style={{ width: "26px", height: "26px", borderRadius: "999px", border: "2.5px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <div style={{ width: "8px", height: "8px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
            </div>
            <span style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "17px", fontWeight: 600 }}>Petfy</span>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", padding: "8px 14px", minHeight: "44px", background: "oklch(0.975 0.004 150)" }}>
            <span style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "comecar.agindoComo" })}
            </span>
            <span style={{ fontSize: "15px", fontWeight: 500 }}>{nome}</span>
          </div>
        </div>

        <div style={{ padding: "48px 56px 56px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "34px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "comecar.titulo" }, { nome: nome.split(" ")[0] ?? "" })}
          </h1>
          <p style={{ fontSize: "17px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 36px", maxWidth: "60ch" }}>
            {intl.formatMessage({ id: "comecar.apoio" })}
          </p>

          {/* Duas colunas, e nao tres: o comodo do convite saiu. Ver o cabecalho do arquivo. */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "20px" }}>
            <Comodo
              destacado
              marca={
                <div aria-hidden style={{ width: "44px", height: "44px", borderRadius: "999px", border: "3px solid oklch(0.46 0.085 150)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <div style={{ width: "13px", height: "13px", borderRadius: "999px", background: "oklch(0.46 0.085 150)" }}></div>
                </div>
              }
              titulo={intl.formatMessage({ id: "comecar.animal.titulo" })}
              texto={intl.formatMessage({ id: "comecar.animal.texto" })}
              acao={
                <Botao para="/animais/novo" principal>
                  {intl.formatMessage({ id: "comecar.animal.acao" })}
                </Botao>
              }
            />

            <Comodo
              marca={<div aria-hidden style={{ width: "44px", height: "44px", borderRadius: "4px", border: "2px solid oklch(0.72 0.012 150)" }}></div>}
              titulo={intl.formatMessage({ id: "comecar.profissional.titulo" })}
              texto={intl.formatMessage({ id: "comecar.profissional.texto" })}
              acao={
                /*
                 * As TRES saidas, e o desenho pedia a primeira delas desde sempre: o botao da
                 * Tela 08 se chama "Declarar registro".
                 *
                 * <b>Ele nao existia porque o cartao afirmava que nao havia caminho</b> — e
                 * havia: a `/conta` grava CRMV, UF e especialidade, e a `/convites` recebe o
                 * codigo e roteia sozinha entre convite de animal e de organizacao. O texto
                 * ficou de uma epoca em que as duas telas nao existiam e ninguem voltou aqui.
                 * Era texto velho, o primeiro dos tres tipos de "ainda nao".
                 */
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  <Botao para="/conta">
                    {intl.formatMessage({ id: "comecar.profissional.registro" })}
                  </Botao>
                  <Botao para="/convites">
                    {intl.formatMessage({ id: "comecar.profissional.convite" })}
                  </Botao>
                  {/* A Tela 15 passou a existir: criar organizacao e a terceira saida. */}
                  <Botao para="/organizacoes/nova">
                    {intl.formatMessage({ id: "comecar.profissional.acao" })}
                  </Botao>
                </div>
              }
            />
          </div>

          <div style={{ marginTop: "24px", fontSize: "15px", color: "oklch(0.5 0.015 150)", lineHeight: 1.6 }}>
            {intl.formatMessage({ id: "comecar.tese" })}
          </div>

          {/*
           * A SAIDA, e ela e botao — nao link de menu.
           *
           * O desenho e explicito: "o stepper mantem a marca e o 'fazer isso depois', sem
           * navegacao: sair no meio e botao, nao link de menu". Sem ela esta tela contradizia o
           * proprio texto, que promete que "nada disso bloqueia o resto" e nao oferecia caminho
           * nenhum para o resto — os comodos so levam para frente.
           */}
          <div style={{ marginTop: "32px", borderTop: "1px solid oklch(0.90 0.008 150)", paddingTop: "24px" }}>
            <Link
              to="/"
              style={{ display: "inline-flex", alignItems: "center", justifyContent: "center", textDecoration: "none", fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "14px 22px", minHeight: "48px" }}
            >
              {intl.formatMessage({ id: "comecar.depois" })}
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------------------ pedacos */

function Comodo({
  marca,
  titulo,
  texto,
  acao,
  destacado = false,
}: {
  marca: ReactNode;
  titulo: string;
  texto: string;
  acao: ReactNode;
  destacado?: boolean;
}) {
  return (
    <div style={{ border: `1px solid ${destacado ? "oklch(0.46 0.085 150)" : "oklch(0.90 0.008 150)"}`, background: "oklch(1 0 0)", borderRadius: "12px", padding: "28px 26px", display: "flex", flexDirection: "column" }}>
      <div style={{ marginBottom: "20px" }}>{marca}</div>
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "21px", fontWeight: 500, marginBottom: "10px" }}>
        {titulo}
      </div>
      <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)", flex: 1, marginBottom: "22px" }}>
        {texto}
      </div>
      {acao}
    </div>
  );
}

/**
 * Os quatro destinos da tela, e <b>nenhum deles pede parametro</b>.
 *
 * Era o `/animais/$animalId/quem-cuida` que obrigava este componente a ter dois ramos e um
 * cast em cada um. Ele saiu com o comodo do convite, e o que sobrou e um `Link` so.
 */
type Destino = "/animais/novo" | "/conta" | "/convites" | "/organizacoes/nova";

function Botao({
  para,
  principal = false,
  children,
}: {
  para: Destino;
  principal?: boolean;
  children: ReactNode;
}) {
  return (
    <Link
      to={para}
      style={{
        fontFamily: "inherit",
        fontSize: "15px",
        fontWeight: 500,
        borderRadius: "8px",
        padding: "13px",
        minHeight: "48px",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        textDecoration: "none",
        ...(principal
          ? { color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none" }
          : { color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)" }),
      }}
    >
      {children}
    </Link>
  );
}

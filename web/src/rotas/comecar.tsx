import { createFileRoute, Link, redirect } from "@tanstack/react-router";
import { type ReactNode } from "react";
import { useIntl } from "react-intl";

import { useAnimais } from "../dados/animais.ts";
import { useMeuContexto } from "../dados/contexto.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 08 · depois do login — Onboarding, tres comodos e nenhum corredor", de
 * `design/IdentidadeVisual/Telas Petfy - Entrada e fluxos.dc.html`.
 *
 * <b>A tese esta na ultima linha do desenho, e ela e uma decisao de produto inteira:</b>
 * "nenhum passo pergunta se voce e tutor ou profissional. Os tres comodos ficam abertos, e a
 * area que voce alcanca vem do que voce tem — um animal sob sua custodia, ou um vinculo com
 * uma organizacao". Por isso os tres cartoes aparecem para todo mundo, e nenhum deles esta
 * atras de um papel declarado.
 *
 * <b>O terceiro comodo nao tem para onde levar hoje.</b> "Atender animais de outras pessoas"
 * pede declarar registro profissional ou aceitar convite de organizacao: o registro tem
 * campo (`crmv` no `PersonRequestDTO`, e ele esta na Tela 07), mas nao existe tela de conta
 * para declarar depois, e aceitar convite de organizacao e a Tela 16, que ainda nao existe.
 * O cartao explica isso em vez de levar a lugar nenhum.
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
  const animais = useAnimais();

  const primeiro = (animais.data ?? [])[0];
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

          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "20px" }}>
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
              marca={
                <div style={{ display: "flex" }}>
                  <div aria-hidden style={{ width: "30px", height: "30px", borderRadius: "999px", background: "oklch(0.90 0.03 150)" }}></div>
                  <div aria-hidden style={{ width: "30px", height: "30px", borderRadius: "999px", background: "oklch(0.85 0.03 150)", marginLeft: "-10px", border: "2px solid oklch(1 0 0)" }}></div>
                  <div aria-hidden style={{ width: "30px", height: "30px", borderRadius: "999px", background: "oklch(0.80 0.03 150)", marginLeft: "-10px", border: "2px solid oklch(1 0 0)" }}></div>
                </div>
              }
              titulo={intl.formatMessage({ id: "comecar.rede.titulo" })}
              texto={intl.formatMessage({ id: "comecar.rede.texto" })}
              acao={
                /*
                 * Convidar exige um animal: o convite e sempre PARA um animal
                 * (`/animals/{animalId}/tutors/invites`). Sem nenhum cadastrado, o cartao
                 * diz isso em vez de abrir uma tela que perguntaria "para qual?".
                 */
                primeiro?.animalId === undefined ? (
                  <Motivo>{intl.formatMessage({ id: "comecar.rede.semAnimal" })}</Motivo>
                ) : (
                  <Botao
                    para="/animais/$animalId/quem-cuida"
                    parametros={{ animalId: primeiro.animalId }}
                  >
                    {intl.formatMessage({ id: "comecar.rede.acao" })}
                  </Botao>
                )
              }
            />

            <Comodo
              marca={<div aria-hidden style={{ width: "44px", height: "44px", borderRadius: "4px", border: "2px solid oklch(0.72 0.012 150)" }}></div>}
              titulo={intl.formatMessage({ id: "comecar.profissional.titulo" })}
              texto={intl.formatMessage({ id: "comecar.profissional.texto" })}
              acao={<Motivo>{intl.formatMessage({ id: "comecar.profissional.indisponivel" })}</Motivo>}
            />
          </div>

          <div style={{ marginTop: "24px", fontSize: "15px", color: "oklch(0.5 0.015 150)", lineHeight: 1.6 }}>
            {intl.formatMessage({ id: "comecar.tese" })}
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

function Botao({
  para,
  parametros,
  principal = false,
  children,
}: {
  para: "/animais/novo" | "/animais/$animalId/quem-cuida";
  parametros?: { animalId: string };
  principal?: boolean;
  children: ReactNode;
}) {
  const estilo = {
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
  } as const;

  if (parametros === undefined) {
    return (
      <Link to={para as "/animais/novo"} style={estilo}>
        {children}
      </Link>
    );
  }

  return (
    <Link to={para as "/animais/$animalId/quem-cuida"} params={parametros} style={estilo}>
      {children}
    </Link>
  );
}

/** O lugar do botao quando nao ha para onde ir, com a razao escrita (secao 06). */
function Motivo({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", border: "1px dashed oklch(0.88 0.008 150)", borderRadius: "8px", padding: "13px 14px" }}>
      {children}
    </div>
  );
}

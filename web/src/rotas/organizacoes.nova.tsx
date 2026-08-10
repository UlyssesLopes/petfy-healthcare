import { createFileRoute, Link, redirect, useNavigate } from "@tanstack/react-router";
import { useState, type ReactNode } from "react";
import { useIntl } from "react-intl";

import { ErroAoGravar } from "../componentes/Estados.tsx";
import { useMeuContexto } from "../dados/contexto.ts";
import { useCriarOrganizacao } from "../dados/organizacoes.ts";
import { lerSessao } from "../dados/sessao.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 15 · cadastro de organizacao — Criar a creche", de
 * `design/IdentidadeVisual/Telas Petfy - Organizacao.dc.html`.
 *
 * <b>A frase de abertura e a definicao do conceito, e nao marketing:</b> "uma organizacao
 * existe para que o trabalho da equipe fique assinado em nome dela. Voce continua sendo Vera
 * Quintal em tudo que registrar — a creche nao assina por ninguem". Responsabilidade tem nome
 * (secao 10 da identidade), e organizacao nao e um nome atras do qual alguem se esconde.
 *
 * <b>"O QUE VOCES FAZEM" NAO ENTRA, E E A MAIOR PERDA DESTA TELA.</b> O desenho pede cinco
 * opcoes marcaveis — creche e hospedagem, clinica veterinaria, banho e tosa, abrigo ou
 * resgate, adestramento — e diz para que elas servem: "isso define o que a equipe consegue
 * registrar, e o que faz sentido pedir aos tutores". O `OrganizationCapability` existe no
 * modelo (seis valores, e o `/me/context` os devolve), mas o `OrganizationRequestDTO` nao tem
 * campo nenhum para eles. Nao ha como declarar na criacao, e nao ha rota para declarar depois.
 *
 * Marcar caixas que nao viajam seria o pior dos mundos: a equipe sairia daqui achando que
 * declarou o que faz. Entao as opcoes aparecem, desabilitadas, com o motivo — e o que o
 * desenho promete que elas fazem fica escrito como o que ainda nao acontece.
 */

export const Route = createFileRoute("/organizacoes/nova")({
  beforeLoad: () => {
    if (!lerSessao().autenticada) {
      throw redirect({ to: "/entrar" });
    }
  },
  component: NovaOrganizacao,
});

/** As cinco do desenho, na ordem dele. */
const OQUE_FAZEM = ["creche", "clinica", "banhoETosa", "abrigo", "adestramento"] as const;

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

function NovaOrganizacao() {
  const intl = useIntl();
  const navegar = useNavigate();
  const contexto = useMeuContexto();
  const criar = useCriarOrganizacao();
  const acao = useHover();

  const [nome, setNome] = useState("");
  const [cnpj, setCnpj] = useState("");
  const [telefone, setTelefone] = useState("");
  const [endereco, setEndereco] = useState("");
  const [cidade, setCidade] = useState("");
  const [estado, setEstado] = useState("");

  const quem = contexto.data?.personName ?? "";

  const enviar = async () => {
    const criada = await criar.mutateAsync({
      nome: nome.trim(),
      cnpj: cnpj.trim() === "" ? undefined : cnpj.trim(),
      telefone: telefone.trim() === "" ? undefined : telefone.trim(),
      endereco: endereco.trim() === "" ? undefined : endereco.trim(),
      cidade: cidade.trim() === "" ? undefined : cidade.trim(),
      estado: estado.trim() === "" ? undefined : estado.trim().toUpperCase(),
    });

    if (criada.organizationId !== undefined) {
      await navegar({
        to: "/organizacoes/$organizationId/equipe",
        params: { organizationId: criada.organizationId },
      });
      return;
    }

    await navegar({ to: "/" });
  };

  return (
    <div style={{ padding: "40px 24px" }}>
      <div style={{ maxWidth: "1000px", margin: "0 auto", background: "oklch(0.985 0.004 120)", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "12px", overflow: "hidden" }}>
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
            <span style={{ fontSize: "15px", fontWeight: 500 }}>{quem}</span>
          </div>
        </div>

        <div style={{ padding: "40px 56px 44px" }}>
          <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "32px", fontWeight: 500, margin: "0 0 10px", letterSpacing: "-0.02em" }}>
            {intl.formatMessage({ id: "organizacao.nova.titulo" })}
          </h1>
          <p style={{ fontSize: "16px", lineHeight: 1.6, color: "oklch(0.45 0.015 150)", margin: "0 0 32px", maxWidth: "60ch" }}>
            {intl.formatMessage({ id: "organizacao.nova.apoio" }, { quem })}
          </p>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px 24px", maxWidth: "680px" }}>
            <div style={{ gridColumn: "1 / -1" }}>
              <Rotulo para="org-nome">{intl.formatMessage({ id: "organizacao.nova.nome" })}</Rotulo>
              <input id="org-nome" type="text" value={nome} onChange={(e) => setNome(e.target.value)} style={estiloDoCampo} />
            </div>

            {/* As cinco caixas do desenho, desabilitadas com o motivo. */}
            <div style={{ gridColumn: "1 / -1", border: "1px dashed oklch(0.88 0.008 150)", borderRadius: "12px", padding: "20px 22px" }}>
              <div style={{ fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "12px" }}>
                {intl.formatMessage({ id: "organizacao.nova.oQueFazem" })}
              </div>

              <div style={{ display: "flex", flexWrap: "wrap", gap: "10px", opacity: 0.55 }}>
                {OQUE_FAZEM.map((oQue) => (
                  <div
                    key={oQue}
                    style={{ display: "flex", alignItems: "center", gap: "10px", border: "1px solid oklch(0.86 0.008 150)", borderRadius: "8px", padding: "10px 14px", minHeight: "44px", fontSize: "15px", background: "oklch(1 0 0)" }}
                  >
                    <div aria-hidden style={{ width: "18px", height: "18px", borderRadius: "4px", border: "1px solid oklch(0.78 0.012 150)", flex: "none" }}></div>
                    {intl.formatMessage({ id: `organizacao.nova.oQueFazem.${oQue}` })}
                  </div>
                ))}
              </div>

              <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginTop: "14px" }}>
                {intl.formatMessage({ id: "organizacao.nova.oQueFazem.indisponivel" })}
              </div>
            </div>

            <div>
              <Rotulo para="org-cnpj">
                {intl.formatMessage({ id: "organizacao.nova.cnpj" })}
                <span style={{ fontWeight: 400, color: "oklch(0.55 0.015 150)" }}>
                  {" "}
                  {intl.formatMessage({ id: "onboarding.opcional" })}
                </span>
              </Rotulo>
              <input id="org-cnpj" type="text" value={cnpj} onChange={(e) => setCnpj(e.target.value)} style={{ ...estiloDoCampo, fontFamily: "'DM Mono', monospace" }} />
            </div>

            <div>
              <Rotulo para="org-telefone">{intl.formatMessage({ id: "organizacao.nova.telefone" })}</Rotulo>
              <input id="org-telefone" type="tel" value={telefone} onChange={(e) => setTelefone(e.target.value)} style={estiloDoCampo} />
              <Nota>{intl.formatMessage({ id: "organizacao.nova.telefone.apoio" })}</Nota>
            </div>

            <div style={{ gridColumn: "1 / -1" }}>
              <Rotulo para="org-endereco">{intl.formatMessage({ id: "organizacao.nova.endereco" })}</Rotulo>
              <input id="org-endereco" type="text" value={endereco} onChange={(e) => setEndereco(e.target.value)} style={estiloDoCampo} />
            </div>

            <div>
              <Rotulo para="org-cidade">{intl.formatMessage({ id: "organizacao.nova.cidade" })}</Rotulo>
              <input id="org-cidade" type="text" value={cidade} onChange={(e) => setCidade(e.target.value)} style={estiloDoCampo} />
            </div>

            <div>
              <Rotulo para="org-estado">{intl.formatMessage({ id: "organizacao.nova.estado" })}</Rotulo>
              <input id="org-estado" type="text" maxLength={2} value={estado} onChange={(e) => setEstado(e.target.value.slice(0, 2))} style={{ ...estiloDoCampo, textTransform: "uppercase", fontFamily: "'DM Mono', monospace" }} />
            </div>
          </div>

          {/* A frase que precisa vir ANTES do botao, e nao depois. */}
          <div style={{ border: "1px solid oklch(0.90 0.008 150)", background: "oklch(0.975 0.004 150)", borderRadius: "12px", padding: "20px 22px", margin: "28px 0 0", maxWidth: "680px", fontSize: "15px", lineHeight: 1.6, color: "oklch(0.42 0.015 150)" }}>
            {intl.formatMessage({ id: "organizacao.nova.comecaVazia" }, { nome: nome.trim() })}
          </div>

          {criar.isError && (
            <div style={{ marginTop: "20px", maxWidth: "680px" }}>
              <ErroAoGravar erro={criar.error} oQue={intl.formatMessage({ id: "organizacao.nova.oQue" })} />
            </div>
          )}

          <div style={{ display: "flex", alignItems: "center", gap: "16px", marginTop: "28px", flexWrap: "wrap" }}>
            <button
              type="button"
              disabled={criar.isPending || nome.trim() === ""}
              {...acao.props}
              onClick={() => void enviar()}
              style={{ fontFamily: "inherit", fontSize: "16px", fontWeight: 500, color: "oklch(1 0 0)", background: criar.isPending || nome.trim() === "" ? "oklch(0.62 0.05 150)" : acao.sobre ? "oklch(0.40 0.09 150)" : "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "15px 28px", minHeight: "52px", cursor: nome.trim() === "" ? "not-allowed" : "pointer" }}
            >
              {intl.formatMessage(
                { id: criar.isPending ? "organizacao.nova.criando" : "organizacao.nova.acao" },
                { nome: nome.trim() },
              )}
            </button>
            <span style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)" }}>
              {intl.formatMessage({ id: "organizacao.nova.responsavel" })}
            </span>
          </div>

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

const estiloDoCampo = {
  fontFamily: "inherit",
  border: "1px solid oklch(0.82 0.012 150)",
  borderRadius: "4px",
  padding: "13px 14px",
  fontSize: "16px",
  minHeight: "48px",
  width: "100%",
  background: "oklch(1 0 0)",
} as const;

function Rotulo({ children, para }: { children: ReactNode; para: string }) {
  return (
    <label htmlFor={para} style={{ display: "block", fontSize: "13px", fontWeight: 500, color: "oklch(0.42 0.015 150)", marginBottom: "7px" }}>
      {children}
    </label>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return (
    <div style={{ fontSize: "13px", color: "oklch(0.5 0.015 150)", marginTop: "7px", lineHeight: 1.5 }}>
      {children}
    </div>
  );
}

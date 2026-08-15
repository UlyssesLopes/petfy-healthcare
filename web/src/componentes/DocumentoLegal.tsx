import { Link } from "@tanstack/react-router";
import { useIntl } from "react-intl";

import { VERSAO_VIGENTE, type Documento, type Secao } from "../conteudo/documentos.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A pagina de um documento legal — termos de uso e politica de privacidade.
 *
 * <b>Por que ela existe:</b> o cadastro sempre obrigou a aceitar os dois ("sem aceite nao ha base
 * legal para tratar dado de saude"), o rodape sempre os nomeou, e nenhum dos dois tinha para onde
 * levar. O produto pedia que a pessoa aceitasse um documento que ela nao tinha como ler.
 *
 * <b>O QUE FALTA APARECE, e nao se esconde.</b> Secao sem texto vem marcada com o que falta
 * decidir, em vez de sumir da lista: quem le precisa saber que o assunto existe e ainda nao tem
 * resposta. Um documento que parece completo e nao esta e pior que um que se declara incompleto.
 */

export function DocumentoLegal({ documento }: { documento: Documento }) {
  const intl = useIntl();

  const pendentes = documento.secoes.filter((secao) => secao.pendente !== undefined).length;

  return (
    <main style={{ padding: "40px 24px 64px" }}>
      <div style={{ maxWidth: "760px", margin: "0 auto" }}>
        <Link to="/entrar" style={{ fontSize: "15px", color: "oklch(0.46 0.085 150)" }}>
          {intl.formatMessage({ id: "documento.voltar" })}
        </Link>

        <h1 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "34px", fontWeight: 500, letterSpacing: "-0.02em", margin: "24px 0 12px" }}>
          {documento.titulo}
        </h1>
        <p style={{ fontSize: "17px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", margin: "0 0 8px" }}>
          {documento.resumo}
        </p>
        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: "13px", color: "oklch(0.5 0.015 150)" }}>
          {intl.formatMessage({ id: "documento.versao" }, { versao: VERSAO_VIGENTE })}
        </div>

        {/*
         * O AVISO VEM ANTES DO TEXTO, e nao no rodape.
         *
         * Quem abre isto pode estar decidindo se cria conta. Descobrir no fim que faltam pedacos
         * seria descobrir depois de ter lido como se estivesse completo.
         */}
        {pendentes > 0 && (
          <div style={{ marginTop: "28px", border: "1px solid oklch(0.80 0.09 75)", background: "oklch(0.97 0.03 90)", borderRadius: "8px", padding: "16px 18px" }}>
            <div style={{ fontSize: "15px", fontWeight: 500, marginBottom: "6px" }}>
              {intl.formatMessage({ id: "documento.emElaboracao.titulo" })}
            </div>
            <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)" }}>
              {intl.formatMessage({ id: "documento.emElaboracao.texto" }, { quantas: pendentes })}
            </div>
          </div>
        )}

        <div style={{ marginTop: "40px", display: "flex", flexDirection: "column", gap: "36px" }}>
          {documento.secoes.map((secao) => (
            <Bloco key={secao.titulo} secao={secao} />
          ))}
        </div>
      </div>
    </main>
  );
}

function Bloco({ secao }: { secao: Secao }) {
  const intl = useIntl();

  return (
    <section>
      <h2 style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "22px", fontWeight: 500, margin: "0 0 14px", letterSpacing: "-0.01em" }}>
        {secao.titulo}
      </h2>

      {secao.paragrafos.map((paragrafo) => (
        <p key={paragrafo.slice(0, 40)} style={{ fontSize: "16px", lineHeight: 1.7, color: "oklch(0.3 0.018 150)", margin: "0 0 14px" }}>
          {paragrafo}
        </p>
      ))}

      {secao.pendente !== undefined && (
        <div style={{ borderLeft: "3px solid oklch(0.80 0.09 75)", paddingLeft: "14px", margin: "4px 0 0" }}>
          <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.52 0.07 75)", marginBottom: "6px" }}>
            {intl.formatMessage({ id: "documento.falta" })}
          </div>
          <p style={{ fontSize: "15px", lineHeight: 1.65, color: "oklch(0.42 0.015 150)", margin: 0 }}>
            {secao.pendente}
          </p>
        </div>
      )}
    </section>
  );
}

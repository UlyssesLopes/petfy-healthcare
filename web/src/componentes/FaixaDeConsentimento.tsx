import { useState } from "react";
import { useIntl } from "react-intl";

import { useAceitarConsentimento, useConsentimento } from "../dados/consentimento.ts";
import { lerSessao } from "../dados/sessao.ts";

/**
 * "Atualizamos os termos" — o terceiro painel da Tela 07, que nao e daquela tela.
 *
 * <b>Faixa, e nao bloqueio.</b> A nota do desenho ao lado do painel diz por que: "sem alarme
 * e sem tom de erro — o texto mudou, nao a conta". Entao ela nao e modal, nao tranca nada, e
 * "agora nao" e uma saida de verdade: some ate a proxima visita.
 *
 * <b>O "ver o que mudou" do desenho nao existe.</b> O `/consents/me` devolve documento e
 * versao pendente, e nenhuma rota devolve o texto — muito menos o diff entre duas versoes.
 * Um link para uma pagina que nao existe seria pior que a ausencia dele. Fica registrado.
 */
export function FaixaDeConsentimento() {
  const intl = useIntl();
  const consentimento = useConsentimento();
  const aceitar = useAceitarConsentimento();
  const [dispensada, setDispensada] = useState(false);

  // Sem sessao nao ha o que consentir, e a rota responderia 401 na cara de quem so quer
  // criar a conta.
  if (!lerSessao().autenticada || dispensada) {
    return null;
  }

  const pendentes = consentimento.data?.pendentes ?? [];

  if (consentimento.data?.tudoAceito === true || pendentes.length === 0) {
    return null;
  }

  return (
    <div style={{ borderBottom: "1px solid oklch(0.90 0.008 150)", background: "oklch(1 0 0)" }}>
      <div style={{ maxWidth: "1360px", margin: "0 auto", padding: "16px 24px", display: "flex", gap: "18px", alignItems: "center", flexWrap: "wrap" }}>
        <div style={{ flex: 1, minWidth: "280px" }}>
          <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "4px" }}>
            {intl.formatMessage({ id: "consentimento.titulo" })}
          </div>
          <div style={{ fontSize: "15px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)" }}>
            {intl.formatMessage(
              { id: "consentimento.texto" },
              {
                documentos: new Intl.ListFormat("pt-BR", { style: "long", type: "conjunction" }).format(
                  pendentes.map((pendente) =>
                    intl.formatMessage({
                      id: `consentimento.documento.${pendente.document ?? "OUTRO"}`,
                    }),
                  ),
                ),
              },
            )}
          </div>
        </div>

        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="button"
            disabled={aceitar.isPending}
            onClick={() => aceitar.mutate()}
            style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(1 0 0)", background: "oklch(0.46 0.085 150)", border: "none", borderRadius: "8px", padding: "12px 18px", minHeight: "44px", cursor: "pointer" }}
          >
            {intl.formatMessage({ id: aceitar.isPending ? "consentimento.aceitando" : "consentimento.aceitar" })}
          </button>
          <button
            type="button"
            onClick={() => setDispensada(true)}
            style={{ fontFamily: "inherit", fontSize: "15px", color: "oklch(0.42 0.015 150)", background: "transparent", border: "1px solid oklch(0.84 0.012 150)", borderRadius: "8px", padding: "12px 18px", minHeight: "44px", cursor: "pointer" }}
          >
            {intl.formatMessage({ id: "consentimento.agoraNao" })}
          </button>
        </div>
      </div>
    </div>
  );
}

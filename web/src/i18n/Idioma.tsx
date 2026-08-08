import type { ReactNode } from "react";
import { IntlProvider } from "react-intl";

import { mensagens } from "./mensagens/pt-BR.ts";

/**
 * O idioma da aplicacao.
 *
 * <b>i18n desde a primeira tela</b> (ROADMAP.md, Fase 5) — nao porque exista um segundo
 * idioma hoje, mas porque as consequencias sao caras depois: frase montada por
 * concatenacao nao sobrevive a traducao nenhuma, e largura fixa em rotulo quebra quando
 * o texto cresce 40% (DESIGN.md 4 e 6). Provider desde o comeco e o que torna essas duas
 * regras verificaveis em vez de intencao.
 *
 * <b>Um idioma so, por ora, e sem carregamento sob demanda.</b> Quando o segundo entrar,
 * o que muda e este arquivo: as telas ja falam por chave.
 */
const IDIOMA_PADRAO = "pt-BR";

export function Idioma({ children }: { children: ReactNode }) {
  return (
    <IntlProvider locale={IDIOMA_PADRAO} defaultLocale={IDIOMA_PADRAO} messages={mensagens}>
      {children}
    </IntlProvider>
  );
}

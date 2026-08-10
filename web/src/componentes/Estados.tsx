import { type ReactNode } from "react";
import { useIntl } from "react-intl";

import { chaveDoErro } from "../i18n/erroDaApi.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * A "Tela 14 · em toda superficie — Os estados, desenhados e nao descritos", de
 * `design/IdentidadeVisual/Telas Petfy - Abrigo e estados.dc.html`.
 *
 * <b>Ela nao e uma rota, e por isso mora aqui.</b> O desenho dela e um catalogo de seis
 * estados que valem em toda tela, e o unico jeito de "construir" isso e ter um lugar so onde
 * cada um vive — senao cada tela inventa o seu, que e exatamente o que ela existe para
 * impedir.
 *
 * <b>OS DOIS ESTADOS QUE ESTE ARQUIVO NAO TEM, e o motivo de cada um:</b>
 *
 * <ul>
 *   <li><b>Conflito ("Ja foi feito").</b> O desenho mostra duas pessoas registrando a mesma
 *       dose com tres minutos de diferenca, e o produto recusando a segunda: "dois registros
 *       da mesma dose viram dose dobrada no historico". <b>O servidor nao detecta isso</b> —
 *       nao ha codigo de conflito para dose repetida no `error-codes.json`, e as duas
 *       gravacoes passam. Escrever a tela do conflito seria desenhar a recusa de algo que
 *       ninguem recusa.</li>
 *   <li><b>Cartao expirado.</b> Depende da tela de cartao compartilhado (`/share/{token}`),
 *       que ainda nao existe.</li>
 * </ul>
 */

/**
 * "Nao conseguimos carregar X. O problema e nosso. Nada do registro foi perdido."
 *
 * <b>As tres partes sao obrigatorias juntas</b>, e nao decoracao: o que falhou, de quem e a
 * culpa, e o que aconteceu com o dado. A terceira e a que importa para quem esta com o animal
 * doente na frente — falha de carga nao apaga historico, e a tela precisa dizer isso.
 *
 * E ha `Tentar de novo`, porque a saida do erro nao pode ser recarregar a pagina inteira.
 */
export function ErroDeCarga({
  oQue,
  erro,
  aoTentarDeNovo,
  carregando = false,
}: {
  oQue: string;
  erro: unknown;
  aoTentarDeNovo?: () => void;
  carregando?: boolean;
}) {
  const intl = useIntl();

  return (
    <div
      role="alert"
      style={{ border: "1px solid oklch(0.86 0.03 30)", background: "oklch(0.985 0.008 30)", borderRadius: "12px", padding: "22px 24px" }}
    >
      <div style={{ fontFamily: "Bitter, Georgia, serif", fontSize: "18px", fontWeight: 500, marginBottom: "8px" }}>
        {intl.formatMessage({ id: "estado.erroDeCarga.titulo" }, { o_que: oQue })}
      </div>

      <div style={{ fontSize: "15px", lineHeight: 1.6, color: "oklch(0.35 0.018 150)", marginBottom: "6px" }}>
        {intl.formatMessage({ id: "estado.erroDeCarga.nossa" })}
      </div>

      {/*
       * A mensagem tecnica vem DEPOIS da frase que acalma, e nao no lugar dela. O `chaveDoErro`
       * traduz por codigo do backend; quando nao ha codigo, cai no texto generico.
       */}
      <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)", marginBottom: aoTentarDeNovo === undefined ? 0 : "16px" }}>
        {intl.formatMessage({ id: chaveDoErro(erro) })}
      </div>

      {aoTentarDeNovo !== undefined && (
        <button
          type="button"
          disabled={carregando}
          onClick={aoTentarDeNovo}
          style={{ fontFamily: "inherit", fontSize: "15px", fontWeight: 500, color: "oklch(0.25 0.02 150)", background: "oklch(1 0 0)", border: "1px solid oklch(0.82 0.012 150)", borderRadius: "8px", padding: "12px 18px", minHeight: "44px", cursor: carregando ? "wait" : "pointer" }}
        >
          {intl.formatMessage({ id: carregando ? "estado.tentando" : "estado.tentarDeNovo" })}
        </button>
      )}
    </div>
  );
}

/**
 * "Carregando 318 pacientes" — a lista longa diz QUANTOS, quando sabe.
 *
 * O desenho e explicito ao poem o numero: esperar sem saber o tamanho da espera e o que faz
 * uma lista de trezentos parecer travada. Sem numero, cai na frase seca.
 */
export function Carregando({ oQue, quantos }: { oQue: string; quantos?: number }) {
  const intl = useIntl();

  return (
    <div style={{ fontSize: "15px", color: "oklch(0.5 0.015 150)", padding: "4px 0" }}>
      {quantos === undefined
        ? intl.formatMessage({ id: "estado.carregando" }, { o_que: oQue })
        : intl.formatMessage({ id: "estado.carregando.quantos" }, { quantos, o_que: oQue })}
    </div>
  );
}

/**
 * "Nao conseguimos gravar. <b>O que voce escreveu esta aqui, intacto.</b>"
 *
 * A segunda frase e a razao deste componente existir. O texto continua no estado do React —
 * ninguem perde o que digitou —, mas o desenho cobra que a tela DIGA isso: quem acabou de
 * escrever seis linhas sobre o animal doente nao tem como saber que elas sobreviveram.
 */
export function ErroAoGravar({ erro, oQue }: { erro: unknown; oQue: string }) {
  const intl = useIntl();

  return (
    <div
      role="alert"
      style={{ border: "1px solid oklch(0.86 0.03 30)", background: "oklch(0.985 0.008 30)", borderRadius: "8px", padding: "14px 16px" }}
    >
      <div style={{ fontSize: "15px", lineHeight: 1.55, color: "oklch(0.35 0.018 150)", marginBottom: "4px" }}>
        {intl.formatMessage({ id: "estado.erroAoGravar" }, { o_que: oQue })}
      </div>
      <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)" }}>
        {intl.formatMessage({ id: chaveDoErro(erro) })}
      </div>
    </div>
  );
}

/**
 * "So quem tem registro profissional declarado pode registrar diagnostico. <b>Voce pode
 * registrar uma observacao do que viu.</b>"
 *
 * O estado de sem-permissao do desenho tem duas partes, e a segunda e obrigatoria: dizer o
 * que a pessoa NAO pode sem oferecer o que ela pode e so barrar. E vem antes do gesto — a
 * frase esta ao lado do controle desabilitado, nao depois do clique.
 */
export function SemPermissao({ oQue, saida }: { oQue: string; saida?: ReactNode }) {
  return (
    <div style={{ border: "1px dashed oklch(0.88 0.008 150)", borderRadius: "8px", padding: "13px 14px" }}>
      <div style={{ fontSize: "14px", lineHeight: 1.55, color: "oklch(0.42 0.015 150)" }}>{oQue}</div>
      {saida !== undefined && <div style={{ marginTop: "10px" }}>{saida}</div>}
    </div>
  );
}

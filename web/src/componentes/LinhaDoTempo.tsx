import { useIntl, type IntlShape } from "react-intl";

import { ehAtoClinico, iniciaisDe, useLinhaDoTempo, type EntradaDaLinha } from "../dados/animal.ts";
import { dataLocalDe } from "../i18n/datas.ts";

/**
 * A linha do tempo (DESIGN.md 5.1 e 5.2) — o objeto central do produto.
 *
 * Uma coluna, ordenada por <b>quando aconteceu</b>, com uma <b>espinha continua</b>
 * ligando os registros. A espinha nao quebra, e e isso que torna a promessa 5.1 visivel:
 * ela atravessa o dia em que o animal mudou de mao. A transferencia e um marco na linha,
 * nunca um corte.
 *
 * <b>A ordem vem do servidor</b>, que a le de uma view ordenada por quando aconteceu — e
 * nao por quando foi digitado. A tela nao reordena e nao agrupa por tipo: tipo e filtro,
 * nao estrutura (5.1).
 */
export function LinhaDoTempo({ animalId, animalNome }: { animalId: string; animalNome: string }) {
  const intl = useIntl();
  const linha = useLinhaDoTempo(animalId);

  const entradas = linha.data ?? [];

  return (
    <section className="mt-6">
      <div className="flex flex-wrap items-baseline gap-3">
        <h2 className="text-rotulo uppercase text-tinta-secundaria">
          {intl.formatMessage({ id: "linha.titulo" }, { animal: animalNome })}
        </h2>

        {/* Os numeros do resumo moram no cabecalho da secao: nao e painel, e legenda (5.1). */}
        {entradas.length > 0 ? (
          <span className="text-apoio ml-auto tabular-nums text-tinta-secundaria">
            {intl.formatMessage({ id: "linha.resumo" }, { registros: entradas.length })}
          </span>
        ) : null}
      </div>

      {linha.isPending ? (
        <p className="text-apoio mt-2.5 text-tinta-secundaria">
          {intl.formatMessage({ id: "linha.carregando" })}
        </p>
      ) : entradas.length === 0 ? (
        <div className="mt-2.5 rounded-bloco border border-contorno p-5">
          <p className="text-registro text-tinta">
            {intl.formatMessage({ id: "linha.vazia" }, { animal: animalNome })}
          </p>
          <p className="text-apoio mt-1 text-tinta-secundaria">
            {intl.formatMessage({ id: "linha.vazia.apoio" })}
          </p>
        </div>
      ) : (
        /*
          A espinha e um pseudo-elemento em 28 px — o centro do circulo de 32 px que comeca
          apos 12 px de recuo. Ela e continua de proposito, e cada circulo a "fura" com um
          anel da cor do fundo do proprio item.
        */
        <ul className="relative mt-2.5 flex flex-col gap-0.5 before:absolute before:bottom-4 before:left-7 before:top-4 before:w-0.5 before:rounded-sm before:bg-contorno before:content-['']">
          {entradas.map((entrada) => (
            <Entrada key={entrada.eventId} entrada={entrada} />
          ))}
        </ul>
      )}
    </section>
  );
}

function Entrada({ entrada }: { entrada: EntradaDaLinha }) {
  const intl = useIntl();

  const clinico = ehAtoClinico(entrada);
  const autor = entrada.recordedByName ?? "";

  return (
    <li
      className={`relative grid grid-cols-[32px_1fr] gap-3 rounded-linha px-3 py-2.5 ${
        clinico ? "bg-fundo-acento" : ""
      }`}
    >
      {/*
        A marca do evento e a PESSOA (5.2): as iniciais de quem registrou ocupam o lugar
        que num produto comum teria um icone de tipo. E a promessa 5.7 virando calor em
        vez de burocracia.
      */}
      <span
        aria-hidden="true"
        className={`z-[1] grid size-8 place-items-center rounded-circulo text-[0.71875rem] font-extrabold ${
          clinico
            ? "bg-acento text-sobre-acento shadow-[0_0_0_3px_var(--color-fundo-acento)]"
            : "bg-marca-neutra text-tinta-secundaria shadow-[0_0_0_3px_var(--color-superficie)]"
        }`}
      >
        {iniciaisDe(autor)}
      </span>

      <div className="min-w-0">
        <Fato entrada={entrada} />

        <p className="text-apoio mt-0.5 text-tinta-secundaria">{apoioDe(entrada, intl)}</p>

        {/*
          A credencial diz o que e: CRMV apenas informado aparece COMO INFORMADO, em tinta
          secundaria — sem selo de "verificado" que o produto nao pode dar (5.10).
        */}
        {entrada.credentialLabel !== undefined && entrada.credentialStatus !== undefined ? (
          <p className="text-rotulo normal-case text-tinta-secundaria">
            {intl.formatMessage(
              { id: `linha.credencial.${entrada.credentialStatus}` },
              { credencial: entrada.credentialLabel },
            )}
          </p>
        ) : null}
      </div>
    </li>
  );
}

/**
 * O fato.
 *
 * A pesagem e o unico tipo que carrega numero em vez de frase: a view declara que o
 * `summary` da PESAGEM <b>e o peso</b>, e manda o anterior ao lado para o cliente montar a
 * variacao. <b>A variacao cabe na linha; a curva nao</b> — "12,4 kg" sozinho nao diz se e
 * boa ou ma noticia, e uma curva de anos responde outra pergunta, numa tela propria
 * (5.2, emenda de 2026-08-07).
 */
function Fato({ entrada }: { entrada: EntradaDaLinha }) {
  const intl = useIntl();

  if (entrada.eventType === "ORIENTACAO" || entrada.eventType === "CUMPRIMENTO") {
    return (
      <p className="text-registro text-tinta">
        {intl.formatMessage(
          {
            id:
              entrada.eventType === "CUMPRIMENTO"
                ? "linha.orientacao.cumprida"
                : "linha.orientacao.emitida",
          },
          { "o que": entrada.summary ?? "" },
        )}
      </p>
    );
  }

  if (entrada.eventType !== "PESAGEM") {
    return <p className="text-registro text-tinta">{entrada.summary ?? ""}</p>;
  }

  const peso = Number(entrada.summary);
  const anterior = entrada.previousWeight;

  const pesoEmTexto = Number.isFinite(peso)
    ? `${intl.formatNumber(peso, { maximumFractionDigits: 2 })} kg`
    : (entrada.summary ?? "");

  const diferenca = Number.isFinite(peso) && anterior !== undefined ? peso - anterior : undefined;

  return (
    <p className="text-registro flex flex-wrap items-baseline gap-x-2.5 text-tinta">
      <span className="tabular-nums">{pesoEmTexto}</span>

      {diferenca !== undefined && diferenca !== 0 ? (
        <span className="text-rotulo normal-case tabular-nums text-tinta-secundaria">
          {intl.formatMessage(
            { id: "linha.peso.variacao" },
            {
              sinal: diferenca > 0 ? "+" : "−",
              diferenca: intl.formatNumber(Math.abs(diferenca), { maximumFractionDigits: 2 }),
            },
          )}
        </span>
      ) : null}
    </p>
  );
}

/**
 * Quem afirmou o fato, onde, e quando — a segunda linha que nunca e opcional, nunca e
 * tooltip e nunca e "ver detalhes" (5.2, 5.7).
 *
 * <b>Os dois instantes so aparecem quando divergem.</b> Se foi registrado no mesmo dia em
 * que aconteceu, nao se diz nada: ruido nao e transparencia.
 */
function apoioDe(entrada: EntradaDaLinha, intl: IntlShape): string {
  const partes: string[] = [];

  if (entrada.recordedByName !== undefined) {
    partes.push(entrada.recordedByName);
  }
  if (entrada.organizationName !== undefined) {
    partes.push(entrada.organizationName);
  }
  if (entrada.occurredAt !== undefined) {
    partes.push(porExtenso(entrada.occurredAt, intl));
  }

  if (
    entrada.recordedAt !== undefined
    && entrada.occurredAt !== undefined
    && entrada.recordedAt.slice(0, 10) !== entrada.occurredAt.slice(0, 10)
  ) {
    partes.push(
      intl.formatMessage(
        { id: "linha.registradoEm" },
        { data: porExtenso(entrada.recordedAt, intl) },
      ),
    );
  }

  // Correcao e sucessao, e se ve (5.2): nada de aba "historico", nada de "(editado)".
  if (entrada.correctionCount !== undefined && entrada.correctionCount > 0) {
    partes.push(
      intl.formatMessage({ id: "linha.correcoes" }, { quantas: entrada.correctionCount }),
    );
  }

  return partes.join(" · ");
}

/** So o dia: o instante viaja sem fuso, e a hora seria precisao inventada. */
function porExtenso(instante: string, intl: IntlShape): string {
  return intl.formatDate(dataLocalDe(instante.slice(0, 10)), {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

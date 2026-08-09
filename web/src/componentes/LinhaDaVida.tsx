import { useState } from "react";
import { useIntl } from "react-intl";

import type { EntradaDaLinha } from "../dados/animal.ts";

export type Recorte = "tudo" | "vacinas" | "atendimentos" | "observacoes";

const TIPOS_DO_RECORTE: Record<Recorte, string[] | undefined> = {
  tudo: undefined,
  vacinas: ["VACINA", "ANTIPARASITARIO"],
  atendimentos: ["ATENDIMENTO"],
  observacoes: ["OBSERVACAO"],
};

/**
 * A cor do ponto diz a natureza do evento, e a palavra ao lado diz o resto.
 *
 * Ato clinico e musgo, observacao e ocre — porque observacao "cobra" no vocabulario da
 * secao 05, sem interromper —, e o resto e neutro. Nenhum evento e telha: telha e vencido,
 * e um evento REGISTRADO nunca esta vencido. Ele aconteceu.
 */
function corDoPonto(tipo: string | undefined): string {
  if (tipo === "VACINA" || tipo === "ATENDIMENTO" || tipo === "ANTIPARASITARIO") {
    return "bg-musgo";
  }
  if (tipo === "OBSERVACAO") {
    return "bg-ocre";
  }
  return "bg-marcador-neutro";
}

export function LinhaDaVida({ entradas }: { entradas: EntradaDaLinha[] }) {
  const intl = useIntl();
  const [recorte, setRecorte] = useState<Recorte>("tudo");

  const tipos = TIPOS_DO_RECORTE[recorte];
  const visiveis =
    tipos === undefined
      ? entradas
      : entradas.filter((entrada) => tipos.includes(entrada.eventType ?? ""));

  const anos = entradas
    .map((entrada) => entrada.occurredAt?.slice(0, 4))
    .filter((ano): ano is string => ano !== undefined)
    .sort();

  const faixa = anos.length === 0 ? undefined : { de: anos[0]!, ate: anos[anos.length - 1]! };

  return (
    <div className="px-10 pt-7 pb-12">
      <div className="mb-5.5 flex items-center justify-between gap-4">
        <h2 className="text-secao font-nome">
          {intl.formatMessage({ id: "animal.linha.titulo" })}
        </h2>

        <div className="flex flex-wrap gap-2">
          {(["tudo", "vacinas", "atendimentos", "observacoes"] as const).map((opcao) => (
            <button
              key={opcao}
              type="button"
              aria-pressed={recorte === opcao}
              onClick={() => setRecorte(opcao)}
              className={`text-apoio rounded-controle flex min-h-toque items-center border px-3.5 py-2.25 ${
                recorte === opcao
                  ? "border-musgo text-musgo"
                  : "border-linha-media text-tinta-media"
              }`}
            >
              {intl.formatMessage({ id: `animal.linha.recorte.${opcao}` })}
            </button>
          ))}

          {faixa !== undefined && (
            <div className="text-apoio rounded-controle border-linha-media text-tinta-media flex min-h-toque items-center border px-3.5 py-2.25">
              {faixa.de === faixa.ate
                ? faixa.de
                : intl.formatMessage({ id: "animal.linha.faixa" }, faixa)}
            </div>
          )}
        </div>
      </div>

      {visiveis.length === 0 ? (
        <p className="text-corpo-denso border-linha rounded-bloco border border-dashed p-6 text-tinta-secundaria">
          {intl.formatMessage({ id: `animal.linha.vazio.${recorte}` })}
        </p>
      ) : (
        <ol className="grid grid-cols-[128px_1fr]">
          {visiveis.map((entrada) => (
            <Evento key={entrada.eventId} entrada={entrada} />
          ))}
        </ol>
      )}
    </div>
  );
}

/**
 * A unidade da linha do tempo.
 *
 * <b>A data grande e QUANDO ACONTECEU; a hora miuda e QUANDO FOI LANCADO</b> (secao 06).
 * Um evento com dois anos entre as duas e normal — e a carteirinha de papel de 2019 sendo
 * lancada em 2024 —, e o desenho tem de deixar isso legivel em vez de esconder. Por isso a
 * data ocupa uma coluna propria e o "lancado em" vive no rodape, junto da autoria.
 *
 * <b>Nao ha botao de excluir aqui, e nao havera em lugar nenhum do registro.</b>
 */
function Evento({ entrada }: { entrada: EntradaDaLinha }) {
  const intl = useIntl();

  const quando = entrada.occurredAt === undefined ? undefined : new Date(entrada.occurredAt);

  return (
    <li className="contents">
      <div className="border-linha border-r py-0.5 pr-5 pb-6.5 text-right">
        {quando !== undefined && (
          <>
            <div className="text-corpo-denso font-medium">
              {intl.formatDate(quando, { day: "2-digit", month: "short", year: "numeric" })}
            </div>
            <div className="text-etiqueta font-dado mt-0.75 normal-case text-tinta-fraca">
              {intl.formatDate(quando, { weekday: "long" })}
            </div>
          </>
        )}
      </div>

      <div className="relative pb-6.5 pl-6">
        <div
          aria-hidden
          className={`absolute -left-1.75 top-1.25 size-3.25 rounded-ser border-3 border-papel ${corDoPonto(entrada.eventType)}`}
        />

        <h3 className="text-evento font-nome mb-1.5">{entrada.summary}</h3>

        {entrada.previousWeight !== undefined && (
          <p className="text-corpo-denso mb-2.5 text-tinta-forte">
            {intl.formatMessage(
              { id: "animal.linha.pesoAnterior" },
              { peso: intl.formatNumber(entrada.previousWeight, { maximumFractionDigits: 1 }) },
            )}
          </p>
        )}

        <p className="text-rotulo text-tinta-secundaria">
          {autoriaDe(entrada, intl)}
          {entrada.correctionCount !== undefined && entrada.correctionCount > 0 && (
            <>
              {" · "}
              {intl.formatMessage(
                { id: "animal.linha.corrigido" },
                { vezes: entrada.correctionCount },
              )}
            </>
          )}
        </p>
      </div>
    </li>
  );
}

function autoriaDe(entrada: EntradaDaLinha, intl: ReturnType<typeof useIntl>): string {
  const quemRegistrou = entrada.recordedByName;
  const organizacao = entrada.organizationName;
  const lancadoEm =
    entrada.recordedAt === undefined
      ? undefined
      : intl.formatDate(new Date(entrada.recordedAt), { dateStyle: "short", timeStyle: "short" });

  if (quemRegistrou === undefined) {
    return lancadoEm === undefined
      ? ""
      : intl.formatMessage({ id: "animal.linha.lancado" }, { quando: lancadoEm });
  }

  /*
   * "Responsabilidade tem nome" (secao 10) — a autoria e fixa no rodape de TODO evento, e
   * a credencial aparece quando existe, com o estado dela. `INFORMADO` nao e `VERIFICADO`,
   * e o produto nao finge garantia que nao tem.
   */
  const credencial =
    entrada.credentialLabel === undefined
      ? ""
      : ` (${entrada.credentialLabel}${
          entrada.credentialStatus === undefined
            ? ""
            : ` · ${intl.formatMessage({ id: `animal.credencial.${entrada.credentialStatus}` })}`
        })`;

  return intl.formatMessage(
    { id: organizacao === undefined ? "animal.linha.autoria" : "animal.linha.autoriaComOrg" },
    {
      quem: quemRegistrou + credencial,
      organizacao: organizacao ?? "",
      quando: lancadoEm ?? "",
    },
  );
}

import { useIntl } from "react-intl";

import type { Anexo, Condicao, LinhaDaCarteira, Pesagem } from "../dados/carteira.ts";
import { MarcadorDeEstado } from "./MarcadorDeEstado.tsx";

/** O rotulo em caixa alta que abre cada bloco da carteira (Tela 02, trilho esquerdo). */
function Rotulo({ children }: { children: string }) {
  return <div className="text-etiqueta mb-3.5 uppercase text-tinta-secundaria">{children}</div>;
}

export function BlocoDeVacinacao({ linhas }: { linhas: LinhaDaCarteira[] }) {
  const intl = useIntl();

  return (
    <div>
      <Rotulo>{intl.formatMessage({ id: "animal.carteira.vacinacao" })}</Rotulo>

      {linhas.length === 0 ? (
        <p className="text-apoio text-tinta-secundaria">
          {intl.formatMessage({ id: "animal.carteira.vacinacao.vazio" })}
        </p>
      ) : (
        <ul className="text-corpo-denso flex flex-col gap-3">
          {linhas.map((linha) => (
            <li key={linha.nome} className="flex items-center gap-2.5">
              <MarcadorDeEstado estado={linha.estado} />
              <span className="flex-1">{linha.nome}</span>
              <span
                className={`text-apoio ${
                  linha.estado === "vencida" ? "text-telha-texto" : "text-tinta-secundaria"
                }`}
              >
                {legendaDe(linha, intl)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function legendaDe(linha: LinhaDaCarteira, intl: ReturnType<typeof useIntl>): string {
  const dias = linha.diasAteProximaDose;

  if (linha.estado === "semRegistro") {
    return intl.formatMessage({ id: "animal.carteira.semRegistro" });
  }

  if (linha.estado === "semProximaDose") {
    return intl.formatMessage({ id: "animal.carteira.semProximaDose" });
  }

  if (linha.estado === "vencida" && dias !== undefined) {
    return intl.formatMessage({ id: "animal.carteira.venceuHa" }, { dias: Math.abs(dias) });
  }

  if (linha.proximaDose !== undefined) {
    return intl.formatMessage(
      { id: "animal.carteira.ate" },
      { data: intl.formatDate(linha.proximaDose, { dateStyle: "short", timeZone: "UTC" }) },
    );
  }

  return "";
}

export function BlocoDeCondicoes({ condicoes }: { condicoes: Condicao[] }) {
  const intl = useIntl();

  const ativas = condicoes.filter((condicao) => condicao.ativa !== false);

  return (
    <div>
      <Rotulo>{intl.formatMessage({ id: "animal.carteira.condicoes" })}</Rotulo>

      {ativas.length === 0 ? (
        <p className="text-apoio text-tinta-secundaria">
          {intl.formatMessage({ id: "animal.carteira.condicoes.vazio" })}
        </p>
      ) : (
        <ul className="text-corpo-denso flex flex-col gap-2">
          {ativas.map((condicao) => (
            <li key={condicao.animalHealthConditionId}>
              {condicao.description}
              {condicao.since !== undefined && (
                <span className="text-apoio text-tinta-secundaria">
                  {" "}
                  {intl.formatMessage(
                    { id: "animal.carteira.desde" },
                    { ano: condicao.since.slice(0, 4) },
                  )}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/**
 * O peso como barras, e nao como numero solto.
 *
 * <b>A ultima barra e musgo e as anteriores sao neutras</b> (Tela 02): a leitura que
 * interessa e "para onde isso esta indo", e uma serie em que todas as barras tem o mesmo
 * peso visual nao conta essa historia. Sao as seis ultimas pesagens porque seis e o que a
 * tela desenha — mais que isso vira grafico, e grafico e outra decisao.
 */
export function BlocoDePeso({ pesagens }: { pesagens: Pesagem[] }) {
  const intl = useIntl();

  const ordenadas = [...pesagens]
    .filter((p): p is Pesagem & { weight: number } => typeof p.weight === "number")
    .sort((a, b) => (a.measuredAt ?? "").localeCompare(b.measuredAt ?? ""));

  const ultimas = ordenadas.slice(-6);
  const maior = Math.max(...ultimas.map((p) => p.weight), 0);
  const atual = ultimas[ultimas.length - 1];

  return (
    <div>
      <Rotulo>{intl.formatMessage({ id: "animal.carteira.peso" })}</Rotulo>

      {atual === undefined ? (
        <p className="text-apoio text-tinta-secundaria">
          {intl.formatMessage({ id: "animal.carteira.peso.vazio" })}
        </p>
      ) : (
        <>
          <div aria-hidden className="mb-2.5 flex h-19 items-end gap-1.5">
            {ultimas.map((pesagem, indice) => (
              <div
                key={pesagem.weightHistoryId ?? indice}
                className={`flex-1 rounded-t-[3px] ${
                  indice === ultimas.length - 1 ? "bg-musgo" : "bg-barra"
                }`}
                style={{ height: `${maior === 0 ? 0 : (pesagem.weight / maior) * 100}%` }}
              />
            ))}
          </div>
          <div className="text-apoio text-tinta-media">
            {intl.formatMessage(
              { id: "animal.carteira.peso.resumo" },
              {
                peso: intl.formatNumber(atual.weight, { maximumFractionDigits: 1 }),
                data:
                  atual.measuredAt === undefined
                    ? ""
                    : intl.formatDate(atual.measuredAt, { dateStyle: "short", timeZone: "UTC" }),
                total: ordenadas.length,
              },
            )}
          </div>
        </>
      )}
    </div>
  );
}

export function BlocoDeAnexos({ anexos }: { anexos: Anexo[] }) {
  const intl = useIntl();

  return (
    <div>
      <Rotulo>{intl.formatMessage({ id: "animal.carteira.anexos" })}</Rotulo>

      {anexos.length === 0 ? (
        <p className="text-apoio text-tinta-secundaria">
          {intl.formatMessage({ id: "animal.carteira.anexos.vazio" })}
        </p>
      ) : (
        <ul className="text-corpo-denso flex flex-col gap-2">
          {anexos.map((anexo) => (
            <li key={anexo.attachmentId}>
              <a
                href={`/attachments/${anexo.attachmentId}/content`}
                className="inline-flex min-h-toque items-center text-musgo underline"
              >
                {anexo.description ?? anexo.originalFilename}
              </a>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

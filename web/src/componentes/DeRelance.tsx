import { useIntl } from "react-intl";

import {
  useAntiparasitarios,
  useCarteira,
  useCondicoes,
  usePesagens,
} from "../dados/carteira.ts";
import { diasAte } from "../i18n/datas.ts";

/**
 * "Code, de relance" — as quatro linhas do trilho direito da Tela 01.
 *
 * <b>Nenhuma delas e um campo do backend.</b> Cada uma e uma leitura sobre dados que ja
 * existem: a vacinacao sai da carteira, o antiparasitario da propria rota dele, o peso da
 * data da ultima pesagem e a alergia das condicoes. Por isso elas vivem aqui e nao num
 * endpoint de resumo — nao ha verdade nova, so a mesma verdade dita curta.
 *
 * <b>O quarto estado aparece de verdade aqui.</b> "Peso sem registro ha 5 meses" e anel
 * tracejado, e nao vermelho: nao pesar nao e irregularidade, e "nao sabemos" nao e "esta
 * ruim". E o caso que a secao 05 chama de mais frequente e pior representado.
 */

type Marca = "vencido" | "emDia" | "semInformacao";

function Marcador({ marca }: { marca: Marca }) {
  const base = { width: "12px", height: "12px", flex: "none" as const };

  if (marca === "vencido") {
    return <div aria-hidden style={{ ...base, background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)" }} />;
  }
  if (marca === "semInformacao") {
    return <div aria-hidden style={{ ...base, borderRadius: "999px", border: "2px dashed oklch(0.55 0.015 150)" }} />;
  }
  return <div aria-hidden style={{ ...base, borderRadius: "999px", background: "oklch(0.46 0.085 150)" }} />;
}

/** Sem pesagem ha mais de quatro meses o registro deixa de saber o peso do animal. */
const MESES_ATE_O_PESO_ENVELHECER = 4;

export function DeRelance({ animalId, nome }: { animalId: string; nome: string }) {
  const intl = useIntl();

  const carteira = useCarteira(animalId);
  const antiparasitarios = useAntiparasitarios(animalId);
  const pesagens = usePesagens(animalId);
  const condicoes = useCondicoes(animalId);

  const linhas: { marca: Marca; texto: string }[] = [];

  // ---- vacinacao
  const temVencida = carteira.linhas.some((linha) => linha.estado === "vencida");
  const temAlguma = carteira.linhas.some((linha) => linha.estado !== "semRegistro");

  linhas.push({
    marca: temVencida ? "vencido" : temAlguma ? "emDia" : "semInformacao",
    texto: intl.formatMessage({
      id: temVencida
        ? "relance.vacinacao.irregular"
        : temAlguma
          ? "relance.vacinacao.emDia"
          : "relance.vacinacao.semRegistro",
    }),
  });

  // ---- antiparasitario
  const doses = antiparasitarios.data ?? [];
  const proxima = doses
    .map((dose) => dose.nextDoseDate)
    .filter((data): data is string => data !== undefined)
    .sort()
    .pop();

  linhas.push(
    doses.length === 0
      ? { marca: "semInformacao", texto: intl.formatMessage({ id: "relance.antiparasitario.semRegistro" }) }
      : proxima !== undefined && diasAte(proxima) < 0
        ? { marca: "vencido", texto: intl.formatMessage({ id: "relance.antiparasitario.vencido" }) }
        : { marca: "emDia", texto: intl.formatMessage({ id: "relance.antiparasitario.emDia" }) },
  );

  // ---- peso
  const ultimaPesagem = (pesagens.data ?? [])
    .map((p) => p.measuredAt)
    .filter((d): d is string => d !== undefined)
    .sort()
    .pop();

  if (ultimaPesagem === undefined) {
    linhas.push({ marca: "semInformacao", texto: intl.formatMessage({ id: "relance.peso.semRegistro" }) });
  } else {
    const meses = Math.floor(-diasAte(ultimaPesagem) / 30);

    linhas.push(
      meses >= MESES_ATE_O_PESO_ENVELHECER
        ? { marca: "semInformacao", texto: intl.formatMessage({ id: "relance.peso.antigo" }, { meses }) }
        : { marca: "emDia", texto: intl.formatMessage({ id: "relance.peso.recente" }) },
    );
  }

  // ---- alergia e condicao
  const ativas = (condicoes.data ?? []).filter((c) => c.ativa !== false);
  const alergias = ativas.filter((c) => c.kind === "ALERGIA");

  if (ativas.length > 0) {
    linhas.push({
      marca: "emDia",
      texto:
        alergias.length > 0
          ? intl.formatMessage(
              { id: "relance.alergia" },
              { o_que: alergias[0]!.description ?? "", extras: alergias.length - 1 },
            )
          : intl.formatMessage({ id: "relance.condicao" }, { total: ativas.length }),
    });
  }

  return (
    <div>
      <div style={{ fontSize: "12px", fontWeight: 500, letterSpacing: "0.05em", textTransform: "uppercase", color: "oklch(0.5 0.015 150)", marginBottom: "14px" }}>
        {intl.formatMessage({ id: "relance.titulo" }, { nome })}
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: "10px", fontSize: "15px" }}>
        {linhas.map((linha) => (
          <div key={linha.texto} style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <Marcador marca={linha.marca} />
            {linha.texto}
          </div>
        ))}
      </div>
    </div>
  );
}

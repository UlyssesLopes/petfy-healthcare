import { useIntl } from "react-intl";

import type { EstadoDaDose, LinhaDaCarteira } from "../dados/carteira.ts";

/* ------------------------------------------------------------------ o que este arquivo e
 *
 * O marcador e a legenda de uma dose, num lugar so.
 *
 * <b>Nasceu de dois chamadores, e nao de antecipacao.</b> A Tela 02 mostra a carteira ao tutor e
 * a Tela 30 a mostra a clinica antes de prescrever — e a segunda copia do mesmo mapeamento e
 * que teria feito uma antirrabica vencida aparecer com losango numa tela e com bolinha na outra.
 *
 * <b>Nenhum dos dois DECIDE o estado.</b> Vencida, vencendo e em dia sao regra de dominio, e vem
 * do `/vaccines/agenda`. Aqui so se escolhe a forma e a frase.
 */

/**
 * Os marcadores da secao 05 da identidade, com os valores exatos da Tela 02.
 *
 * <b>Losango e um quadrado girado — a forma tem de sobreviver a fonte.</b> O anel tracejado e o
 * "sem registro", e ele existe porque <b>nao saber nao e a mesma coisa que nao existir</b>.
 */
export function Marcador({ estado }: { estado: EstadoDaDose }) {
  if (estado === "vencida") {
    return <div style={{ width: "12px", height: "12px", background: "oklch(0.55 0.14 30)", transform: "rotate(45deg)", flex: "none" }}></div>;
  }
  if (estado === "vencendo") {
    return <div style={{ width: "12px", height: "12px", borderRadius: "999px", border: "2px solid oklch(0.62 0.11 70)", flex: "none" }}></div>;
  }
  if (estado === "semRegistro") {
    return <div style={{ width: "12px", height: "12px", borderRadius: "999px", border: "2px dashed oklch(0.55 0.015 150)", flex: "none" }}></div>;
  }
  if (estado === "semProximaDose") {
    return <div style={{ width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.72 0.012 150)", flex: "none" }}></div>;
  }
  return <div style={{ width: "12px", height: "12px", borderRadius: "999px", background: "oklch(0.46 0.085 150)", flex: "none" }}></div>;
}

/**
 * "venceu ha 23 dias", "ate 09/03/2027", "sem registro" — a frase da direita.
 *
 * O {@code timeZone: "UTC"} nao e detalhe: a data vem do servidor como dia puro, e interpreta-la
 * no fuso local faria a dose de 09/03 aparecer como 08/03 para quem esta a oeste de Greenwich.
 */
export function legendaDaDose(dose: LinhaDaCarteira, intl: ReturnType<typeof useIntl>): string {
  if (dose.estado === "semRegistro") return intl.formatMessage({ id: "animal.carteira.semRegistro" });
  if (dose.estado === "semProximaDose") return intl.formatMessage({ id: "animal.carteira.semProximaDose" });

  if (dose.estado === "vencida" && dose.diasAteProximaDose !== undefined) {
    return intl.formatMessage({ id: "animal.carteira.venceuHa" }, { dias: Math.abs(dose.diasAteProximaDose) });
  }

  if (dose.proximaDose !== undefined) {
    return intl.formatMessage(
      { id: "animal.carteira.ate" },
      { data: intl.formatDate(dose.proximaDose, { dateStyle: "short", timeZone: "UTC" }) },
    );
  }

  return "";
}

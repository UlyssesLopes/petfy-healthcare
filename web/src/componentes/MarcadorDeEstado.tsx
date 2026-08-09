import type { EstadoDaDose } from "../dados/carteira.ts";

/**
 * As quatro formas da secao 05, e por que elas sao formas.
 *
 * <b>"Cor nunca carrega significado sozinha"</b> — a regra dura da secao 02. Em dia e
 * ponto cheio, vencendo e anel, vencido e losango, sem informacao e anel tracejado. Em
 * escala de cinza os quatro continuam distintos, e e por isso que eles existem: um
 * daltonico e uma impressao em preto e branco leem a mesma tela.
 *
 * O marcador e <b>sempre</b> acompanhado da palavra ao lado — nunca aparece sozinho.
 * Por isso ele e `aria-hidden`: quem usa leitor de tela ja recebe "Antirrabica, venceu ha
 * 23 dias" pelo texto, e anunciar "losango" seria ruido.
 */
export function MarcadorDeEstado({ estado }: { estado: EstadoDaDose }) {
  const comum = "size-3 flex-none";

  if (estado === "vencida") {
    // Losango: um quadrado girado, e nao um glifo — a forma tem de sobreviver a fonte.
    return <div aria-hidden className={`${comum} rotate-45 bg-telha`} />;
  }

  if (estado === "vencendo") {
    return <div aria-hidden className={`${comum} rounded-ser border-2 border-ocre`} />;
  }

  if (estado === "semRegistro") {
    return (
      <div aria-hidden className={`${comum} rounded-ser border-2 border-dashed border-tinta-fraca`} />
    );
  }

  if (estado === "semProximaDose") {
    return <div aria-hidden className={`${comum} rounded-ser bg-marcador-neutro`} />;
  }

  return <div aria-hidden className={`${comum} rounded-ser bg-musgo`} />;
}

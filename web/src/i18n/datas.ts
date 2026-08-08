/**
 * Data e uma coisa; instante e outra (DESIGN.md 6).
 *
 * A vacina vence num <b>dia</b>; a confirmacao de cumprimento aconteceu num
 * <b>instante</b>. O contrato separa os dois — `format: date` contra `format: date-time`
 * —, e o JavaScript nao: `new Date("2026-08-10")` e lido como <b>meia-noite UTC</b>, que
 * no fuso de Brasilia e 9 de agosto as 21h. Uma vacina que vence dia 10 apareceria
 * vencendo dia 9, e "vence hoje" viraria "vencida ha 1 dia".
 *
 * Por isso a data pura nunca passa pelo construtor com string.
 */

/**
 * Le `AAAA-MM-DD` como o dia que ele nomeia, no fuso de quem esta lendo — sem conversao,
 * porque nao ha o que converter: um dia do calendario nao tem hora.
 */
export function dataLocalDe(iso: string): Date {
  const partes = iso.split("-");

  const ano = Number(partes[0]);
  const mes = Number(partes[1]);
  const dia = Number(partes[2]);

  if (!Number.isFinite(ano) || !Number.isFinite(mes) || !Number.isFinite(dia)) {
    throw new Error(`Data fora do formato AAAA-MM-DD: ${iso}`);
  }

  return new Date(ano, mes - 1, dia);
}

/**
 * Quantos anos inteiros se passaram desde a data — a idade do animal.
 *
 * Conta por aniversario, e nao dividindo dias por 365: quem nasceu em 29 de fevereiro
 * tem idade, e ano bissexto nao pode fazer a idade pular um dia antes da hora.
 */
export function anosDesde(iso: string, hoje: Date = new Date()): number {
  const nascimento = dataLocalDe(iso);

  let anos = hoje.getFullYear() - nascimento.getFullYear();

  const mesesAntes = hoje.getMonth() < nascimento.getMonth();
  const mesmoMesEDiaAntes =
    hoje.getMonth() === nascimento.getMonth() && hoje.getDate() < nascimento.getDate();

  if (mesesAntes || mesmoMesEDiaAntes) {
    anos -= 1;
  }

  return Math.max(anos, 0);
}

/**
 * Quantos dias inteiros faltam para a data, contando por <b>dia do calendario</b> e nao
 * por 24 horas: quem abre a tela as 23h e quem abre as 7h leem o mesmo "vence amanha".
 *
 * Negativo quando ja passou.
 */
export function diasAte(iso: string, hoje: Date = new Date()): number {
  const alvo = dataLocalDe(iso);

  const meiaNoiteDeHoje = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate());

  const UM_DIA_EM_MS = 24 * 60 * 60 * 1000;

  /*
   * Arredonda em vez de truncar: entre as duas meias-noites pode haver 23 ou 25 horas
   * quando o horario de verao comeca ou termina no meio do intervalo, e truncar
   * transformaria isso num dia a menos.
   */
  return Math.round((alvo.getTime() - meiaNoiteDeHoje.getTime()) / UM_DIA_EM_MS);
}

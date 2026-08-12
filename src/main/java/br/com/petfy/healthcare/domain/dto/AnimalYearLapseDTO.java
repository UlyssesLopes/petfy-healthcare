package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Um periodo em que uma vacina esteve vencida (Tela 48).
 *
 * <b>"A antirrabica ficou 23 dias vencida em julho. Foi o unico periodo do ano em que ele esteve
 * irregular."</b> E o campo que faz o resumo ser um documento clinico em vez de uma retrospectiva —
 * e o unico que fala do que deu errado com um numero.
 *
 * <b>O intervalo e calculado, e nao gravado.</b> Ele sai da serie de doses: quando a proxima dose
 * prevista de uma vacina passa e a dose seguinte so chega depois, os dias entre as duas datas sao os
 * dias de irregularidade. Nada no banco registra "esteve vencida" — e nao deveria, porque isso e uma
 * leitura de duas linhas e nao um fato proprio.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalYearLapseDTO {

    private String vaccineName;

    /** O dia em que a dose venceu. */
    private LocalDate overdueSince;

    /**
     * O dia em que a irregularidade acabou. <b>Nulo quando ela nao acabou</b> — e esse nulo e
     * informacao: a vacina esta vencida agora.
     */
    private LocalDate regularizedOn;

    private long days;

}

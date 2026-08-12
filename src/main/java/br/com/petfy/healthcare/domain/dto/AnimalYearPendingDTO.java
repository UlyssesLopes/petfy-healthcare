package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Uma condicao cronica aberta que ninguem tocou no ano (Tela 48).
 *
 * <b>"A displasia dele nao foi reavaliada desde 2023."</b>
 *
 * <b>O criterio e a propria linha da condicao</b>, e nao uma inferencia sobre atendimentos: se
 * ninguem editou aquele registro dentro da janela, ninguem o reavaliou. Tentar deduzir da linha do
 * tempo — "houve consulta, logo foi reavaliada" — afirmaria algo que nenhum registro sustenta, e num
 * documento que vai ao veterinario isso e pior que nao dizer nada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalYearPendingDTO {

    private String description;

    /** Desde quando ela existe, ou quando foi tocada pela ultima vez. */
    private LocalDate lastTouchedOn;

}

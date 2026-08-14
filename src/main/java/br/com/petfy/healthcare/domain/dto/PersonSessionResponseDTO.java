package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aparelho conectado, como a Tela 36 o mostra.
 *
 * <b>Nao ha localizacao, e nao e esquecimento.</b> O produto nao guarda de onde alguem entra — "nao
 * guardamos quem fez a busca, e por onde" e a mesma politica —, e um IP nao ajudaria: ninguem
 * reconhece o proprio aparelho por "189.4.x.x". O que identifica e o navegador e quando comecou.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSessionResponseDTO {

    private UUID personSessionId;

    private LocalDateTime createdAt;

    /** Nulo enquanto a sessao vale. Com data, ela ja foi encerrada. */
    private LocalDateTime revokedAt;

    /** O navegador, cru, como ele se identificou. Nulo quando o cliente nao mandou. */
    private String userAgent;

    /**
     * Se e a sessao de onde esta chegando esta requisicao.
     *
     * <b>A tela precisa disto para nao oferecer "encerrar" sem aviso na sessao atual</b> — encerrar
     * a propria e sair, e a pessoa merece saber disso antes de clicar, e nao depois de cair na tela
     * de entrada.
     */
    private boolean current;

}

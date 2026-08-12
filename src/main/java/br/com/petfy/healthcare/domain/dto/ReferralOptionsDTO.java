package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O que a tela de encaminhar precisa saber antes de perguntar qualquer coisa (Tela 45).
 *
 * Uma chamada, e não uma por caixa: as contagens saem de uma consulta agregada na linha do tempo, e
 * o total é a mesma que a Tela 32 já usava para pôr dois cadastros lado a lado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralOptionsDTO {

    private UUID animalId;

    private String animalName;

    /** "Histórico completo desde 2019 · 147 eventos" — a quarta caixa do desenho. */
    private long totalEvents;

    private LocalDateTime firstEventAt;

    /** Quantos dias o acesso vale se o tutor autorizar. Os 90 que a tela promete. */
    private int defaultAccessDays;

    private List<ReferralScopeOptionDTO> scopes;

}

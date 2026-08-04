package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * A lista traz apenas o que pede acao - vencidas e vencendo dentro da janela.
 * Os contadores cobrem todas as vacinas do tutor, para a tela mostrar "3 em dia"
 * sem precisar de outra chamada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaccineAgendaResponseDTO {

    private LocalDate referenceDate;

    private int windowDays;

    private int overdueCount;

    private int dueSoonCount;

    private int upToDateCount;

    private int withoutNextDoseCount;

    /** Vencidas primeiro, da mais atrasada para a mais recente. */
    private List<VaccineAgendaItemDTO> items;

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Regra de "esta dose esta em dia?", usada pela agenda do tutor e pela carteira
 * compartilhada. Fica separada para que as duas nao divirjam: um pet nao pode
 * aparecer em dia num lugar e atrasado no outro.
 */
@Component
public class VaccineStatusCalculator {

    public VaccineStatus classify(LocalDate nextDoseDate, LocalDate hoje, int windowDays) {
        if (nextDoseDate == null) {
            return VaccineStatus.NO_NEXT_DOSE;
        }

        // vencer hoje conta como vencendo, nao como vencido
        if (nextDoseDate.isBefore(hoje)) {
            return VaccineStatus.OVERDUE;
        }

        return nextDoseDate.isAfter(hoje.plusDays(windowDays))
                ? VaccineStatus.UP_TO_DATE
                : VaccineStatus.DUE_SOON;
    }

}

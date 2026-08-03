package br.com.petfy.healthcare.notification;

import br.com.petfy.healthcare.domain.entity.Owner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/** Um lembrete por tutor, agrupando todas as doses pendentes dos pets dele. */
@Getter
@RequiredArgsConstructor
public class VaccineReminder {

    private final Owner owner;

    private final List<Item> items;

    @Getter
    @RequiredArgsConstructor
    public static class Item {

        private final String petName;

        private final String vaccineName;

        private final java.time.LocalDate nextDoseDate;

        /** Negativo quando ja venceu. */
        private final long daysUntilNextDose;

        public boolean isOverdue() {
            return daysUntilNextDose < 0;
        }
    }

}

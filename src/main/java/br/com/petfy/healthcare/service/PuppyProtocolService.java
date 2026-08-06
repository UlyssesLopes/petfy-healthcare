package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Ao cadastrar um filhote, cria o esquema inicial de vacinacao como doses
 * planejadas (application_date nulo, next_dose_date no futuro). Para cada vacina
 * mandatoria da especie, gera N doses espacadas pelo intervalo do catalogo.
 *
 * Adulto nao recebe nada aqui - o comportamento antigo se mantem: o tutor
 * registra vacina caso a caso, e a proxima dose sai do intervalo anual do
 * catalogo. A hipotese e que quem cadastra animal adulto ja tem historico proprio
 * e nao quer que o sistema invente doses passadas.
 *
 * A geracao e uma sugestao: o tutor pode editar ou apagar cada dose planejada
 * depois. So evita que o cadastro do filhote termine em "proxima em um ano",
 * que era o comportamento anterior e nao servia ao momento de maior
 * necessidade do usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PuppyProtocolService {

    private final VaccineCatalogRepository vaccineCatalogRepository;
    private final VaccineRepository vaccineRepository;

    /** Ate esta idade em dias, o animal e considerado filhote. */
    @Value("${petfy.puppy.max-age-days:120}")
    private int maxAgeDays;

    /**
     * Gera as doses planejadas. Silencioso quando o animal nao e filhote ou quando
     * nao ha vacina mandatoria para a especie - o cadastro segue normal.
     *
     * As doses saem contadas a partir de hoje, e nao da data de nascimento. Se
     * o animal ja passou de alguma dose recomendada, calcular retroativamente
     * criaria vacinas com data passada e sem aplicacao - o que confunde a
     * agenda e nao ajuda o tutor. Mais util assumir que a proxima dose comeca
     * agora e o intervalo segue dali.
     */
    public void gerarEsquemaInicialSePuppy(Animal animal) {
        if (!isFilhote(animal)) {
            return;
        }

        List<VaccineCatalog> mandatorias = vaccineCatalogRepository
                .findBySpeciesAndMandatoryTrueOrderByNameAsc(animal.getSpecies());

        if (mandatorias.isEmpty()) {
            log.debug("Nenhuma vacina mandatoria para {} no catalogo, esquema inicial vazio", animal.getSpecies());
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDateTime agora = LocalDateTime.now();
        List<Vaccine> planejadas = new ArrayList<>();

        for (VaccineCatalog catalog : mandatorias) {
            int doses = catalog.getInitialDoseCount() == null ? 1 : catalog.getInitialDoseCount();
            int intervalo = catalog.getInitialDoseIntervalDays() == null ? 0 : catalog.getInitialDoseIntervalDays();

            for (int i = 0; i < doses; i++) {
                planejadas.add(Vaccine.builder()
                        .animal(animal)
                        .catalog(catalog)
                        .vaccineName(catalog.getName())
                        // applicationDate nulo: e uma dose planejada, ainda nao
                        // aplicada. Se o tutor aplicar depois, ele registra a
                        // aplicacao editando esta linha
                        .applicationDate(null)
                        .nextDoseDate(hoje.plusDays((long) i * intervalo))
                        .description(String.format("Dose %d de %d do protocolo inicial", i + 1, doses))
                        .creationDate(agora)
                        .updateDate(agora)
                        .build());
            }
        }

        vaccineRepository.saveAll(planejadas);
        log.info("Esquema inicial de {} vacinas gerado para o animal {} ({} dias, especie {})",
                planejadas.size(), animal.getAnimalId(), idadeEmDias(animal), animal.getSpecies());
    }

    private boolean isFilhote(Animal animal) {
        if (animal.getBornDate() == null) {
            return false;
        }
        return idadeEmDias(animal) <= maxAgeDays;
    }

    private long idadeEmDias(Animal animal) {
        return ChronoUnit.DAYS.between(animal.getBornDate(), LocalDate.now());
    }

}

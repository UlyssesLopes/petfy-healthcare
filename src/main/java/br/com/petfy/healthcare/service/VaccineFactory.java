package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Organization;
import br.com.petfy.healthcare.domain.entity.Person;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.domain.repository.VaccineRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Monta a vacina a partir do request, resolvendo catalogo, nome e proxima dose.
 *
 * Existe separado porque uma vacina agora nasce de dois lugares - o tutor
 * registrando no proprio animal, e o veterinario registrando num animal autorizado - e
 * as duas rotas precisam calcular a proxima dose do mesmo jeito.
 */
@Component
@RequiredArgsConstructor
public class VaccineFactory {

    private final VaccineCatalogRepository vaccineCatalogRepository;

    private final VaccineRepository vaccineRepository;

    public Vaccine build(Animal animal, Organization organization, Person recordedBy,
                         VaccineRequestDTO request) {
        VaccineCatalog catalog = request.getVaccineCatalogId() != null
                ? buscarNoCatalogo(request.getVaccineCatalogId())
                : null;

        if (catalog != null && catalog.getSpecies() != animal.getSpecies()) {
            // Sem essa checagem, o tutor poderia registrar uma vacina canina num
            // gato e o sistema seguiria como se fosse valido. E o tipo de erro
            // que so aparece quando alguem for cobrar por que o lembrete errado
            // saiu.
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.SPECIES_MISMATCH.getMessage(),
                    ErrorMessageEnum.SPECIES_MISMATCH.getCode(),
                    HttpStatus.CONFLICT);
        }

        String nome = resolverNome(request, catalog);
        recusarDoseDuplicada(animal, catalog, nome, request.getApplicationDate());

        return Vaccine.builder()
                .animal(animal)
                .organization(organization)
                .recordedBy(recordedBy)
                .catalog(catalog)
                .vaccineName(nome)
                .applicationDate(request.getApplicationDate())
                .nextDoseDate(resolverProximaDose(request, catalog))
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
    }

    /**
     * <b>A recusa de dose duplicada.</b> "Dois registros da mesma dose viram dose dobrada no
     * historico" — e o cenario nao e hipotetico: a clinica registra a aplicacao e o tutor
     * registra a mesma dose minutos depois, cada um achando que o outro nao registrou.
     *
     * <b>Mora aqui, e nao no servico, porque a vacina nasce de dois lugares</b> — o tutor no
     * proprio animal e o veterinario num animal autorizado — e sao exatamente esses dois que se
     * atropelam. Guarda posta em um dos servicos deixaria o outro passar, que e o caso real.
     *
     * <b>Mesma dose e: mesmo animal, mesmo dia, mesma vacina.</b> O casamento e por catalogo com
     * queda para nome, igual ao da comprovacao da creche: registro digitado em texto livre e
     * registro anterior ao catalogo tem `catalog` nulo, e ignora-los deixaria passar justamente a
     * duplicata que vem da clinica que escolheu do catalogo contra o tutor que digitou.
     *
     * <b>Sem data de aplicacao nao ha recusa.</b> Dose planejada e dose sem data existem, e duas
     * ausencias nao provam que sao a mesma coisa — recusar ai seria barrar registro legitimo por
     * falta de um campo.
     *
     * O que ela NAO cobre, de proposito: a edicao. Mudar a data de um registro para bater com
     * outro cria a duplicata sem passar por aqui. E raro e tem rastro (o
     * {@link VaccineCorrectionLog} grava a versao anterior), enquanto a gravacao dupla e comum e
     * silenciosa.
     */
    private void recusarDoseDuplicada(Animal animal, VaccineCatalog catalog, String nome,
                                      LocalDate applicationDate) {
        if (applicationDate == null) {
            return;
        }

        boolean jaRegistrada = vaccineRepository
                .findByAnimalAnimalIdAndApplicationDate(animal.getAnimalId(), applicationDate)
                .stream()
                .anyMatch(existente -> ehAMesmaVacina(existente, catalog, nome));

        if (jaRegistrada) {
            throw new PetfyHealthcareException(
                    ErrorMessageEnum.DOSE_ALREADY_REGISTERED.getMessage(),
                    ErrorMessageEnum.DOSE_ALREADY_REGISTERED.getCode(),
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Catalogo bate com catalogo; na falta dele, nome com nome.
     *
     * O nome e comparado sem diferenciar maiuscula e sem espaco nas pontas, porque "Antirrábica"
     * digitado pelo tutor e "antirrabica " digitado pela recepcao sao a mesma dose para quem le o
     * historico — e e o historico que o produto responde.
     */
    private boolean ehAMesmaVacina(Vaccine existente, VaccineCatalog catalog, String nome) {
        if (catalog != null && existente.getCatalog() != null) {
            return catalog.getVaccineCatalogId().equals(existente.getCatalog().getVaccineCatalogId());
        }

        return existente.getVaccineName() != null
                && existente.getVaccineName().trim().equalsIgnoreCase(nome.trim());
    }

    /**
     * O nome vem do catalogo quando a vacina e escolhida da lista. Continua
     * obrigatorio quando o tutor digita em texto livre - por isso a validacao
     * esta aqui e nao como @NotBlank no DTO, que nao enxerga essa condicao.
     */
    private String resolverNome(VaccineRequestDTO request, VaccineCatalog catalog) {
        if (request.getVaccineName() != null && !request.getVaccineName().isBlank()) {
            return request.getVaccineName();
        }

        if (catalog != null) {
            return catalog.getName();
        }

        throw new PetfyHealthcareException(
                "vaccineName e obrigatorio quando vaccineCatalogId nao e informado",
                ErrorMessageEnum.INVALID_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Soma o intervalo de reforco do catalogo a data de aplicacao - o ponto do
     * catalogo e o tutor nao ter que estimar isso. Data enviada explicitamente
     * sempre vence, para o caso de orientacao diferente do veterinario.
     */
    private LocalDate resolverProximaDose(VaccineRequestDTO request, VaccineCatalog catalog) {
        if (request.getNextDoseDate() != null) {
            return request.getNextDoseDate();
        }

        if (catalog == null || catalog.getDefaultIntervalDays() == null || request.getApplicationDate() == null) {
            return null;
        }

        return request.getApplicationDate().plusDays(catalog.getDefaultIntervalDays());
    }

    private VaccineCatalog buscarNoCatalogo(UUID vaccineCatalogId) {
        return vaccineCatalogRepository.findById(vaccineCatalogId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

}

package br.com.petfy.healthcare.service;

import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.entity.Clinic;
import br.com.petfy.healthcare.domain.entity.Animal;
import br.com.petfy.healthcare.domain.entity.Vaccine;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
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

    public Vaccine build(Animal animal, Clinic clinic, VaccineRequestDTO request) {
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

        return Vaccine.builder()
                .animal(animal)
                .clinic(clinic)
                .catalog(catalog)
                .vaccineName(resolverNome(request, catalog))
                .applicationDate(request.getApplicationDate())
                .nextDoseDate(resolverProximaDose(request, catalog))
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();
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

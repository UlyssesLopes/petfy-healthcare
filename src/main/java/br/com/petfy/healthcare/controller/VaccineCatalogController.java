package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.VaccineCatalogResponseDTO;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.entity.VaccineCatalog;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Somente leitura: o catalogo e mantido por migration. Nao ha service proprio
 * porque nao ha regra alguma - so a listagem para o cliente montar a selecao.
 *
 * Filtrar por petId devolve apenas as vacinas da especie daquele pet - o
 * default para o cliente da UI, evitando que o tutor escolha vacina que nao
 * casa com o pet.
 */
@RestController
@RequestMapping("/vaccine-catalog")
@RequiredArgsConstructor
public class VaccineCatalogController {

    private final VaccineCatalogRepository vaccineCatalogRepository;
    private final PetRepository petRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    @GetMapping
    public ResponseEntity<List<VaccineCatalogResponseDTO>> list(@RequestParam(required = false) UUID petId) {
        List<VaccineCatalog> catalogo = petId == null
                ? vaccineCatalogRepository.findAllByOrderBySpeciesAscNameAsc()
                : vaccineCatalogRepository.findBySpeciesOrderByNameAsc(buscarPetDoDono(petId).getSpecies());

        return ResponseEntity.ok(catalogo.stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    /**
     * Pet de outro dono responde PET_NOT_FOUND, e nao 403: um 403 confirmaria
     * que aquele id existe, o que permitiria varrer ids para descobrir o que ha
     * na base. Mesma regra do PetServiceImpl.
     */
    private Pet buscarPetDoDono(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private VaccineCatalogResponseDTO toResponse(VaccineCatalog entrada) {
        return VaccineCatalogResponseDTO.builder()
                .vaccineCatalogId(entrada.getVaccineCatalogId())
                .code(entrada.getCode())
                .name(entrada.getName())
                .species(entrada.getSpecies())
                .defaultIntervalDays(entrada.getDefaultIntervalDays())
                .initialDoseCount(entrada.getInitialDoseCount())
                .initialDoseIntervalDays(entrada.getInitialDoseIntervalDays())
                .mandatory(entrada.getMandatory())
                .description(entrada.getDescription())
                .build();
    }

}

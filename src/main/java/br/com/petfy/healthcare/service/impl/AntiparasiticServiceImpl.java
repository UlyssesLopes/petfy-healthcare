package br.com.petfy.healthcare.service.impl;

import br.com.petfy.healthcare.domain.dto.AntiparasiticCatalogResponseDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticRequestDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticResponseDTO;
import br.com.petfy.healthcare.domain.entity.Antiparasitic;
import br.com.petfy.healthcare.domain.entity.AntiparasiticCatalog;
import br.com.petfy.healthcare.domain.entity.Pet;
import br.com.petfy.healthcare.domain.repository.AntiparasiticCatalogRepository;
import br.com.petfy.healthcare.domain.repository.AntiparasiticRepository;
import br.com.petfy.healthcare.domain.repository.PetRepository;
import br.com.petfy.healthcare.exception.PetfyHealthcareException;
import br.com.petfy.healthcare.security.CurrentOwnerProvider;
import br.com.petfy.healthcare.service.AntiparasiticService;
import br.com.petfy.healthcare.service.enums.ErrorMessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AntiparasiticServiceImpl implements AntiparasiticService {

    private final AntiparasiticRepository antiparasiticRepository;
    private final AntiparasiticCatalogRepository catalogRepository;
    private final PetRepository petRepository;
    private final CurrentOwnerProvider currentOwnerProvider;

    @Override
    public AntiparasiticResponseDTO create(AntiparasiticRequestDTO request) {
        Pet pet = buscarPetDoOwnerAutenticado(request.getPetId());

        AntiparasiticCatalog catalog = request.getAntiparasiticCatalogId() != null
                ? buscarNoCatalogo(request.getAntiparasiticCatalogId())
                : null;

        String name = resolverNome(request, catalog);
        String kind = resolverKind(request, catalog);

        Antiparasitic entity = Antiparasitic.builder()
                .pet(pet)
                .name(name)
                .kind(kind)
                .catalog(catalog)
                .applicationDate(request.getApplicationDate())
                .nextDoseDate(resolverProximaDose(request, catalog))
                .description(request.getDescription())
                .creationDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .build();

        return toResponse(antiparasiticRepository.save(entity));
    }

    @Override
    public AntiparasiticResponseDTO update(UUID id, AntiparasiticRequestDTO request) {
        Antiparasitic existing = buscarDoOwnerAutenticado(id);

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getKind() != null) existing.setKind(request.getKind());
        if (request.getApplicationDate() != null) existing.setApplicationDate(request.getApplicationDate());
        if (request.getNextDoseDate() != null) existing.setNextDoseDate(request.getNextDoseDate());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());

        if (request.getPetId() != null) {
            existing.setPet(buscarPetDoOwnerAutenticado(request.getPetId()));
        }

        existing.setUpdateDate(LocalDateTime.now());

        return toResponse(antiparasiticRepository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        antiparasiticRepository.delete(buscarDoOwnerAutenticado(id));
    }

    @Override
    public AntiparasiticResponseDTO getById(UUID id) {
        return toResponse(buscarDoOwnerAutenticado(id));
    }

    @Override
    public List<AntiparasiticResponseDTO> listByPet(UUID petId) {
        // verifica ownership do pet antes de listar
        buscarPetDoOwnerAutenticado(petId);

        return antiparasiticRepository.findByPetPetIdOrderByApplicationDateDesc(petId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AntiparasiticCatalogResponseDTO> listCatalog() {
        return catalogRepository.findAll()
                .stream()
                .map(this::toCatalogResponse)
                .collect(Collectors.toList());
    }

    private String resolverNome(AntiparasiticRequestDTO request, AntiparasiticCatalog catalog) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName();
        }
        if (catalog != null) {
            return catalog.getName();
        }
        throw new PetfyHealthcareException(
                "name e obrigatorio quando antiparasiticCatalogId nao e informado",
                ErrorMessageEnum.INVALID_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST);
    }

    private String resolverKind(AntiparasiticRequestDTO request, AntiparasiticCatalog catalog) {
        if (request.getKind() != null && !request.getKind().isBlank()) {
            return request.getKind();
        }
        if (catalog != null) {
            return catalog.getKind();
        }
        throw new PetfyHealthcareException(
                "kind e obrigatorio quando antiparasiticCatalogId nao e informado (DEWORMER ou FLEA_TICK)",
                ErrorMessageEnum.INVALID_REQUEST.getCode(),
                HttpStatus.BAD_REQUEST);
    }

    private LocalDate resolverProximaDose(AntiparasiticRequestDTO request, AntiparasiticCatalog catalog) {
        if (request.getNextDoseDate() != null) {
            return request.getNextDoseDate();
        }
        if (catalog == null || catalog.getDefaultIntervalDays() == null || request.getApplicationDate() == null) {
            return null;
        }
        return request.getApplicationDate().plusDays(catalog.getDefaultIntervalDays());
    }

    /**
     * Antiparasitario de pet de outro dono responde NOT_FOUND, nao 403:
     * um 403 confirmaria que aquele id existe.
     */
    private Antiparasitic buscarDoOwnerAutenticado(UUID id) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return antiparasiticRepository.findById(id)
                .filter(a -> a.getPet().getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private Pet buscarPetDoOwnerAutenticado(UUID petId) {
        UUID ownerId = currentOwnerProvider.require().getOwnerId();

        return petRepository.findById(petId)
                .filter(pet -> pet.getOwner().getOwnerId().equals(ownerId))
                .orElseThrow(() -> new PetfyHealthcareException(
                        ErrorMessageEnum.PET_NOT_FOUND.getMessage(),
                        ErrorMessageEnum.PET_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private AntiparasiticCatalog buscarNoCatalogo(UUID catalogId) {
        return catalogRepository.findById(catalogId)
                .orElseThrow(() -> new PetfyHealthcareException(
                        "Antiparasitic catalog entry not found",
                        ErrorMessageEnum.VACCINE_CATALOG_NOT_FOUND.getCode(),
                        HttpStatus.NOT_FOUND));
    }

    private AntiparasiticResponseDTO toResponse(Antiparasitic a) {
        return AntiparasiticResponseDTO.builder()
                .antiparasiticId(a.getAntiparasiticId())
                .petId(a.getPet().getPetId())
                .name(a.getName())
                .kind(a.getKind())
                .applicationDate(a.getApplicationDate())
                .nextDoseDate(a.getNextDoseDate())
                .description(a.getDescription())
                .antiparasiticCatalogId(a.getCatalog() != null ? a.getCatalog().getAntiparasiticCatalogId() : null)
                .creationDate(a.getCreationDate())
                .updateDate(a.getUpdateDate())
                .build();
    }

    private AntiparasiticCatalogResponseDTO toCatalogResponse(AntiparasiticCatalog c) {
        return AntiparasiticCatalogResponseDTO.builder()
                .antiparasiticCatalogId(c.getAntiparasiticCatalogId())
                .code(c.getCode())
                .name(c.getName())
                .kind(c.getKind())
                .species(c.getSpecies())
                .defaultIntervalDays(c.getDefaultIntervalDays())
                .description(c.getDescription())
                .build();
    }

}

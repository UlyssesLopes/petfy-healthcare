package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.VaccineCatalogResponseDTO;
import br.com.petfy.healthcare.domain.repository.VaccineCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Somente leitura: o catalogo e mantido por migration. Nao ha service proprio
 * porque nao ha regra alguma - so a listagem para o cliente montar a selecao.
 */
@RestController
@RequestMapping("/vaccine-catalog")
@RequiredArgsConstructor
public class VaccineCatalogController {

    private final VaccineCatalogRepository vaccineCatalogRepository;

    @GetMapping
    public ResponseEntity<List<VaccineCatalogResponseDTO>> listAll() {
        return ResponseEntity.ok(vaccineCatalogRepository.findAllByOrderBySpeciesAscNameAsc()
                .stream()
                .map(entrada -> VaccineCatalogResponseDTO.builder()
                        .vaccineCatalogId(entrada.getVaccineCatalogId())
                        .code(entrada.getCode())
                        .name(entrada.getName())
                        .species(entrada.getSpecies())
                        .defaultIntervalDays(entrada.getDefaultIntervalDays())
                        .description(entrada.getDescription())
                        .build())
                .collect(Collectors.toList()));
    }

}

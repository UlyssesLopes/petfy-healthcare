package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.AntiparasiticCatalogResponseDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticRequestDTO;
import br.com.petfy.healthcare.domain.dto.AntiparasiticResponseDTO;
import br.com.petfy.healthcare.service.AntiparasiticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/antiparasitics")
@RequiredArgsConstructor
public class AntiparasiticController {

    private final AntiparasiticService antiparasiticService;

    @Operation(summary = "Registra uma aplicacao de antiparasitario")
    @PostMapping
    public ResponseEntity<AntiparasiticResponseDTO> create(@Valid @RequestBody AntiparasiticRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(antiparasiticService.create(request));
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados.
     * Campos ausentes ou nulos preservam o valor existente.
     */
    @Operation(summary = "Atualiza a aplicacao")
    @PutMapping("/{antiparasiticId}")
    public ResponseEntity<AntiparasiticResponseDTO> update(
            @PathVariable UUID antiparasiticId,
            @RequestBody AntiparasiticRequestDTO request) {
        return ResponseEntity.ok(antiparasiticService.update(antiparasiticId, request));
    }

    @Operation(summary = "Remove a aplicacao", description = "Para o registro criado por engano.")
    @DeleteMapping("/{antiparasiticId}")
    public ResponseEntity<Void> delete(@PathVariable UUID antiparasiticId) {
        antiparasiticService.delete(antiparasiticId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Uma aplicacao de antiparasitario")
    @GetMapping("/{antiparasiticId}")
    public ResponseEntity<AntiparasiticResponseDTO> getById(@PathVariable UUID antiparasiticId) {
        return ResponseEntity.ok(antiparasiticService.getById(antiparasiticId));
    }

    /**
     * Lista antiparasitarios de um animal especifico do tutor autenticado.
     * animalId e obrigatorio: a listagem e sempre escopada ao animal para evitar
     * misturar vermifugos de animals diferentes numa lista plana.
     */
    @Operation(summary = "As aplicacoes de antiparasitario de um animal",
               description = "animalId e obrigatorio: a listagem e sempre escopada ao animal, para nao "
                             + "misturar vermifugo de animais diferentes numa lista plana.")
    @GetMapping
    public ResponseEntity<List<AntiparasiticResponseDTO>> listByAnimal(@RequestParam UUID animalId) {
        return ResponseEntity.ok(antiparasiticService.listByAnimal(animalId));
    }

    /**
     * Catalogo somente leitura, mantido por migration.
     *
     * Filtrar por animalId devolve apenas os produtos da especie daquele animal - o
     * default para o cliente da UI, evitando que o tutor escolha antiparasitario
     * que nao casa com o animal. Mesmo contrato do /vaccine-catalog.
     */
    @Operation(summary = "Os antiparasitarios que existem para esta especie",
               description = "Somente leitura, mantido por migration. Com animalId, filtra pela especie "
                             + "dele - o que evita o tutor escolher produto que nao serve para o animal. "
                             + "Mesmo contrato do /vaccine-catalog.")
    @GetMapping("/catalog")
    public ResponseEntity<List<AntiparasiticCatalogResponseDTO>> listCatalog(
            @RequestParam(required = false) UUID animalId) {
        return ResponseEntity.ok(antiparasiticService.listCatalog(animalId));
    }

}

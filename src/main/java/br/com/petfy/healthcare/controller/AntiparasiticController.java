package br.com.petfy.healthcare.controller;

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

    @PostMapping
    public ResponseEntity<AntiparasiticResponseDTO> create(@Valid @RequestBody AntiparasiticRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(antiparasiticService.create(request));
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados.
     * Campos ausentes ou nulos preservam o valor existente.
     */
    @PutMapping("/{antiparasiticId}")
    public ResponseEntity<AntiparasiticResponseDTO> update(
            @PathVariable UUID antiparasiticId,
            @RequestBody AntiparasiticRequestDTO request) {
        return ResponseEntity.ok(antiparasiticService.update(antiparasiticId, request));
    }

    @DeleteMapping("/{antiparasiticId}")
    public ResponseEntity<Void> delete(@PathVariable UUID antiparasiticId) {
        antiparasiticService.delete(antiparasiticId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{antiparasiticId}")
    public ResponseEntity<AntiparasiticResponseDTO> getById(@PathVariable UUID antiparasiticId) {
        return ResponseEntity.ok(antiparasiticService.getById(antiparasiticId));
    }

    /**
     * Lista antiparasitarios de um pet especifico do tutor autenticado.
     * petId e obrigatorio: a listagem e sempre escopada ao pet para evitar
     * misturar vermifugos de pets diferentes numa lista plana.
     */
    @GetMapping
    public ResponseEntity<List<AntiparasiticResponseDTO>> listByPet(@RequestParam UUID petId) {
        return ResponseEntity.ok(antiparasiticService.listByPet(petId));
    }

    /** Catalogo somente leitura, mantido por migration. */
    @GetMapping("/catalog")
    public ResponseEntity<List<AntiparasiticCatalogResponseDTO>> listCatalog() {
        return ResponseEntity.ok(antiparasiticService.listCatalog());
    }

}

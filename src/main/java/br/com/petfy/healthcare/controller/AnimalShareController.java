package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalShareResponseDTO;
import br.com.petfy.healthcare.service.AnimalShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Gestao dos links pelo tutor. A leitura publica fica no SharedCardController. */
@RestController
@RequiredArgsConstructor
public class AnimalShareController {

    private final AnimalShareService animalShareService;

    @PostMapping("/animals/{animalId}/shares")
    public ResponseEntity<AnimalShareResponseDTO> createShare(@PathVariable UUID animalId,
                                                           @Valid @RequestBody(required = false) AnimalShareRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalShareService.createShare(animalId, request));
    }

    @GetMapping("/animals/{animalId}/shares")
    public ResponseEntity<List<AnimalShareResponseDTO>> listShares(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalShareService.listShares(animalId));
    }

    @DeleteMapping("/shares/{animalShareId}")
    public ResponseEntity<Void> revokeShare(@PathVariable UUID animalShareId) {
        animalShareService.revokeShare(animalShareId);
        return ResponseEntity.noContent().build();
    }

}

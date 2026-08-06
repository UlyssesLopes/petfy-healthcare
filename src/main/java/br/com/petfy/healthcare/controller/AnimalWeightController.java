package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalWeightResponseDTO;
import br.com.petfy.healthcare.service.AnimalWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/animals/{animalId}/weights")
@RequiredArgsConstructor
public class AnimalWeightController {

    private final AnimalWeightService animalWeightService;

    @PostMapping
    public ResponseEntity<AnimalWeightResponseDTO> addWeight(
            @PathVariable UUID animalId,
            @Valid @RequestBody AnimalWeightRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalWeightService.addWeight(animalId, request));
    }

    /** Serie historica ordenada da mais recente para a mais antiga. */
    @GetMapping
    public ResponseEntity<List<AnimalWeightResponseDTO>> listWeights(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalWeightService.listWeights(animalId));
    }

}

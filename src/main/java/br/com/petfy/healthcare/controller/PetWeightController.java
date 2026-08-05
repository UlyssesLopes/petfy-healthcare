package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetWeightResponseDTO;
import br.com.petfy.healthcare.service.PetWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets/{petId}/weights")
@RequiredArgsConstructor
public class PetWeightController {

    private final PetWeightService petWeightService;

    @PostMapping
    public ResponseEntity<PetWeightResponseDTO> addWeight(
            @PathVariable UUID petId,
            @Valid @RequestBody PetWeightRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petWeightService.addWeight(petId, request));
    }

    /** Serie historica ordenada da mais recente para a mais antiga. */
    @GetMapping
    public ResponseEntity<List<PetWeightResponseDTO>> listWeights(@PathVariable UUID petId) {
        return ResponseEntity.ok(petWeightService.listWeights(petId));
    }

}

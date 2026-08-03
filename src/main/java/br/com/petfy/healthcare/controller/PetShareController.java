package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetShareResponseDTO;
import br.com.petfy.healthcare.service.PetShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Gestao dos links pelo tutor. A leitura publica fica no SharedCardController. */
@RestController
@RequiredArgsConstructor
public class PetShareController {

    private final PetShareService petShareService;

    @PostMapping("/pets/{petId}/shares")
    public ResponseEntity<PetShareResponseDTO> createShare(@PathVariable UUID petId,
                                                           @Valid @RequestBody(required = false) PetShareRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petShareService.createShare(petId, request));
    }

    @GetMapping("/pets/{petId}/shares")
    public ResponseEntity<List<PetShareResponseDTO>> listShares(@PathVariable UUID petId) {
        return ResponseEntity.ok(petShareService.listShares(petId));
    }

    @DeleteMapping("/shares/{petShareId}")
    public ResponseEntity<Void> revokeShare(@PathVariable UUID petShareId) {
        petShareService.revokeShare(petShareId);
        return ResponseEntity.noContent().build();
    }

}

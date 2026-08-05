package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetResponseDTO> createPet(@Valid @RequestBody PetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(dto));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponseDTO> getPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(petService.getPetById(petId));
    }

    @GetMapping
    public ResponseEntity<Page<PetResponseDTO>> listAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(petService.listAllPets(pageable));
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados. Campos ausentes ou
     * nulos no payload sao ignorados e o valor existente e preservado. Por isso
     * @Valid nao e aplicado aqui - nenhum campo e obrigatorio numa atualizacao
     * parcial.
     */
    @Operation(summary = "Atualiza dados do pet",
               description = "Atualizacao parcial: apenas os campos presentes no payload sao alterados. " +
                             "Campos ausentes ou nulos preservam o valor existente. " +
                             "Nenhum campo e obrigatorio neste endpoint.")
    @PutMapping("/{petId}")
    public ResponseEntity<PetResponseDTO> updatePet(@PathVariable UUID petId, @RequestBody PetRequestDTO dto) {
        return ResponseEntity.ok(petService.updatePet(petId, dto));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable UUID petId) {
        petService.deletePet(petId);
        return ResponseEntity.noContent().build();
    }

}

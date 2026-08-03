package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping("/include")
    public ResponseEntity<PetResponseDTO> createPet(@Valid @RequestBody PetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(dto));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponseDTO> getPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(petService.getPetById(petId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PetResponseDTO>> listAll() {
        return ResponseEntity.ok(petService.listAllPets());
    }

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

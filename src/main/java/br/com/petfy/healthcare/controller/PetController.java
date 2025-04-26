package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetResponseDTO;
import br.com.petfy.healthcare.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping("/include")
    public ResponseEntity<PetResponseDTO> createPet(@RequestBody PetRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(dto));
    }

    @GetMapping("/{petId}")
    public ResponseEntity<PetResponseDTO> getPet(@PathVariable UUID id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<PetResponseDTO>> listAll() {
        return ResponseEntity.ok(petService.listAllPets());
    }

    @PutMapping("/{petId}")
    public ResponseEntity<PetResponseDTO> updatePet(@PathVariable UUID id, @RequestBody PetRequestDTO dto) {
        return ResponseEntity.ok(petService.updatePet(id, dto));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> deletePet(@PathVariable UUID id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }

}

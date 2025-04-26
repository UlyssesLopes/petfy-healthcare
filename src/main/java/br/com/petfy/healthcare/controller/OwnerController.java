package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.service.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @PostMapping("/include")
    public ResponseEntity<OwnerResponseDTO> createOwner(@RequestBody OwnerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createOwner(request));
    }

    @GetMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> getOwnerById(@PathVariable("id") UUID ownerId) {
        return ResponseEntity.ok(ownerService.getOwnerById(ownerId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<OwnerResponseDTO>> getAllOwners() {
        return ResponseEntity.ok(ownerService.listAllOwners());
    }

    @PutMapping("/{ownerId}")
    public ResponseEntity<OwnerResponseDTO> updateOwner(@PathVariable UUID id, @RequestBody OwnerRequestDTO request) {
        return ResponseEntity.ok(ownerService.updateOwner(id, request));
    }

    @DeleteMapping("/{ownerId}")
    public ResponseEntity<Void> deleteOwner(@PathVariable UUID id) {
        ownerService.deleteOwner(id);
        return ResponseEntity.noContent().build();
    }


}

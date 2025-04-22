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
        OwnerResponseDTO createdOwner = ownerService.createOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOwner);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerResponseDTO> getOwnerById(@PathVariable("id") UUID ownerId) {
        OwnerResponseDTO ownerById = ownerService.getOwnerById(ownerId);
        return ResponseEntity.ok(ownerById);
    }

    @GetMapping("/all")
    public ResponseEntity<List<OwnerResponseDTO>> getAllOwners() {
        List<OwnerResponseDTO> owners = ownerService.listAllOwners();
        return ResponseEntity.ok(owners);
    }

}

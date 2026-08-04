package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.OwnerRequestDTO;
import br.com.petfy.healthcare.domain.dto.OwnerResponseDTO;
import br.com.petfy.healthcare.domain.dto.PasswordChangeRequestDTO;
import br.com.petfy.healthcare.service.OwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/owners")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /** Unica rota publica de owner: sem ela nao existe o primeiro usuario. */
    @PostMapping("/include")
    public ResponseEntity<OwnerResponseDTO> createOwner(@Valid @RequestBody OwnerRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createOwner(request));
    }

    @GetMapping("/me")
    public ResponseEntity<OwnerResponseDTO> getCurrentOwner() {
        return ResponseEntity.ok(ownerService.getCurrentOwner());
    }

    @PutMapping("/me")
    public ResponseEntity<OwnerResponseDTO> updateCurrentOwner(@RequestBody OwnerRequestDTO request) {
        return ResponseEntity.ok(ownerService.updateCurrentOwner(request));
    }

    /**
     * Separado do PUT /me porque aqui a validacao vale sempre: o PUT e parcial e
     * preserva campo ausente, o que nao serve para uma troca de senha.
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody PasswordChangeRequestDTO request) {
        ownerService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentOwner() {
        ownerService.deleteCurrentOwner();
        return ResponseEntity.noContent().build();
    }

}

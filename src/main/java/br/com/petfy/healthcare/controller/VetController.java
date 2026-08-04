package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.VetRequestDTO;
import br.com.petfy.healthcare.domain.dto.VetResponseDTO;
import br.com.petfy.healthcare.service.VetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/vets")
@RequiredArgsConstructor
public class VetController {

    private final VetService vetService;

    /** Publico pelo mesmo motivo do cadastro de tutor: sem isso nao existe o primeiro vet. */
    @PostMapping
    public ResponseEntity<VetResponseDTO> register(@Valid @RequestBody VetRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetService.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<VetResponseDTO> getCurrentVet() {
        return ResponseEntity.ok(vetService.getCurrentVet());
    }

}

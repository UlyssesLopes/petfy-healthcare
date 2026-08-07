package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.AnimalShareRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalShareResponseDTO;
import br.com.petfy.healthcare.service.AnimalShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Gestao dos links pelo tutor. A leitura publica fica no SharedCardController. */
@RestController
@RequiredArgsConstructor
public class AnimalShareController {

    private final AnimalShareService animalShareService;

    @Operation(summary = "Cria um link para o cartao do animal",
               description = "O tutor escolhe o escopo e o prazo. E a compensacao por nao haver "
                             + "quebra-vidro: em vez de o produto abrir o prontuario numa emergencia, "
                             + "o tutor prepara de vespera o minimo que quem socorre precisa ver. O "
                             + "token devolvido aqui E a credencial - e a unica vez que ele aparece.")
    @PostMapping("/animals/{animalId}/shares")
    public ResponseEntity<AnimalShareResponseDTO> createShare(@PathVariable UUID animalId,
                                                           @Valid @RequestBody(required = false) AnimalShareRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalShareService.createShare(animalId, request));
    }

    @Operation(summary = "Os links criados para este animal",
               description = "Sem o token: quem ja o tem nao precisa dele de novo, e listar credencial "
                             + "seria entregar a chave a qualquer leitura da tela.")
    @GetMapping("/animals/{animalId}/shares")
    public ResponseEntity<List<AnimalShareResponseDTO>> listShares(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalShareService.listShares(animalId));
    }

    @Operation(summary = "Revoga o link",
               description = "Idempotente: revogar duas vezes nao e erro, e a primeira data e que vale.")
    @DeleteMapping("/shares/{animalShareId}")
    public ResponseEntity<Void> revokeShare(@PathVariable UUID animalShareId) {
        animalShareService.revokeShare(animalShareId);
        return ResponseEntity.noContent().build();
    }

}

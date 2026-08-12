package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalSightingRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalSightingResponseDTO;
import br.com.petfy.healthcare.service.AnimalSightingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** "Vi o gato hoje" — o gesto mais frequente da colônia (Tela 43). */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalSightingController {

    private final AnimalSightingService animalSightingService;

    @Operation(summary = "Marca que viu o animal",
               description = "IDEMPOTENTE por pessoa, animal e dia: tocar duas vezes devolve o "
                             + "mesmo registro em vez de erro — quem passa na praca de manha e a "
                             + "noite ja fez o que queria fazer. Duas PESSOAS marcando o mesmo "
                             + "dia sao dois registros, porque confirmacao e informacao. Exige "
                             + "escrita, e nao custodia: marcar que viu e do que qualquer um pode "
                             + "fazer. Corpo opcional — sem data, e hoje.")
    @PostMapping("/{animalId}/sightings")
    public ResponseEntity<AnimalSightingResponseDTO> registrar(
            @PathVariable UUID animalId,
            @RequestBody(required = false) AnimalSightingRequestDTO dto) {
        return ResponseEntity.ok(animalSightingService.registrar(animalId,
                dto != null ? dto : new AnimalSightingRequestDTO()));
    }

}

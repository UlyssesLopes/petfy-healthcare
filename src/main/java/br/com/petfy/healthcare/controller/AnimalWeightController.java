package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.AnimalWeightRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalWeightResponseDTO;
import br.com.petfy.healthcare.service.AnimalWeightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/animals/{animalId}/weights")
@RequiredArgsConstructor
public class AnimalWeightController {

    private final AnimalWeightService animalWeightService;

    @Operation(summary = "Registra uma pesagem",
               description = "Peso e serie, e nao campo do cadastro: cada pesagem e um evento com data "
                             + "propria, e e a curva que diz algo clinico - nao o numero de hoje. "
                             + "measuredAt e quando o animal foi pesado, nao quando alguem digitou.")
    @PostMapping
    public ResponseEntity<AnimalWeightResponseDTO> addWeight(
            @PathVariable UUID animalId,
            @Valid @RequestBody AnimalWeightRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animalWeightService.addWeight(animalId, request));
    }

    /** Serie historica ordenada da mais recente para a mais antiga. */
    @Operation(summary = "A serie de pesagens do animal",
               description = "E daqui que sai a curva de peso. A linha do tempo traz a pesagem com o "
                             + "valor anterior, para a entrada mostrar a variacao; o grafico precisa de "
                             + "N pontos, e os N pontos estao nesta rota.")
    @GetMapping
    public ResponseEntity<List<AnimalWeightResponseDTO>> listWeights(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalWeightService.listWeights(animalId));
    }

}

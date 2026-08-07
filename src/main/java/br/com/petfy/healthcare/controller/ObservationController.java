package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ObservationRequestDTO;
import br.com.petfy.healthcare.domain.dto.ObservationResponseDTO;
import br.com.petfy.healthcare.service.ObservationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * O que alguem viu com o animal.
 *
 * Sob {@code /animals/{animalId}} porque o animal e a ancora de autorizacao, como no resto
 * do produto. E <b>fora de {@code /professional/**}</b>: quem mais registra observacao e o
 * monitor da creche, que e membro sem CRMV, e o tutor em casa - exigir credencial aqui
 * transformaria observacao em ato clinico por via de autorizacao.
 */
@RestController
@RequestMapping("/animals/{animalId}/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @Operation(summary = "Registra o que se viu com o animal",
               description = "Qualquer um com acesso de escrita registra: tutor, monitor, lar "
                             + "transitorio, voluntario. observedAt ausente assume agora - preencha "
                             + "quando o que se viu aconteceu antes da digitacao. urgent sinaliza o "
                             + "alerta, que pede atencao sem ser emergencia medica nem ato clinico.")
    @PostMapping
    public ResponseEntity<ObservationResponseDTO> create(
            @PathVariable UUID animalId,
            @Valid @RequestBody ObservationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(observationService.create(animalId, request));
    }

    @Operation(summary = "As observacoes do animal",
               description = "Ordenadas por quando foram vistas, mais recentes primeiro - e nao por "
                             + "quando foram digitadas.")
    @GetMapping
    public ResponseEntity<List<ObservationResponseDTO>> listByAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(observationService.listByAnimal(animalId));
    }

}

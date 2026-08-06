package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionFulfillmentResponseDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionRequestDTO;
import br.com.petfy.healthcare.domain.dto.CareInstructionResponseDTO;
import br.com.petfy.healthcare.service.CareInstructionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Orientacoes de cuidado do animal, e a confirmacao de que foram cumpridas.
 *
 * Sob {@code /animals/{animalId}} porque o animal e a ancora de autorizacao - e, aqui,
 * tambem a ancora do conceito: a orientacao segue a custodia, entao ela pertence ao
 * animal e nao a quem deve cumpri-la.
 */
@RestController
@RequestMapping("/animals/{animalId}/care-instructions")
@RequiredArgsConstructor
public class CareInstructionController {

    private final CareInstructionService careInstructionService;

    @Operation(summary = "Emite uma orientacao de cuidado",
               description = "Serve a prescricao do veterinario, a medicacao continua e o tema de "
                             + "casa da creche - os tres tem a mesma forma. Informe "
                             + "X-Petfy-Organization para emitir em nome de uma organizacao; sem o "
                             + "header a orientacao sai assinada apenas pela pessoa. endsOn ausente "
                             + "significa tratamento indefinido.")
    @PostMapping
    public ResponseEntity<CareInstructionResponseDTO> create(
            @PathVariable UUID animalId,
            @Valid @RequestBody CareInstructionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careInstructionService.create(animalId, request));
    }

    @Operation(summary = "Orientacoes do animal",
               description = "Inclui as encerradas: tratamento suspenso e informacao clinica, e quem "
                             + "ler o historico depois precisa saber que houve. O campo vigente diz "
                             + "o que vale hoje.")
    @GetMapping
    public ResponseEntity<List<CareInstructionResponseDTO>> listByAnimal(@PathVariable UUID animalId) {
        return ResponseEntity.ok(careInstructionService.listByAnimal(animalId));
    }

    @Operation(summary = "Encerra a orientacao",
               description = "Tira das pendencias sem apagar do historico. Idempotente: a segunda "
                             + "chamada nao mexe na data nem em quem encerrou. Nao existe PUT - "
                             + "mudar a dose e emitir orientacao nova, porque editar o texto "
                             + "reescreveria o que os cumprimentos anteriores atestam.")
    @DeleteMapping("/{careInstructionId}")
    public ResponseEntity<CareInstructionResponseDTO> revoke(@PathVariable UUID animalId,
                                                            @PathVariable UUID careInstructionId) {
        return ResponseEntity.ok(careInstructionService.revoke(animalId, careInstructionId));
    }

    @Operation(summary = "Confirma que a orientacao foi cumprida",
               description = "Evento proprio, e nao um booleano na orientacao: e o que transforma "
                             + "orientacao em historico de aderencia. fulfilledAt ausente significa "
                             + "agora, e nao aceita futuro. Responde 409 se a orientacao nao estava "
                             + "valendo no instante informado.")
    @PostMapping("/{careInstructionId}/fulfillments")
    public ResponseEntity<CareInstructionFulfillmentResponseDTO> confirmFulfillment(
            @PathVariable UUID animalId,
            @PathVariable UUID careInstructionId,
            @Valid @RequestBody(required = false) CareInstructionFulfillmentRequestDTO request) {
        CareInstructionFulfillmentRequestDTO corpo = request != null
                ? request
                : new CareInstructionFulfillmentRequestDTO();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(careInstructionService.confirmFulfillment(animalId, careInstructionId, corpo));
    }

    @Operation(summary = "Historico de cumprimento da orientacao",
               description = "Do mais recente ao mais antigo. E o dado que o veterinario nao tem "
                             + "quando o tratamento nao funciona: saber se alguem estava dando o "
                             + "remedio.")
    @GetMapping("/{careInstructionId}/fulfillments")
    public ResponseEntity<List<CareInstructionFulfillmentResponseDTO>> listFulfillments(
            @PathVariable UUID animalId, @PathVariable UUID careInstructionId) {
        return ResponseEntity.ok(careInstructionService.listFulfillments(animalId, careInstructionId));
    }

}

package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.BoardingRequestDTO;
import br.com.petfy.healthcare.domain.dto.BoardingResponseDTO;
import br.com.petfy.healthcare.service.BoardingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * O animal fora de casa por uma semana (Tela 47).
 *
 * <b>Nao ha recurso novo aqui: uma estadia E uma custodia.</b> As rotas moram debaixo do animal
 * porque e sobre ele que se fala, e nao debaixo da creche — a mesma estadia e lida pelo tutor em
 * viagem e pela creche que esta com o animal, e uma URL com o id da organizacao obrigaria o tutor a
 * saber de quem e a creche antes de perguntar como esta o cachorro dele.
 *
 * <b>Os eventos da estadia nao vem por aqui.</b> Eles vem da linha do tempo de sempre, com
 * {@code ?since=} igual ao {@code startedAt} desta resposta — o mesmo endpoint, o mesmo
 * mascaramento por escopo, nenhuma segunda verdade sobre o que aconteceu com o animal.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class BoardingController {

    private final BoardingService boardingService;

    @Operation(summary = "Entrega o animal para hospedagem",
               description = "A custodia passa para a organizacao, com prazo, e volta no dia em que "
                             + "alguem registrar a devolucao. Exige RESPONDER pelo animal: passar a "
                             + "custodia adiante e o mesmo ato que transferir titularidade, e nenhum "
                             + "nivel de concessao chega la. Quem entrega ganha uma concessao pelo "
                             + "tempo da estadia — sem ela, perderia o proprio animal de vista.")
    @PostMapping("/{animalId}/boarding")
    public ResponseEntity<BoardingResponseDTO> hospedar(
            @PathVariable UUID animalId,
            @Valid @RequestBody BoardingRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardingService.hospedar(animalId, dto));
    }

    @Operation(summary = "A estadia em curso",
               description = "Onde ele esta, desde quando, ate quando, e em que dia da estadia "
                             + "estamos. O dia e contado no servidor: um calculo no cliente diria "
                             + "'dia 2' para quem abriu o Petfy em Lisboa.")
    @GetMapping("/{animalId}/boarding")
    public ResponseEntity<BoardingResponseDTO> emCurso(@PathVariable UUID animalId) {
        return ResponseEntity.ok(boardingService.emCurso(animalId));
    }

    @Operation(summary = "Registra a volta",
               description = "A custodia retorna a quem entregou, e a concessao da estadia e "
                             + "revogada. Pode ser registrada pela organizacao que esta com o animal "
                             + "OU por quem o entregou — sem o segundo, uma creche que esquecesse de "
                             + "registrar deixaria o animal fora de casa para sempre.")
    @PostMapping("/{animalId}/boarding/end")
    public ResponseEntity<BoardingResponseDTO> devolver(@PathVariable UUID animalId) {
        return ResponseEntity.ok(boardingService.devolver(animalId));
    }

}

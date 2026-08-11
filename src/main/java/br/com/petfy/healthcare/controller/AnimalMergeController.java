package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestDTO;
import br.com.petfy.healthcare.domain.dto.AnimalMergeRequestResponseDTO;
import br.com.petfy.healthcare.service.AnimalMergeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * O mesmo animal, cadastrado duas vezes (Tela 32).
 *
 * <b>As rotas de PEDIR e de DECIDIR moram em lugares diferentes de proposito.</b> Pedir e sobre um
 * animal — "quero unir este ao meu cadastro" —, e decidir e sobre o pedido. Se o aceite morasse
 * sob o animal, ele pareceria mais uma operacao do animal entre outras; ele nao e. E a unica coisa
 * neste produto que faz dois cadastros virarem um, e ela e irreversivel.
 */
@RestController
@RequiredArgsConstructor
public class AnimalMergeController {

    private final AnimalMergeService animalMergeService;

    @Operation(summary = "Outros cadastros com o mesmo microchip deste animal",
               description = "A deteccao da duplicata. Devolve o minimo para a comparacao — nome, "
                             + "especie, raca, quem responde e o TAMANHO da linha do tempo —, e "
                             + "nenhum evento: o prontuario do outro cadastro continua protegido "
                             + "pelo escopo. Nao filtra pelo que quem pergunta alcanca, porque a "
                             + "clinica que acabou de cadastrar quase nunca alcanca o cadastro "
                             + "antigo — e e por isso que ela nao sabia que ele existia.")
    @GetMapping("/animals/{animalId}/duplicates")
    public ResponseEntity<List<AnimalMergeRequestResponseDTO.LadoDaUniao>> duplicates(
            @PathVariable UUID animalId) {
        return ResponseEntity.ok(animalMergeService.duplicatasDe(animalId));
    }

    @Operation(summary = "Pede a uniao a quem responde pelo animal",
               description = "O animal do caminho e o que SOBREVIVE — e dele o dono que decide, e "
                             + "e na tela dele que o pedido aparece. Quem pede so precisa alcancar "
                             + "os dois cadastros: exigir custodia trancaria o pedido para a unica "
                             + "pessoa que tem como perceber a duplicata.")
    @PostMapping("/animals/{animalId}/merge-requests")
    public ResponseEntity<AnimalMergeRequestResponseDTO> requestMerge(
            @PathVariable UUID animalId,
            @Valid @RequestBody AnimalMergeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(animalMergeService.pedir(animalId, request));
    }

    @Operation(summary = "Os pedidos de uniao esperando decisao sobre este animal")
    @GetMapping("/animals/{animalId}/merge-requests")
    public ResponseEntity<List<AnimalMergeRequestResponseDTO>> pendingMerges(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalMergeService.pendentesDe(animalId));
    }

    @Operation(summary = "Tudo que ja envolveu este cadastro, inclusive o recusado",
               description = "A recusa importa tanto quanto o aceite: ela e a afirmacao de que os "
                             + "dois sao animais diferentes, e o motivo original fica ao lado — "
                             + "informacao util na proxima vez que os dois aparecerem parecidos.")
    @GetMapping("/animals/{animalId}/merge-requests/history")
    public ResponseEntity<List<AnimalMergeRequestResponseDTO>> mergeHistory(@PathVariable UUID animalId) {
        return ResponseEntity.ok(animalMergeService.historicoDe(animalId));
    }

    @Operation(summary = "Aceita a uniao — irreversivel",
               description = "Os eventos do cadastro absorvido passam para o sobrevivente, cada um "
                             + "mantendo quem o registrou e a data de lancamento original. O "
                             + "absorvido vira apontador e nao e apagado. Unir NAO transfere "
                             + "custodia nem concede acesso a ninguem. So quem responde pelo "
                             + "animal sobrevivente pode.")
    @PostMapping("/merge-requests/{animalMergeRequestId}/accept")
    public ResponseEntity<AnimalMergeRequestResponseDTO> acceptMerge(
            @PathVariable UUID animalMergeRequestId) {
        return ResponseEntity.ok(animalMergeService.aceitar(animalMergeRequestId));
    }

    @Operation(summary = "Sao animais diferentes — dito por quem percebeu, sem pedido nenhum",
               description = "Marca o microchip em conflito nos dois cadastros. NAO passa por quem "
                             + "responde pelo animal, e nao deveria: pedir a uniao mexe na vida "
                             + "registrada e por isso precisa dele; dizer 'sao outros bichos' nao "
                             + "mexe em nada — so acende uma marca. Basta alcancar os dois. Nao "
                             + "cancela pedido pendente, se houver: a marca e uma observacao sobre "
                             + "o microchip, e o pedido e uma pergunta feita a outra pessoa.")
    @PostMapping("/animals/{animalId}/duplicates/{otherAnimalId}/distinct")
    public ResponseEntity<Void> markAsDistinct(@PathVariable UUID animalId,
                                               @PathVariable UUID otherAnimalId) {
        animalMergeService.marcarComoDiferentes(animalId, otherAnimalId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Recusa: sao animais diferentes",
               description = "Marca o microchip em conflito nos DOIS cadastros — provavelmente ha "
                             + "um erro de digitacao em algum lugar, e alguem vai precisar saber "
                             + "disso. O produto nao adivinha qual dos dois esta errado.")
    @PostMapping("/merge-requests/{animalMergeRequestId}/reject")
    public ResponseEntity<AnimalMergeRequestResponseDTO> rejectMerge(
            @PathVariable UUID animalMergeRequestId) {
        return ResponseEntity.ok(animalMergeService.recusar(animalMergeRequestId));
    }

}

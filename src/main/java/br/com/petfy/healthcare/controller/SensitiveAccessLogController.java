package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.SensitiveAccessLogResponseDTO;
import br.com.petfy.healthcare.service.SensitiveAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Quem, de fora, leu o dado de saude deste animal.
 *
 * Ate aqui havia rastro de escrita e nenhum de leitura: um veterinario podia abrir o
 * historico completo e o tutor nunca saberia. Registra acesso de terceiro - clinica
 * autorizada e link publico -, nao a leitura do proprio tutor nem a dos co-tutores.
 */
@RestController
@RequestMapping("/animals/{animalId}/access-log")
@RequiredArgsConstructor
public class SensitiveAccessLogController {

    private final SensitiveAccessLogService sensitiveAccessLogService;

    @Operation(summary = "Acessos de terceiros ao dado de saude do animal",
               description = "Do mais recente para o mais antigo. Inclui leitura por veterinario de "
                             + "clinica autorizada e abertura do link publico de carteira. Leitura do "
                             + "proprio tutor e dos co-tutores nao entra - o log responde 'quem MAIS viu "
                             + "isto', e a resposta nao inclui quem pergunta.")
    @GetMapping
    public ResponseEntity<Page<SensitiveAccessLogResponseDTO>> listByAnimal(
            @PathVariable UUID animalId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sensitiveAccessLogService.listByAnimal(animalId, pageable));
    }

}

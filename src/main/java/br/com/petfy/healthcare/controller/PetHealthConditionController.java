package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetHealthConditionRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetHealthConditionResponseDTO;
import br.com.petfy.healthcare.service.PetHealthConditionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Alergias e condicoes cronicas do pet.
 *
 * Sob {@code /pets/{petId}} porque o pet e a ancora de autorizacao, como em todo o resto
 * do prontuario.
 */
@RestController
@RequestMapping("/pets/{petId}/conditions")
@RequiredArgsConstructor
public class PetHealthConditionController {

    private final PetHealthConditionService petHealthConditionService;

    @Operation(summary = "Registra alergia ou condicao cronica",
               description = "severity vale apenas para ALERGIA. Em CONDICAO_CRONICA responde 400 - "
                             + "gravidade de diabetes nao se mede em leve, moderada e grave.")
    @PostMapping
    public ResponseEntity<PetHealthConditionResponseDTO> create(
            @PathVariable UUID petId,
            @Valid @RequestBody PetHealthConditionRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(petHealthConditionService.create(petId, request));
    }

    @Operation(summary = "Alergias e condicoes do pet",
               description = "As ativas vem primeiro: a pergunta que a lista responde e o que vale "
                             + "para este animal hoje. Condicao encerrada aparece com resolvedAt "
                             + "preenchido, porque faz parte do historico.")
    @GetMapping
    public ResponseEntity<List<PetHealthConditionResponseDTO>> listByPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(petHealthConditionService.listByPet(petId));
    }

    /**
     * Atualizacao parcial, como nos outros PUT do projeto.
     *
     * <b>Encerrar uma condicao e preencher {@code resolvedAt} por aqui</b>, e nao apagar:
     * condicao que passou faz parte do historico do animal.
     */
    @Operation(summary = "Atualiza a condicao",
               description = "Parcial: campo ausente preserva o valor. O kind nao pode mudar - trocar "
                             + "o tipo nao e corrigir um campo, e dizer que era outra coisa desde o "
                             + "comeco. Para encerrar, preencha resolvedAt.")
    @PutMapping("/{conditionId}")
    public ResponseEntity<PetHealthConditionResponseDTO> update(
            @PathVariable UUID petId,
            @PathVariable UUID conditionId,
            @RequestBody PetHealthConditionRequestDTO request) {
        return ResponseEntity.ok(petHealthConditionService.update(petId, conditionId, request));
    }

    @Operation(summary = "Remove a condicao",
               description = "Para o registro criado por engano. Condicao que passou nao se apaga: "
                             + "preenche-se resolvedAt.")
    @DeleteMapping("/{conditionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID petId, @PathVariable UUID conditionId) {
        petHealthConditionService.delete(petId, conditionId);
        return ResponseEntity.noContent().build();
    }

}

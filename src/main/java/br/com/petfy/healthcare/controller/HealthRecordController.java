package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.service.HealthRecordService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/health-records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    @PostMapping
    public ResponseEntity<HealthRecordResponseDTO> createHealthRecord(@Valid @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(healthRecordService.createHealthRecord(request));
    }

    @GetMapping("/{healthRecordId}")
    public ResponseEntity<HealthRecordResponseDTO> getHealthRecordById(@PathVariable UUID healthRecordId) {
        return ResponseEntity.ok(healthRecordService.getHealthRecordById(healthRecordId));
    }

    @GetMapping
    public ResponseEntity<Page<HealthRecordResponseDTO>> listAll(
            @PageableDefault(size = 20, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(healthRecordService.listAllHealthRecords(pageable));
    }

    /** Rastro de alteracoes: quem mudou o que, e quando. */
    @GetMapping("/{healthRecordId}/corrections")
    public ResponseEntity<List<HealthRecordCorrectionResponseDTO>> listCorrections(@PathVariable UUID healthRecordId) {
        return ResponseEntity.ok(healthRecordService.listCorrections(healthRecordId));
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<HealthRecordResponseDTO>> listByPet(@PathVariable UUID petId) {
        return ResponseEntity.ok(healthRecordService.listHealthRecordsByPet(petId));
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados. Campos ausentes ou
     * nulos no payload sao ignorados e o valor existente e preservado. Por isso
     * @Valid nao e aplicado aqui - nenhum campo e obrigatorio numa atualizacao
     * parcial.
     */
    @Operation(summary = "Atualiza prontuario de saude",
               description = "Atualizacao parcial: apenas os campos presentes no payload sao alterados. " +
                             "Campos ausentes ou nulos preservam o valor existente. " +
                             "Nenhum campo e obrigatorio neste endpoint.")
    @PutMapping("/{healthRecordId}")
    public ResponseEntity<HealthRecordResponseDTO> updateHealthRecord(@PathVariable UUID healthRecordId,
                                                                      @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.ok(healthRecordService.updateHealthRecord(healthRecordId, request));
    }

    @DeleteMapping("/{healthRecordId}")
    public ResponseEntity<Void> deleteHealthRecord(@PathVariable UUID healthRecordId) {
        healthRecordService.deleteHealthRecord(healthRecordId);
        return ResponseEntity.noContent().build();
    }

}

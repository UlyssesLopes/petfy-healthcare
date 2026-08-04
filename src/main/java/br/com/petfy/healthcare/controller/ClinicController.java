package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    public ResponseEntity<ClinicResponseDTO> createClinic(@Valid @RequestBody ClinicRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }

    @GetMapping("/{clinicId}")
    public ResponseEntity<ClinicResponseDTO> getClinicById(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(clinicService.getClinicById(clinicId));
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponseDTO>> listAll() {
        return ResponseEntity.ok(clinicService.listAllClinics());
    }

    /**
     * Atualizacao parcial: so os campos enviados sao alterados. Campos ausentes ou
     * nulos no payload sao ignorados e o valor existente e preservado. Por isso
     * @Valid nao e aplicado aqui - nenhum campo e obrigatorio numa atualizacao
     * parcial.
     */
    @Operation(summary = "Atualiza dados da clinica",
               description = "Atualizacao parcial: apenas os campos presentes no payload sao alterados. " +
                             "Campos ausentes ou nulos preservam o valor existente. " +
                             "Nenhum campo e obrigatorio neste endpoint.")
    @PutMapping("/{clinicId}")
    public ResponseEntity<ClinicResponseDTO> updateClinic(@PathVariable UUID clinicId, @RequestBody ClinicRequestDTO request) {
        return ResponseEntity.ok(clinicService.updateClinic(clinicId, request));
    }

    @DeleteMapping("/{clinicId}")
    public ResponseEntity<Void> deleteClinic(@PathVariable UUID clinicId) {
        clinicService.deleteClinic(clinicId);
        return ResponseEntity.noContent().build();
    }

}

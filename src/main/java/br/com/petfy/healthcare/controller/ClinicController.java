package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicResponseDTO;
import br.com.petfy.healthcare.service.ClinicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping("/include")
    public ResponseEntity<ClinicResponseDTO> createClinic(@RequestBody ClinicRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }

    @GetMapping("/{clinicId}")
    public ResponseEntity<ClinicResponseDTO> getClinicById(@PathVariable UUID clinicId) {
        return ResponseEntity.ok(clinicService.getClinicById(clinicId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ClinicResponseDTO>> listAll() {
        return ResponseEntity.ok(clinicService.listAllClinics());
    }

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

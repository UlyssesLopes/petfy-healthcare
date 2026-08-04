package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.VaccineAgendaResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.service.VaccineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/vaccines")
@RequiredArgsConstructor
public class VaccineController {

    private final VaccineService vaccineService;

    @PostMapping("/include")
    public ResponseEntity<VaccineResponseDTO> createVaccine(@Valid @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaccineService.createVaccine(request));
    }

    @PutMapping("/{vaccineId}")
    public ResponseEntity<VaccineResponseDTO> updateVaccine(@PathVariable UUID vaccineId, @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.ok(vaccineService.updateVaccine(vaccineId, request));
    }

    @DeleteMapping("/{vaccineId}")
    public ResponseEntity<Void> deleteVaccine(@PathVariable UUID vaccineId) {
        vaccineService.deleteVaccine(vaccineId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vaccineId}")
    public ResponseEntity<VaccineResponseDTO> getVaccineById(@PathVariable UUID vaccineId) {
        return ResponseEntity.ok(vaccineService.getVaccineById(vaccineId));
    }

    @GetMapping
    public ResponseEntity<List<VaccineResponseDTO>> listAllVaccines() {
        return ResponseEntity.ok(vaccineService.listAllVaccines());
    }

    /** Rastro de alteracoes: quem mudou o que, e quando. */
    @GetMapping("/{vaccineId}/corrections")
    public ResponseEntity<List<VaccineCorrectionResponseDTO>> listCorrections(@PathVariable UUID vaccineId) {
        return ResponseEntity.ok(vaccineService.listCorrections(vaccineId));
    }

    /**
     * Declarado antes de /{vaccineId} nao por exigencia do Spring, que casa a
     * rota literal primeiro, mas para deixar visivel que as duas dividem o
     * mesmo nivel do path.
     */
    @GetMapping("/agenda")
    public ResponseEntity<VaccineAgendaResponseDTO> getAgenda(
            @RequestParam(defaultValue = "30") int windowDays) {
        return ResponseEntity.ok(vaccineService.getAgenda(windowDays));
    }

}

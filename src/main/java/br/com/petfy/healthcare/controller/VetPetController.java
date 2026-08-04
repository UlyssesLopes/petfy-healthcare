package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.HealthRecordCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordRequestDTO;
import br.com.petfy.healthcare.domain.dto.HealthRecordResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineCorrectionResponseDTO;
import br.com.petfy.healthcare.domain.dto.VaccineRequestDTO;
import br.com.petfy.healthcare.domain.dto.VaccineResponseDTO;
import br.com.petfy.healthcare.domain.dto.VetPetDTO;
import br.com.petfy.healthcare.service.VetPetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Prefixo /vet separado do /pets do tutor de proposito: sao visoes diferentes do
 * mesmo recurso, com regras de acesso diferentes. Misturar as duas no mesmo path
 * faria a autorizacao depender de quem chamou, que e onde esse tipo de bug mora.
 *
 * Nao ha DELETE em nenhum dos dois recursos: apagar registro de saude nao e
 * correcao - ver README.
 */
@RestController
@RequestMapping("/vet/pets")
@RequiredArgsConstructor
public class VetPetController {

    private final VetPetService vetPetService;

    @GetMapping
    public ResponseEntity<List<VetPetDTO>> listAccessiblePets() {
        return ResponseEntity.ok(vetPetService.listAccessiblePets());
    }

    @GetMapping("/{petId}/vaccines")
    public ResponseEntity<List<VaccineResponseDTO>> listVaccines(@PathVariable UUID petId) {
        return ResponseEntity.ok(vetPetService.listVaccines(petId));
    }

    /**
     * Sem @Valid de proposito: no VaccineRequestDTO o unico campo obrigatorio e o
     * petId, que aqui vem do path. Exigi-lo tambem no corpo so criaria uma
     * duplicidade que o service ignora - ele usa o do path.
     */
    @PostMapping("/{petId}/vaccines")
    public ResponseEntity<VaccineResponseDTO> registerVaccine(@PathVariable UUID petId,
                                                              @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerVaccine(petId, request));
    }

    @PutMapping("/{petId}/vaccines/{vaccineId}")
    public ResponseEntity<VaccineResponseDTO> correctVaccine(@PathVariable UUID petId,
                                                             @PathVariable UUID vaccineId,
                                                             @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctVaccine(petId, vaccineId, request));
    }

    @GetMapping("/{petId}/vaccines/{vaccineId}/corrections")
    public ResponseEntity<List<VaccineCorrectionResponseDTO>> listCorrections(@PathVariable UUID petId,
                                                                              @PathVariable UUID vaccineId) {
        return ResponseEntity.ok(vetPetService.listCorrections(petId, vaccineId));
    }

    @GetMapping("/{petId}/health-records")
    public ResponseEntity<List<HealthRecordResponseDTO>> listHealthRecords(@PathVariable UUID petId) {
        return ResponseEntity.ok(vetPetService.listHealthRecords(petId));
    }

    @PostMapping("/{petId}/health-records")
    public ResponseEntity<HealthRecordResponseDTO> registerHealthRecord(@PathVariable UUID petId,
                                                                        @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerHealthRecord(petId, request));
    }

    @PutMapping("/{petId}/health-records/{healthRecordId}")
    public ResponseEntity<HealthRecordResponseDTO> correctHealthRecord(@PathVariable UUID petId,
                                                                       @PathVariable UUID healthRecordId,
                                                                       @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctHealthRecord(petId, healthRecordId, request));
    }

    @GetMapping("/{petId}/health-records/{healthRecordId}/corrections")
    public ResponseEntity<List<HealthRecordCorrectionResponseDTO>> listHealthRecordCorrections(
            @PathVariable UUID petId, @PathVariable UUID healthRecordId) {
        return ResponseEntity.ok(vetPetService.listHealthRecordCorrections(petId, healthRecordId));
    }

}

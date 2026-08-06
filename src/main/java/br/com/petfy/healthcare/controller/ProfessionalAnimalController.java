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
 * Prefixo /vet separado do /animals do tutor de proposito: sao visoes diferentes do
 * mesmo recurso, com regras de acesso diferentes. Misturar as duas no mesmo path
 * faria a autorizacao depender de quem chamou, que e onde esse tipo de bug mora.
 *
 * Nao ha DELETE em nenhum dos dois recursos: apagar registro de saude nao e
 * correcao - ver README.
 */
@RestController
@RequestMapping("/professional/animals")
@RequiredArgsConstructor
public class ProfessionalAnimalController {

    private final VetPetService vetPetService;

    @GetMapping
    public ResponseEntity<List<VetPetDTO>> listAccessibleAnimals() {
        return ResponseEntity.ok(vetPetService.listAccessibleAnimals());
    }

    @GetMapping("/{animalId}/vaccines")
    public ResponseEntity<List<VaccineResponseDTO>> listVaccines(@PathVariable UUID animalId) {
        return ResponseEntity.ok(vetPetService.listVaccines(animalId));
    }

    /**
     * Sem @Valid de proposito: no VaccineRequestDTO o unico campo obrigatorio e o
     * animalId, que aqui vem do path. Exigi-lo tambem no corpo so criaria uma
     * duplicidade que o service ignora - ele usa o do path.
     */
    @PostMapping("/{animalId}/vaccines")
    public ResponseEntity<VaccineResponseDTO> registerVaccine(@PathVariable UUID animalId,
                                                              @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerVaccine(animalId, request));
    }

    @PutMapping("/{animalId}/vaccines/{vaccineId}")
    public ResponseEntity<VaccineResponseDTO> correctVaccine(@PathVariable UUID animalId,
                                                             @PathVariable UUID vaccineId,
                                                             @RequestBody VaccineRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctVaccine(animalId, vaccineId, request));
    }

    @GetMapping("/{animalId}/vaccines/{vaccineId}/corrections")
    public ResponseEntity<List<VaccineCorrectionResponseDTO>> listCorrections(@PathVariable UUID animalId,
                                                                              @PathVariable UUID vaccineId) {
        return ResponseEntity.ok(vetPetService.listCorrections(animalId, vaccineId));
    }

    @GetMapping("/{animalId}/health-records")
    public ResponseEntity<List<HealthRecordResponseDTO>> listHealthRecords(@PathVariable UUID animalId) {
        return ResponseEntity.ok(vetPetService.listHealthRecords(animalId));
    }

    @PostMapping("/{animalId}/health-records")
    public ResponseEntity<HealthRecordResponseDTO> registerHealthRecord(@PathVariable UUID animalId,
                                                                        @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vetPetService.registerHealthRecord(animalId, request));
    }

    @PutMapping("/{animalId}/health-records/{healthRecordId}")
    public ResponseEntity<HealthRecordResponseDTO> correctHealthRecord(@PathVariable UUID animalId,
                                                                       @PathVariable UUID healthRecordId,
                                                                       @RequestBody HealthRecordRequestDTO request) {
        return ResponseEntity.ok(vetPetService.correctHealthRecord(animalId, healthRecordId, request));
    }

    @GetMapping("/{animalId}/health-records/{healthRecordId}/corrections")
    public ResponseEntity<List<HealthRecordCorrectionResponseDTO>> listHealthRecordCorrections(
            @PathVariable UUID animalId, @PathVariable UUID healthRecordId) {
        return ResponseEntity.ok(vetPetService.listHealthRecordCorrections(animalId, healthRecordId));
    }

}

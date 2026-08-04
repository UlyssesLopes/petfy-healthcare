package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicAccessResponseDTO;
import br.com.petfy.healthcare.service.PetClinicAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Lado do tutor: conceder, listar e revogar acesso de clinicas ao pet. */
@RestController
@RequestMapping("/pets/{petId}/clinic-access")
@RequiredArgsConstructor
public class PetClinicAccessController {

    private final PetClinicAccessService petClinicAccessService;

    @PostMapping
    public ResponseEntity<ClinicAccessResponseDTO> grant(@PathVariable UUID petId,
                                                         @Valid @RequestBody ClinicAccessRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petClinicAccessService.grant(petId, request));
    }

    @GetMapping
    public ResponseEntity<List<ClinicAccessResponseDTO>> list(@PathVariable UUID petId) {
        return ResponseEntity.ok(petClinicAccessService.list(petId));
    }

    @DeleteMapping("/{clinicId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID petId, @PathVariable UUID clinicId) {
        petClinicAccessService.revoke(petId, clinicId);
        return ResponseEntity.noContent().build();
    }

}

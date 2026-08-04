package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.ClinicInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.ClinicInviteResponseDTO;
import br.com.petfy.healthcare.service.ClinicInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Sob /vet, entao ja exige ROLE_VET na cadeia de filtros. */
@RestController
@RequestMapping("/vet/clinic-invites")
@RequiredArgsConstructor
public class ClinicInviteController {

    private final ClinicInviteService clinicInviteService;

    @PostMapping
    public ResponseEntity<ClinicInviteResponseDTO> create(
            @Valid @RequestBody(required = false) ClinicInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicInviteService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ClinicInviteResponseDTO>> listFromMyClinic() {
        return ResponseEntity.ok(clinicInviteService.listFromMyClinic());
    }

    @DeleteMapping("/{clinicInviteId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID clinicInviteId) {
        clinicInviteService.revoke(clinicInviteId);
        return ResponseEntity.noContent().build();
    }

}

package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationInviteResponseDTO;
import br.com.petfy.healthcare.service.OrganizationInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Sob /vet, entao ja exige ROLE_VET na cadeia de filtros. */
@RestController
@RequestMapping("/organizations/invites")
@RequiredArgsConstructor
public class OrganizationInviteController {

    private final OrganizationInviteService organizationInviteService;

    @Operation(summary = "Convida alguem para ser membro da organizacao",
               description = "Membro age em nome dela - e o vinculo, e nao um papel declarado no "
                             + "cadastro, que da acesso a area de organizacao.")
    @PostMapping
    public ResponseEntity<OrganizationInviteResponseDTO> create(
            @Valid @RequestBody(required = false) OrganizationInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationInviteService.create(request));
    }

    @Operation(summary = "Os convites de membro em aberto")
    @GetMapping
    public ResponseEntity<List<OrganizationInviteResponseDTO>> listFromMyOrganization() {
        return ResponseEntity.ok(organizationInviteService.listFromMyOrganization());
    }

    @Operation(summary = "Revoga o convite de membro", description = "Idempotente.")
    @DeleteMapping("/{organizationInviteId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID organizationInviteId) {
        organizationInviteService.revoke(organizationInviteId);
        return ResponseEntity.noContent().build();
    }

}

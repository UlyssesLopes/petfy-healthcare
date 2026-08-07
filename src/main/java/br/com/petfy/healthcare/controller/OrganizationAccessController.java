package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
import br.com.petfy.healthcare.domain.dto.OrganizationAccessRequestDTO;
import br.com.petfy.healthcare.domain.dto.OrganizationAccessResponseDTO;
import br.com.petfy.healthcare.service.OrganizationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** Lado do tutor: conceder, listar e revogar acesso de clinicas ao animal. */
@RestController
@RequestMapping("/animals/{animalId}/organization-access")
@RequiredArgsConstructor
public class OrganizationAccessController {

    private final OrganizationAccessService organizationAccessService;

    @Operation(summary = "Concede acesso deste animal a uma organizacao",
               description = "Com escopo e, se quiser, com prazo. Matricular o cachorro numa creche nao "
                             + "pode entregar a ela o prontuario inteiro - e para isso que o escopo "
                             + "existe.")
    @PostMapping
    public ResponseEntity<OrganizationAccessResponseDTO> grant(@PathVariable UUID animalId,
                                                         @Valid @RequestBody OrganizationAccessRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(organizationAccessService.grant(animalId, request));
    }

    @Operation(summary = "As organizacoes que alcancam este animal")
    @GetMapping
    public ResponseEntity<List<OrganizationAccessResponseDTO>> list(@PathVariable UUID animalId) {
        return ResponseEntity.ok(organizationAccessService.list(animalId));
    }

    @Operation(summary = "Revoga o acesso da organizacao",
               description = "Idempotente. O que a organizacao ja registrou fica: revogar acesso nao "
                             + "apaga historico, porque o registro e do animal.")
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID animalId, @PathVariable UUID organizationId) {
        organizationAccessService.revoke(animalId, organizationId);
        return ResponseEntity.noContent().build();
    }

}

package br.com.petfy.healthcare.controller;

import br.com.petfy.healthcare.domain.dto.PetTutorInviteRequestDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorInviteResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorResponseDTO;
import br.com.petfy.healthcare.domain.dto.PetTutorRoleUpdateRequestDTO;
import br.com.petfy.healthcare.service.PetTutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Quem cuida do pet, junto com quem cadastrou.
 *
 * Os niveis de cada rota estao no {@code PetTutorServiceImpl}, e nao aqui: a
 * decisao de acesso mora numa peca so desde a V15, e repeti-la em anotacao no
 * controller criaria a segunda copia que o {@code PetAccessGuard} existe para
 * evitar.
 */
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetTutorController {

    private final PetTutorService petTutorService;

    @GetMapping("/{petId}/tutors")
    public ResponseEntity<List<PetTutorResponseDTO>> listTutors(@PathVariable UUID petId) {
        return ResponseEntity.ok(petTutorService.listTutors(petId));
    }

    @PostMapping("/{petId}/tutors/invites")
    public ResponseEntity<PetTutorInviteResponseDTO> invite(
            @PathVariable UUID petId,
            @Valid @RequestBody PetTutorInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petTutorService.invite(petId, request));
    }

    @GetMapping("/{petId}/tutors/invites")
    public ResponseEntity<List<PetTutorInviteResponseDTO>> listInvites(@PathVariable UUID petId) {
        return ResponseEntity.ok(petTutorService.listInvites(petId));
    }

    @DeleteMapping("/{petId}/tutors/invites/{petTutorInviteId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID petId,
            @PathVariable UUID petTutorInviteId) {
        petTutorService.revokeInvite(petId, petTutorInviteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{petId}/tutors/{ownerId}")
    public ResponseEntity<PetTutorResponseDTO> changeRole(
            @PathVariable UUID petId,
            @PathVariable UUID ownerId,
            @Valid @RequestBody PetTutorRoleUpdateRequestDTO request) {
        return ResponseEntity.ok(petTutorService.changeRole(petId, ownerId, request));
    }

    @DeleteMapping("/{petId}/tutors/{ownerId}")
    public ResponseEntity<Void> removeTutor(
            @PathVariable UUID petId,
            @PathVariable UUID ownerId) {
        petTutorService.removeTutor(petId, ownerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST, e nao PATCH no papel: a transferencia mexe em dois vinculos ao mesmo
     * tempo - promove um e rebaixa o outro - entao nao e a edicao de um recurso.
     */
    @PostMapping("/{petId}/tutors/{ownerId}/transfer-holder")
    public ResponseEntity<PetTutorResponseDTO> transferHolder(
            @PathVariable UUID petId,
            @PathVariable UUID ownerId) {
        return ResponseEntity.ok(petTutorService.transferHolder(petId, ownerId));
    }

}

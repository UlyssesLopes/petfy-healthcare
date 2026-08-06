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
 * Quem cuida do animal, junto com quem cadastrou.
 *
 * Os niveis de cada rota estao no {@code PetTutorServiceImpl}, e nao aqui: a
 * decisao de acesso mora numa peca so desde a V15, e reanimali-la em anotacao no
 * controller criaria a segunda copia que o {@code AnimalAccessGuard} existe para
 * evitar.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class PetTutorController {

    private final PetTutorService petTutorService;

    @GetMapping("/{animalId}/tutors")
    public ResponseEntity<List<PetTutorResponseDTO>> listTutors(@PathVariable UUID animalId) {
        return ResponseEntity.ok(petTutorService.listTutors(animalId));
    }

    @PostMapping("/{animalId}/tutors/invites")
    public ResponseEntity<PetTutorInviteResponseDTO> invite(
            @PathVariable UUID animalId,
            @Valid @RequestBody PetTutorInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petTutorService.invite(animalId, request));
    }

    @GetMapping("/{animalId}/tutors/invites")
    public ResponseEntity<List<PetTutorInviteResponseDTO>> listInvites(@PathVariable UUID animalId) {
        return ResponseEntity.ok(petTutorService.listInvites(animalId));
    }

    @DeleteMapping("/{animalId}/tutors/invites/{petTutorInviteId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID animalId,
            @PathVariable UUID petTutorInviteId) {
        petTutorService.revokeInvite(animalId, petTutorInviteId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{animalId}/tutors/{personId}")
    public ResponseEntity<PetTutorResponseDTO> changeRole(
            @PathVariable UUID animalId,
            @PathVariable UUID personId,
            @Valid @RequestBody PetTutorRoleUpdateRequestDTO request) {
        return ResponseEntity.ok(petTutorService.changeRole(animalId, personId, request));
    }

    @DeleteMapping("/{animalId}/tutors/{personId}")
    public ResponseEntity<Void> removeTutor(
            @PathVariable UUID animalId,
            @PathVariable UUID personId) {
        petTutorService.removeTutor(animalId, personId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST, e nao PATCH no papel: a transferencia mexe em dois vinculos ao mesmo
     * tempo - promove um e rebaixa o outro - entao nao e a edicao de um recurso.
     */
    @PostMapping("/{animalId}/tutors/{personId}/transfer-holder")
    public ResponseEntity<PetTutorResponseDTO> transferHolder(
            @PathVariable UUID animalId,
            @PathVariable UUID personId) {
        return ResponseEntity.ok(petTutorService.transferHolder(animalId, personId));
    }

}

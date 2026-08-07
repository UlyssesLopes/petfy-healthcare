package br.com.petfy.healthcare.controller;

import io.swagger.v3.oas.annotations.Operation;
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
 * decisao de acesso mora numa peca so desde a V15, e repeti-la em anotacao no
 * controller criaria a segunda copia que o {@code AnimalAccessGuard} existe para
 * evitar.
 */
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class PetTutorController {

    private final PetTutorService petTutorService;

    @Operation(summary = "Quem responde e quem alcanca este animal por concessao")
    @GetMapping("/{animalId}/tutors")
    public ResponseEntity<List<PetTutorResponseDTO>> listTutors(@PathVariable UUID animalId) {
        return ResponseEntity.ok(petTutorService.listTutors(animalId));
    }

    @Operation(summary = "Convida um co-tutor")
    @PostMapping("/{animalId}/tutors/invites")
    public ResponseEntity<PetTutorInviteResponseDTO> invite(
            @PathVariable UUID animalId,
            @Valid @RequestBody PetTutorInviteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petTutorService.invite(animalId, request));
    }

    @Operation(summary = "Os convites de co-tutor em aberto")
    @GetMapping("/{animalId}/tutors/invites")
    public ResponseEntity<List<PetTutorInviteResponseDTO>> listInvites(@PathVariable UUID animalId) {
        return ResponseEntity.ok(petTutorService.listInvites(animalId));
    }

    @Operation(summary = "Revoga o convite de co-tutor", description = "Idempotente.")
    @DeleteMapping("/{animalId}/tutors/invites/{petTutorInviteId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID animalId,
            @PathVariable UUID petTutorInviteId) {
        petTutorService.revokeInvite(animalId, petTutorInviteId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Muda o nivel de acesso do co-tutor")
    @PatchMapping("/{animalId}/tutors/{personId}")
    public ResponseEntity<PetTutorResponseDTO> changeRole(
            @PathVariable UUID animalId,
            @PathVariable UUID personId,
            @Valid @RequestBody PetTutorRoleUpdateRequestDTO request) {
        return ResponseEntity.ok(petTutorService.changeRole(animalId, personId, request));
    }

    @Operation(summary = "Tira o alcance do co-tutor", description = "O que ele registrou fica: o registro e do animal, e nao de quem o escreveu.")
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
    @Operation(summary = "Passa a titularidade a outra pessoa", description = "Encerra a custodia atual e abre a do sucessor. A linha do tempo NAO recomeca - quem assume recebe a vida inteira do animal.")
    @PostMapping("/{animalId}/tutors/{personId}/transfer-holder")
    public ResponseEntity<PetTutorResponseDTO> transferHolder(
            @PathVariable UUID animalId,
            @PathVariable UUID personId) {
        return ResponseEntity.ok(petTutorService.transferHolder(animalId, personId));
    }

}

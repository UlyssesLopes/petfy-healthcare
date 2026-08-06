package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorInviteResponseDTO {

    private UUID petTutorInviteId;

    private UUID animalId;

    private String animalName;

    /**
     * Preenchido apenas na criacao. Nas listagens vem nulo, porque so o hash e
     * guardado - nao ha como reexibir. Quem perdeu o convite revoga e cria outro.
     */
    private String token;

    private String email;

    private PetTutorRole role;

    private String createdByOwnerName;

    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private boolean usable;

    /** Explicito na resposta: aceitar este convite troca o titular do animal. */
    private boolean transfersHolder;

}

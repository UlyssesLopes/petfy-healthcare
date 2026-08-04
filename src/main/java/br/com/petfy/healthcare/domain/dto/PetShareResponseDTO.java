package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetShareResponseDTO {

    private UUID petShareId;

    private UUID petId;

    /**
     * Preenchido apenas na criacao. Nas listagens vem nulo, porque so o hash e
     * guardado - nao ha como reexibir. Quem perdeu o link revoga e cria outro.
     */
    private String token;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime creationDate;

    private boolean active;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInviteResponseDTO {

    private UUID organizationInviteId;

    private UUID organizationId;

    private String organizationName;

    /**
     * Preenchido apenas na criacao. Nas listagens vem nulo, porque so o hash e
     * guardado - nao ha como reexibir. Quem perdeu o convite revoga e cria outro.
     */
    private String token;

    private String email;

    private String createdByVetName;

    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private boolean usable;

}

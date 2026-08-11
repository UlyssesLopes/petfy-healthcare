package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
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

    /**
     * A funcao que a pessoa tera ao aceitar. Nula nos convites emitidos antes de a funcao
     * existir no convite — e nulo aqui significa "nao declarada", e nao "sem funcao".
     */
    private MembershipRole role;

    private String createdByVetName;

    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime revokedAt;

    private boolean usable;

}

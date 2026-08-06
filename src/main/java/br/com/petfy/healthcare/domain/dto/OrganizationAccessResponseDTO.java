package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import br.com.petfy.healthcare.domain.entity.GrantScope;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationAccessResponseDTO {

    private UUID grantId;

    private UUID animalId;

    private UUID organizationId;

    private String organizationName;

    /** O quanto do animal esta concessao alcanca. */
    private Set<GrantScope> scopes;

    private LocalDateTime grantedAt;

    /** Nulo em concessao sem prazo. */
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private boolean active;

}

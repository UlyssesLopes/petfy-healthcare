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
public class AnimalShareResponseDTO {

    private UUID grantId;

    private UUID animalId;

    /**
     * Preenchido apenas na criacao. Nas listagens vem nulo, porque so o hash e
     * guardado - nao ha como reexibir. Quem perdeu o link revoga e cria outro.
     */
    private String token;

    /** O escopo que este link alcanca. */
    private Set<GrantScope> scopes;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime creationDate;

    private boolean active;

}

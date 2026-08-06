package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import br.com.petfy.healthcare.domain.entity.GrantScope;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicAccessRequestDTO {

    @NotNull(message = "clinicId e obrigatorio")
    private UUID clinicId;

    /**
     * O quanto a clinica alcanca. Ausente ou vazio recebe o escopo clinico
     * inteiro, que e o que conceder acesso a uma clinica sempre significou.
     */
    private Set<GrantScope> scopes;

    /** Prazo opcional. Nulo mantem o acesso ate ser revogado. */
    private LocalDateTime expiresAt;

}

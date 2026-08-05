package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorRoleUpdateRequestDTO {

    /**
     * Novo papel do co-tutor: EDITOR ou VIEWER.
     *
     * HOLDER nao passa por aqui. Promover alguem a titular rebaixa o titular
     * atual, entao nao e "mudar o papel de um tutor" - e a transferencia, que tem
     * endpoint proprio e uma confirmacao a mais.
     */
    @NotNull(message = "role e obrigatorio (EDITOR ou VIEWER)")
    private PetTutorRole role;

}

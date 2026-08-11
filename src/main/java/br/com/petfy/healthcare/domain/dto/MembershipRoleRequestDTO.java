package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/** O corpo do "ajustar": a funcao nova, e so ela. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipRoleRequestDTO {

    @NotNull(message = "role e obrigatorio")
    private MembershipRole role;

}

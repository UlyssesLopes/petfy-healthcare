package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionKind;
import br.com.petfy.healthcare.domain.entity.AnimalHealthConditionSeverity;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalHealthConditionRequestDTO {

    @NotNull(message = "kind e obrigatorio (ALERGIA ou CONDICAO_CRONICA)")
    private AnimalHealthConditionKind kind;

    @NotBlank(message = "description e obrigatoria")
    @Size(max = 255, message = "description nao pode passar de 255 caracteres")
    private String description;

    /**
     * So para alergia. Em condicao cronica o banco recusa - gravidade de diabetes nao se
     * mede em leve, moderada e grave.
     */
    private AnimalHealthConditionSeverity severity;

    @Size(max = 500, message = "notes nao pode passar de 500 caracteres")
    private String notes;

    private LocalDate since;

    /**
     * Preenchida encerra a condicao sem apagar o registro: condicao que passou faz parte
     * do historico do animal.
     */
    private LocalDate resolvedAt;

}

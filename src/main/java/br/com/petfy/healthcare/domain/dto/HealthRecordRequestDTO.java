package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.HealthEventCategory;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordRequestDTO {

    @NotNull(message = "animalId e obrigatorio")
    private UUID animalId;

    /**
     * Classificacao do atendimento, obrigatoria na criacao.
     *
     * Nao substitui {@link #eventType}, que continua sendo o rotulo livre: a categoria
     * responde "o que este animal ja passou", e o rotulo continua dizendo como aquele
     * atendimento se chamou.
     *
     * <b>Aqui havia um @NotBlank, e ele derrubava a rota inteira.</b> O @NotBlank so sabe
     * validar CharSequence; sobre um enum, o Hibernate Validator nao acha validador e
     * lanca UnexpectedTypeException ANTES de olhar o corpo. O resultado era 500 em todo
     * POST /health-records, com corpo valido ou nao. A anotacao sobrou de quando o campo
     * ali era texto livre, e ficou pendurada quando o enum entrou no lugar.
     */
    @NotNull(message = "category e obrigatoria (CONSULTA, RETORNO, EXAME, CIRURGIA, INTERNACAO, EMERGENCIA, PROCEDIMENTO ou OUTRO)")
    private HealthEventCategory category;

    /** Diagnostico como campo proprio, e nao enterrado na descricao. */
    @Size(max = 500, message = "diagnosis nao pode passar de 500 caracteres")
    private String diagnosis;

    private String eventType;

    private LocalDate eventDate;

    private String description;

    private UUID organizationId;

}

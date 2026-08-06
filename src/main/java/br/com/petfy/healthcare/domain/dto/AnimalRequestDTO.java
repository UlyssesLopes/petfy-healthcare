package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.Species;
import lombok.*;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * Usado tanto na criacao quanto na atualizacao. As restricoes so valem onde o
 * controller marca @Valid - hoje apenas no POST, porque o PUT e parcial de
 * proposito e preserva os campos nao enviados.
 *
 * Nao ha ownerId: o dono do animal e sempre o autenticado na requisicao. Aceitar o
 * campo do cliente deixaria criar animal no nome de outra pessoa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalRequestDTO {

    @NotBlank(message = "nome e obrigatorio")
    private String name;

    /** Sub-classificacao livre (raca, cor, etc). Nao substitui species. */
    private String type;

    private String breed;

    private LocalDate bornDate;

    @Positive(message = "peso deve ser maior que zero")
    private Double weight;

    private String gender;

    /**
     * Especie do animal - obrigatorio no cadastro. Sem isso o catalogo de vacinas
     * nao filtra corretamente e o protocolo de filhote nao roda.
     */
    @NotNull(message = "species e obrigatoria")
    private Species species;

    /**
     * Numero do microchip.
     *
     * O request nunca aceitou nem o booleano {@code microchip} - so o OCR o preenchia.
     * Passa a aceitar o numero, que e o que serve: e o identificador legal do animal e o
     * que liga o Petfy a registro de animal perdido. O booleano continua na resposta e o
     * servico o mantem coerente com o numero.
     */
    @Size(max = 32, message = "microchipNumber nao pode passar de 32 caracteres")
    private String microchipNumber;

    /** Afeta protocolo vacinal, peso esperado e risco de doenca. */
    private Boolean castrated;

    /**
     * Data da castracao. Aceita sem {@code castrated}: informar a data e afirmar o fato,
     * e exigir os dois campos juntos seria burocracia sobre o obvio.
     */
    private LocalDate castratedAt;

}

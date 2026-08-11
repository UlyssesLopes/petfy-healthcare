package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

/**
 * O pedido de unir dois cadastros do mesmo animal.
 *
 * <b>Quem pede informa qual dos dois SOBREVIVE, e nao e detalhe de implementacao.</b> A tela
 * mostra os dois lado a lado com "eventos: 147, desde 2019" contra "eventos: 1, hoje", e quem esta
 * olhando sabe qual e o cadastro real do animal. Deixar o servidor escolher — pelo mais antigo,
 * digamos — acertaria quase sempre e erraria exatamente no caso em que o cadastro antigo e o
 * errado, que e quando alguem digitou o microchip trocado ha dois anos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalMergeRequestDTO {

    /** O cadastro que sera absorvido — o que deixa de receber registro novo. */
    @NotNull(message = "absorbedAnimalId e obrigatorio")
    private UUID absorbedAnimalId;

    /**
     * Por que voce acha que e o mesmo animal.
     *
     * <b>Obrigatorio.</b> Sem motivo, quem decide recebe um pedido que so diz "una" e nao tem como
     * julgar — e o motivo vai junto para o evento da uniao, para "quem ler daqui a cinco anos
     * entender por que existem dois nomes no historico".
     */
    @NotBlank(message = "reason e obrigatorio")
    @Size(max = 500, message = "reason nao pode passar de 500 caracteres")
    private String reason;

}

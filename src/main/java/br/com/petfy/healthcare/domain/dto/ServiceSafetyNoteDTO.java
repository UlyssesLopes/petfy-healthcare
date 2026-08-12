package br.com.petfy.healthcare.domain.dto;

import lombok.*;

/**
 * Uma das quatro linhas de "o que voce precisa saber antes de encostar nele" (Tela 18).
 *
 * <b>"Quatro linhas de saude bastam para o banho ser seguro. Pedir mais que isso seria pedir o que
 * nao se usa."</b> — e "se o sistema fica bom com um tosador vendo quatro linhas, o escopo funciona.
 * E o teste mais duro da tela 09".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSafetyNoteDTO {

    /**
     * O peso da linha, para a tela desenhar o marcador certo.
     *
     * {@code MANEJO} e o losango vermelho — "displasia no quadril, nao erguer pelas patas traseiras";
     * {@code ATENCAO} e o circulo ambar; {@code OK} e o verde de "vacinacao em dia".
     */
    private String severity;

    private String text;

}

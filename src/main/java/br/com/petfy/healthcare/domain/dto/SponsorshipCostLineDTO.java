package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma linha do "o que o abrigo gasta com ele por mes" (Tela 46).
 *
 * <b>E tambem um dos botoes de "quanto voce quer bancar"</b>, e nao por economia de codigo: o desenho
 * oferece os tres valores que ele acabou de mostrar na tabela — R$ 45 a consulta, R$ 80 o remedio,
 * R$ 190 a racao. <i>"Voce banca uma coisa concreta, nao uma cota abstrata."</i> Se os botoes viessem
 * de outra fonte, o padrinho poderia bancar um valor que nao corresponde a gasto nenhum.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipCostLineDTO {

    /** Para o pedido poder dizer de qual gasto real aquele valor saiu. */
    private UUID costId;

    /** O texto que o abrigo escreveu. "Condroprotetor · uso continuo pela artrose". */
    private String description;

    private BigDecimal amount;

    /**
     * Se alguem ja banca esta linha.
     *
     * <b>Nao esconde o botao, e a diferenca importa:</b> duas pessoas podem bancar a racao de um
     * animal que come R$ 190 por mes, e o abrigo aplica o excedente onde faltar. Esconder faria o
     * produto recusar ajuda em nome de uma aritmetica que ele nao conhece.
     */
    private boolean alreadySponsored;

}

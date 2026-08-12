package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um evento que o padrinho recebe: <i>"Condroprotetor comprado em 04/08 · R$ 80 · Claudia
 * Menezes"</i> (Tela 46).
 *
 * <b>E um evento de CUSTO, e so isso — nao e a linha do tempo do animal recortada.</b> A distincao e a
 * regra inteira desta tela: <i>"Nenhum historico clinico aberto ao padrinho. Ele ve o que banca."</i>
 *
 * O custo mora fora da linha do tempo neste produto desde a Tela 40, porque "nenhum escopo de acesso
 * concede preco junto com saude" — e e justamente essa separacao que torna o feed do padrinho
 * possivel sem inventar escopo nenhum. O que ele le e a prestacao de contas que o produto ja dava ao
 * tutor: <i>"o padrinho ve o evento que o dinheiro dele pagou, assinado por quem comprou, com data"</i>.
 *
 * <b>O que NAO vem, e o desenho mesmo avisa:</b> a foto. <i>"Uma foto, quando alguem do abrigo tirar.
 * Nao prometemos foto toda semana."</i> Anexo e dado do animal e vive sob escopo — entregar um ao
 * padrinho exigiria decidir quais anexos sao publicos, o que nao existe. Fica declarado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsoredEventDTO {

    private UUID costId;

    /** O que foi comprado, como o abrigo escreveu. */
    private String description;

    private BigDecimal amount;

    /** Quando o gasto aconteceu — e nao quando alguem digitou. */
    private LocalDateTime occurredAt;

    /**
     * Quem comprou. "assinado por quem comprou" e metade da promessa da tela.
     *
     * Nulo quando o lancamento veio de uma organizacao sem pessoa atras — o que acontece, e a tela
     * mostra o nome da organizacao no lugar.
     */
    private String recordedByName;

    private String organizationName;

}

package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.AnimalCostCategory;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Uma linha de "quem pagou o que" (Tela 37).
 *
 * <b>O produto so sabe quem pagou o que uma PESSOA lancou em nome proprio</b> — a compra da Tela 42.
 * Num atendimento, quem registrou e a veterinaria; ela informou o valor, e nao quem o pagou. Chamar
 * a veterinaria de pagadora seria inventar um fato, e o desenho e explicito sobre o que este cartao
 * e: "o Petfy so mostra — nao cobra ninguem, nao faz acerto".
 *
 * <b>Por isso existe a linha sem nome</b>, com {@code personName} nulo: e onde entra tudo que uma
 * organizacao registrou. Escondida, o cartao pareceria dizer que o resto do dinheiro nao existiu, e
 * a soma das linhas nao fecharia com o total logo acima.
 *
 * <b>E por isso o produto NAO PEDE que o tutor atribua depois.</b> Seria a contabilidade que a Tela
 * 42 recusa em tres toques — "quanto mais campos, menos gente lanca". O cartao mostra o que alguem
 * disse, e para de mostrar onde ninguem disse nada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalCostPayerDTO {

    /** Nulo e "ninguem informou" — o que veio de organizacao. */
    private String personName;

    private BigDecimal amount;

    /** Em que essa pessoa gastou — o "creche, racao e banho" do desenho. */
    private List<AnimalCostCategory> categories;

}

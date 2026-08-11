package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma linha da comprovacao de saude da Tela 10.
 *
 * <b>O produto responde pela saude, e nao a creche.</b> Cada linha e o cruzamento entre o que a
 * organizacao exige e o que a carteira do animal tem — e o `blocks` diz se aquela linha impede a
 * matricula, para a tela nao ter de reimplementar a regra.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProofItemDTO {

    private UUID vaccineCatalogId;
    private String vaccineName;

    /** EM_DIA, VENCIDA, SEM_REGISTRO. */
    private String state;

    /** A dose que sustenta o EM_DIA, quando existe. */
    private LocalDate lastApplicationDate;
    private LocalDate nextDoseDate;

    /**
     * A linha impede a matricula de ativar.
     *
     * <b>Nao e o mesmo que "nao esta em dia".</b> A vacina sem registro tambem impede — o produto
     * nao pode afirmar que um animal esta protegido por uma dose que ninguem viu.
     */
    private boolean blocks;

    /**
     * O registro que sustenta esta linha foi casado por NOME, e nao pelo catalogo.
     *
     * A `Vaccine.catalog` e nula para dose digitada em texto livre e para tudo que entrou antes do
     * catalogo existir. Casar por nome funciona e e mais fraco: quem le precisa saber a diferenca
     * antes de deixar um animal entrar na creche por causa dela.
     */
    private boolean matchedByName;
}

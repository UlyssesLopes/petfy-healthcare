package br.com.petfy.healthcare.domain.dto;

import lombok.*;

/**
 * Os quatro numeros do cabecalho da Tela 03: "vencendo em 30 dias · 12", "em tratamento · 7",
 * "atendidos este mes · 41", "todos · 318".
 *
 * <b>Eles contam sobre a organizacao inteira, e nao sobre a pagina aberta.</b> Um numero que
 * muda ao virar a pagina nao e um resumo — e a decisao que ele apoia, a quem ligar hoje, e sobre
 * todo mundo.
 *
 * <b>Por que uma rota separada da listagem.</b> A lista e paginada e ordenada pelo que a tela
 * pede; o resumo varre o conjunto todo. Enfia-lo na resposta da pagina faria cada virada de
 * pagina recalcular quatro agregacoes que nao mudaram.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationPatientsSummaryDTO {

    /** Vencidas contam junto: quem ja passou do prazo e mais urgente, e nao menos. */
    private long dueIn30Days;

    private long underTreatment;

    /** Animais, e nao atendimentos: dois retornos do mesmo animal no mes sao um animal. */
    private long seenThisMonth;

    private long total;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * "Quanto o Code custou" (Tela 37).
 *
 * <b>Montado a partir do que ja esta registrado</b> — "voce nao digitou nada disto duas vezes; cada
 * valor veio junto com o evento que o gerou". Nao ha nada aqui que alguem tenha preenchido para
 * este resumo existir.
 *
 * <b>Os tres numeros do topo convivem, e nao se substituem.</b> O desenho mostra "nos ultimos 12
 * meses", "por mes em media" e "desde 2019" ao mesmo tempo, com a janela de 12 meses selecionada —
 * o terceiro cartao nao muda quando a janela muda. Por isso {@code totalEver} e {@code firstYear}
 * viajam sempre, e nao dependem do recorte pedido.
 *
 * <b>Nao ha percentual neste DTO, e a ausencia e escolha.</b> A fatia e {@code amount} sobre
 * {@code total}, e as duas ja estao aqui: mandar o percentual junto criaria uma segunda fonte para
 * a mesma razao, e o dia em que ela divergisse da largura da barra desenhada seria o dia em que o
 * tutor veria "38%" ao lado de uma barra de outro tamanho.
 *
 * <b>E nao ha comparacao com ninguem.</b> "Nenhuma tela aqui diz que voce gasta mais ou menos que
 * outros tutores, nem sugere trocar de clinica por preco. Quem cuida de um animal doente ja tem o
 * suficiente na cabeca." Nao existe consulta no repositorio capaz de responder isso.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalCostSummaryResponseDTO {

    /** O recorte pedido: {@code DOZE_MESES} ou {@code SEMPRE}. */
    private String window;

    /** O total do recorte. Zero quando nao ha custo nenhum — e nao nulo. */
    private BigDecimal total;

    /**
     * Por mes, em media, dentro do recorte.
     *
     * <b>Divide pelos meses do RECORTE, e nao pelos meses em que houve gasto.</b> Um animal que
     * custou R$ 1.200 em dois meses do ano custou R$ 100 por mes no ano — dividir por dois daria
     * R$ 600 e diria ao tutor que o animal custa seis vezes mais do que custa.
     */
    private BigDecimal monthlyAverage;

    /**
     * Tudo, desde o primeiro valor registrado.
     *
     * Viaja mesmo quando a janela e de 12 meses: e o terceiro cartao do desenho, e ele nao muda
     * quando a pessoa troca o recorte.
     */
    private BigDecimal totalEver;

    /** O ano do primeiro valor — o "desde 2019" do cartao. Nulo quando nao ha valor nenhum. */
    private Integer firstYear;

    /** "Onde foi", fatia por fatia. Categoria sem valor no recorte nao aparece. */
    private List<AnimalCostSliceDTO> byCategory;

    /**
     * "Quem pagou o que."
     *
     * <b>So tem pagador o que uma PESSOA lancou em nome proprio.</b> O que uma organizacao
     * registrou entra numa linha sem nome — ver {@link AnimalCostPayerDTO}.
     */
    private List<AnimalCostPayerDTO> byPayer;

}

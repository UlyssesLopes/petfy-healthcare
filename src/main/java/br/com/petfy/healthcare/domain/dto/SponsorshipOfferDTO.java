package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * O que a tela de apadrinhar mostra antes de perguntar qualquer coisa (Tela 46).
 *
 * <b>NAO HA NENHUM DADO CLINICO AQUI, e a ausencia e a parte pensada.</b> O desenho fecha a porta —
 * <i>"Nenhum historico clinico aberto ao padrinho"</i> —, e quem le esta tela e uma pessoa qualquer
 * com conta, que nao alcanca o animal por nada.
 *
 * O que parece dado clinico na tela sai das LINHAS DE CUSTO, que o abrigo escreveu: <i>"Condroprotetor
 * · uso continuo pela artrose"</i> e um texto que o abrigo digitou ao lancar o gasto, e nao um
 * diagnostico lido do prontuario. <b>Quem decide o que aparece e quem escreve a linha</b> — o que e
 * exatamente o controle que o desenho promete ao abrigo, sem precisar de campo novo para isso.
 *
 * A idade vem de {@code bornDate} porque idade nao e dado de saude, e e o que a frase do desenho usa
 * primeiro: <i>"Cao · SRD · 11 anos"</i>.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorshipOfferDTO {

    private UUID animalId;

    private String name;

    private String species;

    private String breed;

    private LocalDate bornDate;

    /** Quem responde por ele. "Abrigo Lar dos Focinhos". */
    private String organizationName;

    /** Desde quando ele esta lá — a custodia em curso da organizacao. */
    private LocalDate atOrganizationSince;

    /**
     * "O que o abrigo gasta com ele por mes."
     *
     * So o que se repete: uma consulta avulsa de tres anos atras nao e custo mensal, e somar tudo
     * daria ao padrinho um numero que nao corresponde a nada que ele possa bancar.
     */
    private List<SponsorshipCostLineDTO> monthlyCosts;

    /** "Custo real, todo mes." A soma das linhas acima, feita no servidor. */
    private BigDecimal monthlyTotal;

    /**
     * Quantas pessoas ja bancam algo dele.
     *
     * <b>Um numero, e nao nomes, e nao "faltam R$ 80 para a meta".</b> O desenho e explicito sobre o
     * que fica de fora: <i>"Nenhum ranking de padrinhos, nenhuma barra de meta, nenhuma urgencia
     * fabricada."</i> A contagem existe para quem chega saber que nao esta sozinho, e para nada mais.
     */
    private int sponsorCount;

    /**
     * Se quem esta lendo pode apadrinhar.
     *
     * <b>Falso para quem esta agindo EM NOME do abrigo que responde pelo animal</b>, e a razao nao e
     * rigor: o abrigo apadrinhando o proprio animal apareceria na lista dele mesmo cobrindo um custo
     * que ele paga, e o total de "custo coberto por padrinhos" passaria a incluir o proprio dinheiro
     * do abrigo. <b>Um voluntario agindo como PESSOA pode apadrinhar</b> — ele esta tirando do bolso
     * dele, e recusar isso seria paternalismo.
     */
    private boolean canSponsor;

}

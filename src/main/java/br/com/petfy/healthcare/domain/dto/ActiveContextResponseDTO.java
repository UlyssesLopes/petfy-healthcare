package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Em nome de quem esta pessoa esta agindo, e em nome de quem ela poderia agir.
 *
 * <b>Existe para tirar a ambiguidade do meio da operacao.</b> O mecanismo de troca
 * ja existia - o header {@code X-Petfy-Organization} -, mas nao havia como o cliente
 * saber quais contextos a pessoa tem: ele descobria que precisava escolher ao tomar
 * um 409 no meio de um registro. A secao 9.5 do PRODUTO cobra o contrario: <i>"agir
 * em nome de uma organizacao passa a ser algo que a pessoa escolheu antes - a
 * ambiguidade deixa de ser erro devolvido no meio de uma operacao e vira decisao
 * tomada na entrada"</i>.
 *
 * <b>Esta leitura nao exige credencial profissional</b>, e a rota fica fora de
 * {@code /professional/**} por isso: o monitor da creche e membro e nao tem CRMV, e
 * precisa ver os proprios contextos como qualquer outro.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveContextResponseDTO {

    private UUID personId;

    private String personName;

    /**
     * Se a pessoa tem credencial profissional ativa.
     *
     * Diz ao cliente se ato clinico e possivel <i>antes</i> de ele oferecer a acao.
     * Independe de contexto: e da pessoa, e nao da organizacao (5.10).
     */
    private boolean professional;

    /**
     * Se a pessoa ja confirmou o proprio e-mail.
     *
     * <b>Existe para a faixa do cabecalho</b> — "confirme seu e-mail para receber avisos de
     * vacina", que a moldura do produto pede logo abaixo dos 64 px. Sem este campo a faixa so
     * teria dois desfechos, e os dois errados: aparecer sempre, mentindo para quem ja
     * confirmou, ou nao existir, calando quem nao recebe aviso nenhum e nao sabe por que.
     *
     * <b>Mora aqui e nao no {@code /persons/me} porque quem a desenha e o cabecalho</b>, e o
     * cabecalho ja le esta rota em toda tela. Uma segunda chamada, em toda tela, para um
     * booleano seria a moldura cobrando do servidor o dobro para dizer a mesma coisa.
     *
     * O nome diz o estado, e nao o instante: a tela pergunta "confirmou?", nao "quando".
     */
    private boolean emailVerified;

    /**
     * O contexto que valeria agora, se a pessoa fizesse uma requisicao sem declarar
     * organizacao. <b>Nulo quando ambiguo</b> - e ai o cliente tem de perguntar.
     */
    private ContextOptionDTO active;

    /**
     * Verdadeiro quando ha mais de um vinculo e nenhum foi declarado.
     *
     * E o mesmo caso que responderia 409 numa operacao de escrita. Aqui ele e um
     * campo, e nao um erro: a pergunta cabe na entrada, o erro nao cabe no meio.
     */
    private boolean ambiguous;

    /**
     * Tudo em que a pessoa pode atuar.
     *
     * Traz o contexto de pessoa <b>apenas quando ela nao tem vinculo ativo</b>, que e
     * quando ele e de fato alcancavel hoje: o autonomo e "pessoa com credencial e sem
     * vinculo" (9.3). Quem tem vinculo e quer atuar por si nao tem como declarar isso
     * - esta registrado como lacuna no ROADMAP, e nao se resolve inventando aqui.
     */
    private List<ContextOptionDTO> available;

}

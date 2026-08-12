package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.List;

/**
 * O resultado da busca autenticada (Tela 35).
 *
 * <b>Dois grupos, e o desenho os separa por uma razao de leitura:</b> "Seus animais" e "Animais que
 * voce alcanca pela Clinica Vet Norte". Sao alcances de naturezas diferentes — um e responder pelo
 * animal, o outro e um acesso que alguem concedeu e pode revogar amanha. Uma lista unica faria a
 * veterinaria achar que os animais dos clientes sao dela.
 *
 * <b>E o campo que quase nenhum produto teria:</b> {@link #othersExist}. Ver o javadoc dele.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalSearchResultDTO {

    /** Os que voce responde. */
    private List<AnimalSearchItemDTO> mine;

    /** Os que voce alcanca pela organizacao em que esta agindo. Vazio quando age como pessoa. */
    private List<AnimalSearchItemDTO> throughOrganization;

    /** O nome da organizacao, para o cabecalho do segundo grupo. Nulo quando nao ha uma declarada. */
    private String organizationName;

    /**
     * <b>"Existem outros animais com microchip comecando em 9810 no Petfy. Voce nao tem acesso a eles,
     * e por isso nao aparecem aqui."</b>
     *
     * A saida obvia seria devolver a lista curta e calar sobre o resto — e o efeito seria a pessoa
     * concluir que o animal que ela procura nao esta no produto. Dizer que ele existe e que ela nao o
     * alcanca e a unica resposta verdadeira, e e ela que torna util o caminho seguinte: a busca de
     * animal encontrado, que e publica e existe desde a Tela 34.
     *
     * <b>Booleano, e nao contagem</b>: um numero seria um oraculo, e quem variasse o termo mediria
     * quantos animais existem com cada prefixo de microchip.
     */
    private boolean othersExist;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * "Sua conta" (Tela 36).
 *
 * <b>Uma tela de conta que diz o que cada coisa SIGNIFICA</b>, e nao uma lista de campos: "nome e
 * telefone — aparecem para quem cuida dos seus animais, e no cartao de emergencia"; "registro
 * profissional — sem ele, voce nao registra diagnostico nem prescricao". Este DTO carrega os fatos; o
 * significado e da tela, e mora nas mensagens.
 *
 * <b>O que NAO esta aqui: as sessoes abertas.</b> O desenho pede "3 sessoes abertas, a mais antiga e
 * de 11/2025", e este produto autentica com JWT sem estado — o servidor nao sabe quantos tokens
 * validos existem, e nao teria como invalidar um deles. Um numero estimado seria pior que a ausencia,
 * porque a pessoa clicaria em "encerrar" acreditando ter encerrado. A tela diz que nao sabe.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountOverviewDTO {

    private String name;

    private String email;

    private String phone;

    /**
     * Quando a senha foi trocada pela ultima vez. <b>Nulo significa "nunca desde que a conta
     * existe"</b> — e a tela diz isso, em vez de mostrar a data do cadastro como se fosse troca.
     */
    private LocalDateTime passwordChangedAt;

    /** "CRMV-SP 12345". Nulo em quem nao declarou nenhum. */
    private String professionalCredential;

    /**
     * Os animais por que esta pessoa responde AGORA.
     *
     * <b>E a lista que decide se a conta pode ser encerrada</b>, e a tela a usa para oferecer o
     * caminho: "o Code e o Bartolomeu precisam de alguem antes que voce saia. Passe cada um para outra
     * pessoa — a vida registrada deles vai junto e nao se apaga com a sua conta."
     */
    private List<AnimalSearchItemDTO> animalsUnderMyResponsibility;

    /**
     * Se o encerramento esta disponivel.
     *
     * <b>Vem do servidor, e o servidor recusa de novo se alguem tentar.</b> A regra e do produto
     * (3.4), e nao da tela: ninguem sai do Petfy deixando um animal sem quem responda por ele.
     */
    private boolean canDeleteAccount;

}

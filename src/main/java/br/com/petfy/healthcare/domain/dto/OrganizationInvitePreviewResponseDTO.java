package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O convite visto por quem o recebeu, antes de aceitar.
 *
 * <b>Existe porque aceitar as cegas nao e aceitar.</b> Quem clica num link precisa ler de qual
 * organizacao e o convite, com que funcao e ate quando vale, antes de entrar numa equipe que
 * enxerga a saude de animais alheios. Uma tela que so oferecesse "aceitar" pediria confianca
 * num token opaco.
 *
 * <b>Nao traz o token de volta, nem quem mais foi convidado.</b> O que ele responde e "o que e
 * este convite que esta na minha mao", e nada alem disso — o cadastro de convites da organizacao
 * continua sendo coisa de quem e da organizacao.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationInvitePreviewResponseDTO {

    private UUID organizationId;

    private String organizationName;

    /**
     * A funcao que a pessoa tera. Nula em convite antigo, emitido antes de a funcao existir —
     * e a tela diz isso em vez de inventar uma.
     */
    private MembershipRole role;

    /** Quem convidou, pelo nome: e o que faz o convite ser reconhecivel por quem o recebe. */
    private String invitedByName;

    private LocalDateTime expiresAt;

}

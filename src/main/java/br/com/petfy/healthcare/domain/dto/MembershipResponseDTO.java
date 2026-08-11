package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.MembershipRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma pessoa da equipe, como a Tela 16 pede: pessoa, funcao, desde quando, e o que ela registra.
 *
 * <b>O CRMV vem junto, e nao e enfeite.</b> A tabela do desenho mostra o registro ao lado da
 * veterinaria porque funcao e credencial sao coisas diferentes: ser VETERINARIO aqui e o papel
 * na organizacao, e o que autoriza ato clinico e a credencial da pessoa. Quem le a tabela precisa
 * distinguir "a organizacao chama de veterinaria" de "o conselho registrou".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponseDTO {

    private UUID membershipId;

    /**
     * De qual organizacao e este vinculo.
     *
     * <b>Entrou quando o aceite de convite passou a devolver o vinculo recem-criado.</b> Ali a
     * organizacao e a informacao principal — "voce agora e da Clinica X" —, e um DTO que so
     * dissesse a funcao obrigaria o cliente a saber de antemao em qual organizacao ele entrou,
     * que e justamente o que ele nao sabe: o convite e que decide.
     */
    private UUID organizationId;

    private String organizationName;

    private UUID personId;

    private String personName;

    private String personEmail;

    private MembershipRole role;

    /** Desde quando e da equipe — o "desde 03/2024" do desenho. */
    private LocalDateTime joinedAt;

    /**
     * O registro profissional, quando existe. Nulo diz que a pessoa nao declarou credencial —
     * o que e o normal do monitor e do voluntario, e nao uma pendencia.
     */
    private String professionalCredential;

}

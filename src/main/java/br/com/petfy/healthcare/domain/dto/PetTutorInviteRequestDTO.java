package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.PetTutorRole;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetTutorInviteRequestDTO {

    /**
     * **Obrigatorio**, ao contrario do convite de clinica, onde e opcional. Um
     * convite de animal da acesso ao historico de saude do animal e ao nome do
     * tutor: link sem dono encaminhado por engano entrega dado pessoal a quem
     * passar por ele.
     */
    @NotBlank(message = "email e obrigatorio")
    @Email(message = "email invalido")
    private String email;

    /**
     * Papel oferecido. {@code HOLDER} e transferencia de titularidade: aceitar
     * promove quem recebeu e rebaixa quem enviou a EDITOR.
     */
    @NotNull(message = "role e obrigatorio (HOLDER, EDITOR ou VIEWER)")
    private PetTutorRole role;

    /**
     * Janela curta por padrao. Convite de animal vale menos tempo que o de clinica:
     * o de clinica circula dentro de uma equipe, este vai por mensagem para uma
     * pessoa que responde na hora ou nao responde.
     */
    @Min(value = 1, message = "a validade deve ser de pelo menos 1 dia")
    @Max(value = 30, message = "a validade nao pode passar de 30 dias")
    private Integer expiresInDays;

}

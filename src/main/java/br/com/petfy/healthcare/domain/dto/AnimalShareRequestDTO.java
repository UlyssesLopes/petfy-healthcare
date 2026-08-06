package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import br.com.petfy.healthcare.domain.entity.GrantScope;

import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalShareRequestDTO {

    /**
     * Validade em dias. O teto existe para que um link nao vire permanente por
     * descuido: o tutor renova quando precisar.
     */
    @Min(value = 1, message = "a validade deve ser de pelo menos 1 dia")
    @Max(value = 365, message = "a validade nao pode passar de 365 dias")
    private Integer expiresInDays;

    /**
     * O quanto o link mostra. Ausente ou vazio recebe apenas CARTEIRA, que e o que
     * o link sempre mostrou - o prontuario fica de fora de proposito.
     *
     * <b>E aqui que mora o cartao de emergencia</b> (decisao 13): o tutor prepara de
     * vespera um link com alergia, medicacao e contato, e quem socorre le sem
     * precisar acorda-lo. Quem autoriza continua sendo o tutor, antecipadamente.
     */
    private Set<GrantScope> scopes;

}

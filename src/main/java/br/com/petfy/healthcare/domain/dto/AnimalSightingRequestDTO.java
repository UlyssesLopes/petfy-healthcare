package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * "Vi o gato" (Tela 43).
 *
 * <b>O corpo inteiro e opcional, e isso e a especificacao do gesto:</b> ele acontece todo dia, por
 * seis pessoas, em catorze gatos. Um formulario aqui mataria o registro — e sem registro a tela
 * nao sabe dizer quem sumiu, que e a unica coisa que ela existe para dizer.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalSightingRequestDTO {

    /**
     * Nulo e hoje, que e o caso de quase todo toque.
     *
     * Existe preenchido para quem registra a noite o que viu de manha — e para quem volta de uma
     * semana fora e lanca o que lembra.
     */
    private LocalDate seenOn;

}

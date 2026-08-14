package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um aviso, como quem o recebeu o le.
 *
 * <b>Assunto e corpo vem prontos do banco</b>, e nao de um tipo de evento remontado agora: o aviso e
 * um fato, e vale depois de o fato deixar de valer. "Ana passou a cuidar do Code" continua sendo o
 * que a pessoa leu mesmo no dia em que Ana sair.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonNotificationResponseDTO {

    private UUID personNotificationId;

    private String subject;

    /** O corpo inteiro, em linhas separadas por quebra — o mesmo texto que foi por e-mail. */
    private String body;

    /** O evento que o produziu, como o dispatcher o nomeia. Serve para agrupar, nao para remontar. */
    private String event;

    private LocalDateTime createdAt;

    /** Nulo e "nao lido". */
    private LocalDateTime readAt;

}

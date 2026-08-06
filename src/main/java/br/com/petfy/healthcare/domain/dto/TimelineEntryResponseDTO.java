package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.TimelineEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma entrada da linha do tempo, como o cliente a le.
 *
 * O detalhe do evento mora no recurso proprio - a vacina tem proxima dose, a condicao
 * tem gravidade. Aqui vai o que a linha do tempo precisa: o que foi, quando aconteceu,
 * quando foi registrado, por quem, e se quem esta lendo alcanca o conteudo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEntryResponseDTO {

    /** Nulo quando o evento esta fora do escopo de quem le: nao ha o que abrir. */
    private UUID eventId;

    private TimelineEventType eventType;

    /** Quando aconteceu. E por este campo que a linha ordena. */
    private LocalDateTime occurredAt;

    /** Quando foi registrado. Diferente do acima sempre que houve atraso de digitacao. */
    private LocalDateTime recordedAt;

    /** Nulo fora do escopo, e nulo tambem nos eventos anteriores ao P4. */
    private String recordedByName;

    /** Nulo fora do escopo. */
    private String summary;

    /**
     * Se quem le alcanca o conteudo deste evento.
     *
     * Falso nao significa que o evento nao existe: significa que ele existe e nao foi
     * concedido. A diferenca importa - some seria dizer ao leitor que o animal nunca foi
     * ao veterinario.
     */
    private boolean visivel;

    private boolean healthData;

}

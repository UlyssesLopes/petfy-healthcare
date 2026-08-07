package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.CredentialStatus;
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
     * Em nome de que organizacao o registro foi feito. Nulo fora do escopo, e nulo
     * tambem quando quem registrou agia por si.
     *
     * <b>Fora do escopo tem de ser nulo, e nao e zelo:</b> dizer "Clinica Bicho Feliz"
     * num evento que quem le nao pode abrir entregaria justamente o que o escopo
     * esconde - que o animal foi atendido, e por quem.
     */
    private String organizationName;

    /**
     * A credencial de quem registrou, como {@code CRMV-SP 12345}. Nulo fora do escopo,
     * e nulo quando quem registrou nao tem credencial.
     */
    private String credentialLabel;

    /**
     * O estado da credencial acima.
     *
     * A tela mostra credencial apenas informada <b>como</b> informada, em tinta
     * secundaria - o produto nao pode dar selo de verificado que nao conferiu (5.10).
     */
    private CredentialStatus credentialStatus;

    /**
     * Quantas correcoes este evento sofreu, e zero quando nenhuma.
     *
     * Existe para a tela marcar sucessao sem chamar a rota de correcoes de cada evento
     * para descobrir que a maioria nao tem nenhuma. O conteudo do valor anterior
     * continua em {@code /vaccines/{id}/corrections} e
     * {@code /health-records/{id}/corrections}.
     */
    private long correctionCount;

    /**
     * O peso da pesagem anterior, para o cliente calcular a variacao.
     *
     * Nulo fora de pesagem, nulo na primeira pesagem do animal, e nulo fora do escopo. A
     * curva inteira mora em {@code GET /animals/{id}/weights} - aqui vai so o suficiente
     * para a entrada dizer se o peso subiu ou desceu, que e o que a secao 5.2 do DESIGN
     * cobra do evento.
     */
    private Double previousWeight;

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

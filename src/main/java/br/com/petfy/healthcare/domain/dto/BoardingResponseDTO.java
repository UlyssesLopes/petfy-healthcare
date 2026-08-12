package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma estadia, do lado de quem le (Tela 47).
 *
 * <b>"Você está em viagem. Isto é o que aconteceu com ele desde que saiu de casa."</b> Este DTO e a
 * moldura dessa frase; os eventos vem da linha do tempo, recortada por {@code startedAt} — a mesma
 * linha do tempo de sempre, com o mesmo mascaramento por escopo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardingResponseDTO {

    private UUID boardingId;

    private UUID animalId;

    private String animalName;

    /** Onde ele esta. "Creche Quintal". */
    private String organizationName;

    private UUID organizationId;

    /** Quando entrou. E tambem o {@code since} que a linha do tempo desta tela usa. */
    private LocalDateTime startedAt;

    private LocalDate expectedReturnOn;

    /** Quando voltou. Nulo enquanto ele esta la. */
    private LocalDateTime endedAt;

    /**
     * "Dia 3 de 7."
     *
     * <b>Calculado no servidor, e nao na tela.</b> Nao e economia de codigo: o dia 3 depende do fuso
     * de quem esta viajando, e um calculo no cliente diria "dia 2" para o tutor que abriu o Petfy em
     * Lisboa. O servidor conta a partir das datas que ele gravou, e a resposta e a mesma em qualquer
     * lugar do mundo — que e justamente o caso desta tela.
     */
    private int dayOfStay;

    private int totalDays;

    /**
     * Quem responde pelo animal antes e depois da estadia.
     *
     * A tela usa para dizer a quem ele volta, e o servico usa para saber quem pode encerrar: o
     * anterior tambem encerra, e nao so a creche.
     */
    private String returnsToName;

    /** Se quem esta lendo pode registrar a volta. */
    private boolean canEnd;

}

package br.com.petfy.healthcare.domain.dto;

import br.com.petfy.healthcare.domain.entity.ServiceAppointmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha da agenda do petshop (Tela 18).
 *
 * <b>"E tudo o que Marcelo compartilhou, e e tudo o que o banho exige. O historico clinico do Code
 * existe e nao esta aqui."</b> E a frase que este DTO precisa cumprir: as {@link #safetyNotes} sao as
 * quatro linhas que tornam o banho seguro, e nao um recorte do prontuario.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceAppointmentResponseDTO {

    private UUID serviceAppointmentId;

    private UUID animalId;

    private String animalName;

    private String service;

    private LocalDateTime scheduledAt;

    private ServiceAppointmentStatus status;

    private LocalDateTime checkedInAt;

    private LocalDateTime completedAt;

    /**
     * "O que voce precisa saber antes de encostar nele."
     *
     * Vazia quando o petshop nao alcanca o animal — e ai o {@link #inTheDark} diz por que.
     */
    private List<ServiceSafetyNoteDTO> safetyNotes;

    /**
     * <b>"Acesso ao Petfy venceu em 31/07 — voce esta no escuro."</b>
     *
     * O estado mais importante desta tela depois do normal, e o que a maioria dos produtos esconderia.
     * Um cartao sem as quatro linhas parece um animal sem restricao nenhuma; dizer que o acesso venceu
     * e a diferenca entre tosar com cuidado e tosar as cegas.
     */
    private boolean inTheDark;

}

package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma linha do dia da creche (Tela 17).
 *
 * <b>Ela existe para animal que ainda nao tem registro nenhum do dia.</b> Quem tem matricula ativa
 * nasce ESPERADO as 00h01, sem ninguem marcar nada — e e isso que faz a tela poder dizer "14
 * esperados · 6 ja chegaram" as 7h34.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {

    /** Nulo enquanto ninguem marcou nada: o dia dele ainda nao foi gravado. */
    private UUID attendanceId;

    private UUID enrollmentId;
    private UUID animalId;
    private String animalName;
    private String species;

    private LocalDate day;

    /** ESPERADO, PRESENTE, SAIU ou FALTA. */
    private String status;

    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private String pickupNote;

    /**
     * "Hoje precisa": o que a creche tem de saber sobre este animal hoje.
     *
     * Sai das orientacoes vigentes e das condicoes — "Amoxicilina ao meio-dia · sem frango" do
     * desenho. Vem do que o tutor concedeu, e nao do prontuario inteiro.
     */
    private List<String> todayNeeds;

    /**
     * O animal esta impedido de entrar hoje, e por que.
     *
     * "V10 venceu ontem — nao pode entrar" e a linha mais dura da Tela 17, e ela nao e opiniao da
     * monitora: e a mesma comprovacao da matricula, olhada no dia.
     */
    private boolean blocked;
    private String blockedReason;
}

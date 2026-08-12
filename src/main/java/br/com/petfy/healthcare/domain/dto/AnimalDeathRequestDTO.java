package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * O encerramento da linha do tempo (Tela 33).
 *
 * <b>Um campo obrigatorio, e os outros dois existem so se a pessoa quiser.</b> Quem preenche este
 * formulario acabou de perder o animal — "quando voce conseguir, preencha o que souber. Nada aqui
 * tem pressa". Exigir local ou motivo transformaria o pior dia do tutor num formulario que o
 * recusa.
 *
 * <b>Nao ha campo de causa da morte, e a ausencia e o desenho:</b> "nao pergunta a causa da morte
 * — se voce quiser contar, o campo aberto esta la".
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnimalDeathRequestDTO {

    /**
     * "Quando foi".
     *
     * O unico obrigatorio, e o unico que o tutor nao consegue reconstruir depois — e tambem o que
     * faz "2019 — 2026" existir na ficha fechada. A recusa a data futura mora no servico:
     * {@code CURRENT_DATE} nao e IMMUTABLE e nao cabe num CHECK do Postgres.
     */
    @NotNull(message = "deceasedOn e obrigatorio")
    private LocalDate deceasedOn;

    /** "Onde · opcional". Em casa, na clinica, na estrada. */
    @Size(max = 120, message = "place nao pode passar de 120 caracteres")
    private String place;

    /**
     * "Se quiser dizer alguma coisa · opcional".
     *
     * Vai para a linha do tempo assinada por quem escreveu, e nao vira observacao clinica: uma
     * despedida nao e um achado, e no escopo {@code OBSERVACOES} ela seria entregue a toda creche
     * que tem observacoes concedidas.
     */
    private String farewellNote;

}

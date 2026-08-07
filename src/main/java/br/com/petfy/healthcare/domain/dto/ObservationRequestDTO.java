package br.com.petfy.healthcare.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/** O que se viu com o animal. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationRequestDTO {

    @NotBlank
    @Size(max = 1000)
    private String description;

    /**
     * Quando foi visto. Ausente assume agora.
     *
     * <b>Nao e o instante da digitacao</b>: a creche registra as 18h o que viu as 9h, e a
     * linha do tempo ordena por isto (3.9). Quem registra na hora nao precisa preencher.
     */
    private LocalDateTime observedAt;

    /**
     * O alerta (4.5): o que se viu pede atencao agora.
     *
     * Nao e emergencia medica e nao e ato clinico - ganha peso pela posicao no feed, e nao
     * por cor de alarme.
     */
    private boolean urgent;

}

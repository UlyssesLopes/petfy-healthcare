package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.util.UUID;

/** A turma com a ocupacao contada: "Turma Tarde · 12 de 15 vagas". */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassGroupResponseDTO {

    private UUID classGroupId;
    private String name;

    /** Nulo quando a turma nao declara limite. */
    private Integer capacity;

    /**
     * Matriculas vivas, e a PENDENTE conta.
     *
     * A vaga da matricula pendente esta guardada — e o que a Tela 10 promete ao dizer que ela "se
     * completa sozinha". Nao contar deixaria a creche vender a mesma vaga duas vezes.
     */
    private long occupied;
}

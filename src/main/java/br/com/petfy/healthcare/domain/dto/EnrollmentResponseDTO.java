package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A matricula, com a comprovacao de saude que decide se ela vale. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponseDTO {

    private UUID enrollmentId;
    private UUID animalId;
    private String animalName;
    private UUID classGroupId;
    private String classGroupName;
    private String organizationName;

    /** PENDENTE, ATIVA ou ENCERRADA. */
    private String status;

    private LocalDateTime requestedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime endedAt;
    private String createdByName;

    /**
     * A comprovacao, linha por linha — e ela vem SEMPRE, inclusive na matricula ativa.
     *
     * Uma dose vence sozinha depois de a matricula ativar, e a creche precisa ver isso no dia em
     * que acontecer, nao no dia em que alguem for matricular de novo.
     */
    private List<HealthProofItemDTO> healthProof;
}

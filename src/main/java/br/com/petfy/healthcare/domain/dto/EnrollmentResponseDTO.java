package br.com.petfy.healthcare.domain.dto;

import lombok.*;

import java.math.BigDecimal;
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

    /* --------------------------------------------- o combinado com o tutor (Tela 41) */

    /**
     * A mensalidade combinada.
     *
     * <b>Os quatro campos abaixo vem nulos para quem alcanca o animal sem responder por ele</b>, e
     * isso e a mesma regra do custo, aplicada no unico outro lugar em que ha dinheiro: "o que a
     * Clinica Vet Norte cobra do Marcelo nao e assunto da creche, do petshop nem de outra clinica.
     * Nenhum escopo de acesso concede preco junto com saude."
     *
     * A creche que combinou LE o que ela mesma combinou — ela precisa, para conferir e corrigir.
     * O que ela nao alcanca e o combinado das outras, e nenhuma rota daqui lhe da isso.
     */
    private BigDecimal monthlyFee;

    /** "Vence todo dia 05". */
    private Integer dueDay;

    /** A diaria de quem vem fora dos dias combinados. */
    private BigDecimal dailyRate;

    /**
     * Os dias em que o animal e esperado — MONDAY..SUNDAY.
     *
     * E o que faz o tutor ler "3 dias por semana" sem perguntar, e e o que decide se o check-in de
     * hoje lanca uma diaria. Lista vazia e "nao se combinou dia", e nao "nenhum dia".
     */
    private List<String> weekdays;
}
